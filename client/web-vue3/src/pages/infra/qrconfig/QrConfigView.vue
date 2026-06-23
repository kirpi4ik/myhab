<template>
  <q-page padding>
    <q-card flat bordered>
      <!-- Header -->
      <q-card-section>
        <div class="row items-center">
          <div class="col">
            <div class="text-h5">
              <q-icon name="mdi-qrcode" color="primary" size="sm" class="q-mr-sm"/>
              {{ $t('qr.config.title') }}
            </div>
            <div class="text-subtitle2 text-grey-7">
              {{ $t('qr.config.subtitle') }}
            </div>
          </div>
        </div>
      </q-card-section>

      <q-separator/>

      <q-card-section class="q-gutter-md">
        <!-- Enable toggle -->
        <q-toggle
          v-model="enabled"
          color="primary"
          :label="enabled ? $t('qr.config.enabled_on') : $t('qr.config.enabled_off')"
          left-label
        />

        <q-separator/>

        <!-- Content template -->
        <div>
          <div class="text-subtitle1 q-mb-xs">{{ $t('qr.config.template_label') }}</div>
          <div class="text-caption text-grey-7 q-mb-sm">
            {{ $t('qr.config.template_hint') }}
          </div>
          <q-input
            ref="templateInput"
            v-model="contentTemplate"
            type="textarea"
            filled
            autogrow
            :disable="!enabled"
            :rules="[val => !!(val && val.trim()) || $t('qr.config.template_required')]"
          />
          <div class="q-mt-sm q-gutter-xs">
            <q-chip
              v-for="v in availableVariables"
              :key="v"
              clickable
              dense
              color="primary"
              text-color="white"
              :disable="!enabled"
              @click="insertVariable(v)"
            >
              {{ '{' + v + '}' }}
            </q-chip>
          </div>
        </div>

        <!-- Resolved preview (client-side, with sample values) -->
        <q-banner dense class="bg-grey-2 text-grey-9">
          <template v-slot:avatar>
            <q-icon name="mdi-eye-outline" color="primary"/>
          </template>
          <div class="text-caption">{{ $t('qr.config.resolved_example') }}</div>
          <code class="text-primary">{{ resolvedPreview }}</code>
        </q-banner>

        <q-separator/>

        <!-- Rendering options -->
        <div class="row q-col-gutter-md">
          <div class="col-12 col-sm-6">
            <q-select
              v-model="position"
              :options="['right', 'left']"
              :label="$t('qr.config.position')"
              filled
              :disable="!enabled"
            />
          </div>
          <div class="col-12 col-sm-6">
            <q-input
              v-model.number="size"
              type="number"
              min="0"
              :label="$t('qr.config.size')"
              filled
              :disable="!enabled"
            />
          </div>
        </div>

        <q-separator/>

        <!-- Live label preview via the existing REST endpoint -->
        <div>
          <div class="text-subtitle1 q-mb-sm">{{ $t('qr.config.preview_section') }}</div>
          <div class="row items-center q-col-gutter-md">
            <div class="col-auto">
              <q-input
                v-model.number="previewCableId"
                type="number"
                :label="$t('qr.config.cable_id')"
                filled
                dense
                style="width: 140px"
              />
            </div>
            <div class="col-auto">
              <q-btn
                color="secondary"
                icon="mdi-image-search"
                :label="$t('qr.config.preview_btn')"
                :loading="previewing"
                :disable="!previewCableId"
                @click="previewLabel"
              />
              <div class="text-caption text-grey-7 q-mt-xs">
                {{ $t('qr.config.preview_hint') }}
              </div>
            </div>
          </div>
        </div>
      </q-card-section>

      <q-separator/>

      <q-card-actions>
        <q-btn color="grey" icon="mdi-arrow-left" :label="$t('actions.nav.back')" flat @click="$router.go(-1)"/>
        <q-space/>
        <q-btn
          unelevated
          color="positive"
          icon="mdi-content-save"
          :label="$t('qr.config.save')"
          :loading="saving"
          @click="onSave"
        />
      </q-card-actions>
    </q-card>

    <q-inner-loading :showing="loading">
      <q-spinner-gears size="50px" color="primary"/>
    </q-inner-loading>
  </q-page>
</template>

<script>
import {computed, defineComponent, onMounted, ref} from "vue";
import {useApolloClient} from "@vue/apollo-composable";
import {useQuasar} from "quasar";
import {useI18n} from "vue-i18n";
import {QR_CONFIG_GET, QR_CONFIG_UPDATE} from "@/graphql/queries";
import {Utils} from '@/_helpers';
import {authzService} from '@/_services';

export default defineComponent({
  name: 'QrConfigView',
  setup() {
    const $q = useQuasar();
    const {t} = useI18n({useScope: 'global'});
    const {client} = useApolloClient();

    const loading = ref(false);
    const saving = ref(false);
    const previewing = ref(false);

    const enabled = ref(false);
    const contentTemplate = ref('myhab://{entityType}/{id}');
    const position = ref('right');
    const size = ref(0);
    const availableVariables = ref([]);
    const previewCableId = ref(null);
    const templateInput = ref(null);

    // Sample values for the client-side resolved preview.
    const SAMPLE = {
      entityType: 'CABLE',
      id: '42',
      code: 'C-042',
      description: 'Living room ceiling',
      category: 'CAT6',
      rack: 'Rack-A',
      patchPanel: 'PP1:12'
    };

    const resolvedPreview = computed(() => {
      let out = contentTemplate.value || '';
      Object.keys(SAMPLE).forEach(k => {
        out = out.split('{' + k + '}').join(SAMPLE[k]);
      });
      return out;
    });

    /**
     * Insert a {variable} token at the textarea cursor position.
     */
    const insertVariable = (name) => {
      const token = '{' + name + '}';
      const el = templateInput.value?.$el?.querySelector('textarea');
      const current = contentTemplate.value || '';
      if (el && typeof el.selectionStart === 'number') {
        const start = el.selectionStart;
        const end = el.selectionEnd;
        contentTemplate.value = current.slice(0, start) + token + current.slice(end);
        // Restore caret after the inserted token on next tick.
        requestAnimationFrame(() => {
          el.focus();
          const pos = start + token.length;
          el.setSelectionRange(pos, pos);
        });
      } else {
        contentTemplate.value = current + token;
      }
    };

    const fetchData = () => {
      loading.value = true;
      client.query({
        query: QR_CONFIG_GET,
        fetchPolicy: 'network-only',
      }).then(response => {
        const cfg = response.data.qrConfig;
        if (cfg) {
          enabled.value = cfg.enabled;
          contentTemplate.value = cfg.contentTemplate;
          position.value = cfg.position;
          size.value = cfg.size;
          availableVariables.value = cfg.availableVariables || [];
        }
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({color: 'negative', message: t('qr.config.load_error'), icon: 'mdi-alert-circle', position: 'top'});
        console.error('Error fetching QR config:', error);
      });
    };

    const onSave = () => {
      if (!contentTemplate.value || !contentTemplate.value.trim()) {
        $q.notify({color: 'warning', message: t('qr.config.template_required_notify'), icon: 'mdi-alert', position: 'top'});
        return;
      }
      saving.value = true;
      client.mutate({
        mutation: QR_CONFIG_UPDATE,
        variables: {
          enabled: enabled.value,
          contentTemplate: contentTemplate.value.trim(),
          position: position.value,
          size: Number(size.value) || 0
        },
        fetchPolicy: 'no-cache',
      }).then(response => {
        saving.value = false;
        const result = response.data.qrConfigUpdate;
        if (result?.success) {
          $q.notify({color: 'positive', message: t('qr.config.save_success'), icon: 'mdi-check-circle', position: 'top'});
        } else {
          $q.notify({color: 'negative', message: result?.error || t('qr.config.save_error'), icon: 'mdi-alert-circle', position: 'top'});
        }
      }).catch(error => {
        saving.value = false;
        $q.notify({color: 'negative', message: t('qr.config.save_error'), icon: 'mdi-alert-circle', position: 'top'});
        console.error('Error saving QR config:', error);
      });
    };

    /**
     * Fetch the cable label PNG (QR forced on) and open it in a new tab.
     * Done via fetch + blob because the REST endpoint needs the Bearer token.
     */
    const previewLabel = async () => {
      previewing.value = true;
      try {
        const url = `${Utils.host()}/api/labels/cable/${previewCableId.value}?qr=true`;
        const response = await fetch(url, {
          headers: {'Authorization': `Bearer ${authzService.currentUserValue?.access_token || ''}`}
        });
        if (!response.ok) {
          const errorData = await response.json().catch(() => ({}));
          throw new Error(errorData.error || 'Failed to generate preview');
        }
        const blob = await response.blob();
        const blobUrl = window.URL.createObjectURL(blob);
        window.open(blobUrl, '_blank');
      } catch (error) {
        $q.notify({color: 'negative', message: t('qr.config.preview_failed', {error: error.message}), icon: 'mdi-alert-circle', position: 'top'});
      } finally {
        previewing.value = false;
      }
    };

    onMounted(() => {
      fetchData();
    });

    return {
      loading,
      saving,
      previewing,
      enabled,
      contentTemplate,
      position,
      size,
      availableVariables,
      previewCableId,
      templateInput,
      resolvedPreview,
      insertVariable,
      onSave,
      previewLabel,
    };
  }
});
</script>
