<template>
  <q-page padding>
    <q-card flat bordered class="scan-card">
      <q-card-section>
        <div class="text-h5 text-primary">
          <q-icon name="mdi-qrcode-scan" class="q-mr-sm"/>
          {{ $t('qr.scan.title') }}
        </div>
        <div class="text-subtitle2 text-grey-7">
          {{ $t('qr.scan.subtitle') }}
        </div>
      </q-card-section>

      <q-separator/>

      <q-card-section>
        <div class="scan-frame">
          <qrcode-stream
            :paused="paused"
            @detect="onDetect"
            @error="onError"
            @camera-on="onReady"
          >
            <div v-if="loadingCamera" class="absolute-center text-center text-grey-7">
              <q-spinner-gears size="40px" color="primary"/>
              <div class="q-mt-sm">{{ $t('qr.scan.starting_camera') }}</div>
            </div>
          </qrcode-stream>
        </div>

        <q-banner v-if="lastError" dense class="bg-red-1 text-red-9 q-mt-md">
          <template v-slot:avatar>
            <q-icon name="mdi-alert-circle"/>
          </template>
          {{ lastError }}
        </q-banner>

        <q-banner v-if="lastUnrecognized" dense class="bg-orange-1 text-orange-9 q-mt-md">
          <template v-slot:avatar>
            <q-icon name="mdi-help-circle"/>
          </template>
          {{ $t('qr.scan.unrecognized') }}
          <template v-slot:action>
            <q-btn flat dense :label="$t('qr.scan.try_again')" @click="resume"/>
          </template>
        </q-banner>
      </q-card-section>

      <q-separator/>

      <q-card-actions>
        <q-btn color="grey" icon="mdi-arrow-left" :label="$t('actions.nav.back')" flat @click="$router.go(-1)"/>
      </q-card-actions>
    </q-card>
  </q-page>
</template>

<script>
import {defineComponent, ref} from "vue";
import {QrcodeStream} from "vue-qrcode-reader";
import {useQuasar} from "quasar";
import {useI18n} from "vue-i18n";
import {useEntityRouter} from "@/composables";

export default defineComponent({
  name: 'ScanQr',
  components: {QrcodeStream},
  setup() {
    const $q = useQuasar();
    const {t} = useI18n({useScope: 'global'});
    const {navigateToEntity, parseEntityToken} = useEntityRouter();

    const paused = ref(false);
    const loadingCamera = ref(true);
    const lastError = ref(null);
    const lastUnrecognized = ref(false);

    const onReady = () => {
      loadingCamera.value = false;
      lastError.value = null;
    };

    const resume = () => {
      lastUnrecognized.value = false;
      paused.value = false;
    };

    /**
     * vue-qrcode-reader emits an array of detected codes. We take the first,
     * parse the stable myhab://TYPE/ID token and navigate against the current app.
     */
    const onDetect = (codes) => {
      const raw = Array.isArray(codes) ? codes[0]?.rawValue : codes?.rawValue;
      const parsed = parseEntityToken(raw);
      if (!parsed) {
        lastUnrecognized.value = true;
        paused.value = true;
        return;
      }
      const ok = navigateToEntity(parsed.entityType, parsed.id);
      if (!ok) {
        lastUnrecognized.value = true;
        paused.value = true;
        $q.notify({
          color: 'negative',
          message: t('qr.scan.no_route', {type: parsed.entityType}),
          icon: 'mdi-alert-circle',
          position: 'top'
        });
      }
    };

    const onError = (error) => {
      loadingCamera.value = false;
      const messages = {
        NotAllowedError: t('qr.scan.errors.denied'),
        NotFoundError: t('qr.scan.errors.not_found'),
        NotSupportedError: t('qr.scan.errors.not_supported'),
        NotReadableError: t('qr.scan.errors.in_use'),
        OverconstrainedError: t('qr.scan.errors.overconstrained'),
        StreamApiNotSupportedError: t('qr.scan.errors.stream_unsupported'),
        InsecureContextError: t('qr.scan.errors.insecure')
      };
      lastError.value = messages[error.name] || t('qr.scan.errors.generic', {message: error.message || error.name});
    };

    return {
      paused,
      loadingCamera,
      lastError,
      lastUnrecognized,
      onReady,
      onDetect,
      onError,
      resume,
    };
  }
});
</script>

<style scoped>
.scan-card {
  max-width: 560px;
  margin: 0 auto;
}
.scan-frame {
  position: relative;
  width: 100%;
  max-width: 480px;
  aspect-ratio: 1 / 1;
  margin: 0 auto;
  border-radius: 8px;
  overflow: hidden;
  background: #000;
}
</style>
