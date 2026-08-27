<template>
  <q-card flat bordered class="intercom-card small-card">
    <q-card-section class="intercom-header row items-center q-py-sm">
      <q-icon name="mdi-doorbell-video" size="22px" class="q-mr-sm"/>
      <div class="text-subtitle2 ellipsis">{{ title }}</div>
      <q-space/>
      <q-btn flat dense round icon="mdi-history" :to="'/messages'">
        <q-tooltip>{{ $t('intercom.view_all') }}</q-tooltip>
      </q-btn>
    </q-card-section>

    <!-- Last doorbell / motion event -->
    <q-card-section class="intercom-event col-grow">
      <div v-if="lastEvent" class="row items-start no-wrap">
        <q-icon :name="levelIcon(lastEvent.level)" :color="levelColor(lastEvent.level)"
                size="24px" class="q-mr-sm q-mt-xs"/>
        <div class="col">
          <div class="text-body2 ellipsis-2-lines">{{ lastEvent.subject }}</div>
          <div v-if="lastEvent.message && lastEvent.message !== lastEvent.subject"
               class="text-caption text-grey-7 ellipsis-2-lines">{{ lastEvent.message }}</div>
          <div class="text-caption text-grey">{{ formatRelativeDate(lastEvent.tsCreated) }}</div>
        </div>
      </div>
      <div v-else class="column items-center justify-center text-grey full-height q-py-md">
        <q-icon name="mdi-bell-outline" size="28px"/>
        <div class="text-caption q-mt-xs">{{ $t('intercom.no_activity') }}</div>
      </div>
    </q-card-section>

    <q-card-actions align="center" class="q-px-md q-pb-md">
      <q-btn unelevated color="primary" icon="mdi-lock-open-variant" class="full-width"
             :label="$t('intercom.unlock')" @click="showUnlock = true"/>
    </q-card-actions>

    <!-- PIN confirm dialog -->
    <q-dialog v-model="showUnlock" persistent>
      <q-card style="min-width: 320px; max-width: 90vw">
        <q-bar class="bg-primary text-white">
          <q-icon name="mdi-lock-open-variant"/>
          <div class="q-ml-sm">{{ $t('intercom.unlock') }}</div>
          <q-space/>
          <q-btn dense flat round icon="mdi-close" v-close-popup/>
        </q-bar>
        <q-card-section>
          <q-input v-model="pin" :label="$t('intercom.pin')" type="password"
                   inputmode="numeric" autofocus outlined
                   @keyup.enter="submitUnlock"/>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat :label="$t('common.cancel')" v-close-popup/>
          <q-btn unelevated color="primary" :label="$t('intercom.unlock')"
                 :loading="unlocking" :disable="!pin" @click="submitUnlock"/>
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-card>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useApolloClient, useQuery} from '@vue/apollo-composable';
import {useQuasar} from 'quasar';
import {useI18n} from 'vue-i18n';
import {formatDistanceToNow, parseISO} from 'date-fns';

import {PERIPHERAL_GET_BY_ID, PUSH_EVENT} from '@/graphql/queries';
import {MY_MESSAGES} from '@/graphql/queries/messages';
import {authzService} from '@/_services';
import {useWebSocketListeners} from '@/composables';
import {useAppConfigStore} from 'src/store/app-config.store';

/**
 * Dashboard intercom tile: shows the latest doorbell/motion notification and a
 * PIN-gated unlock. This doorbell exposes no usable snapshot or live stream to a
 * self-hosted backend (firmware strips local RTSP/ONVIF/HTTP-snapshot; the Tuya
 * cloud pic API is unavailable), so the tile is notification + unlock only.
 *
 * Notifications arrive as UserMessage rows (the tuya-bridge publishes a notify
 * envelope on each doorbell/motion event); we surface the most recent one whose
 * fromSender matches the intercom's configured notify source. Unlock fires its own
 * evt_intercom_unlock — never the DOOR_LOCK gate.
 */
const props = defineProps({
  peripheralId: {type: Number, required: true},
});

const $q = useQuasar();
const {t} = useI18n();
const {client} = useApolloClient();
const appConfig = useAppConfigStore();

// The tuya-bridge's notify source (topic segment); UserMessage.fromSender carries it.
const notifySource = appConfig.get('specialDevices.intercom.notifySource', 'intercom');

// --- peripheral name (nice-to-have header) ---------------------------------
const {result} = useQuery(PERIPHERAL_GET_BY_ID, () => ({id: props.peripheralId}), {fetchPolicy: 'cache-first'});
const title = computed(() => result.value?.devicePeripheral?.name || t('dashboard.widgets.intercom'));

// --- latest doorbell / motion event ----------------------------------------
const {result: msgResult, refetch} = useQuery(MY_MESSAGES, () => ({}), {fetchPolicy: 'network-only'});
const lastEvent = computed(() => {
  const rows = (msgResult.value?.myMessages || []).filter((m) => m.fromSender === notifySource);
  if (!rows.length) return null;
  return [...rows].sort((a, b) => (b.tsCreated || '').localeCompare(a.tsCreated || ''))[0];
});

// A doorbell/motion notification means a new event to show.
useWebSocketListeners([{eventName: 'evt_user_notification', callback: () => refetch()}]);

const levelIcon = (level) => {
  switch (level) {
    case 'ERROR': return 'mdi-alert-circle';
    case 'WARN': return 'mdi-alert';
    default: return 'mdi-doorbell';
  }
};
const levelColor = (level) => {
  switch (level) {
    case 'ERROR': return 'negative';
    case 'WARN': return 'warning';
    default: return 'primary';
  }
};
const formatRelativeDate = (dateStr) => {
  if (!dateStr) return '';
  try {
    return formatDistanceToNow(parseISO(dateStr), {addSuffix: true});
  } catch {
    return dateStr;
  }
};

// --- unlock ----------------------------------------------------------------
const showUnlock = ref(false);
const pin = ref('');
const unlocking = ref(false);

const submitUnlock = async () => {
  if (!pin.value || unlocking.value) return;
  unlocking.value = true;
  try {
    await client.mutate({
      mutation: PUSH_EVENT,
      variables: {
        input: {
          p0: 'evt_intercom_unlock',
          p1: 'PERIPHERAL',
          p2: props.peripheralId,
          p3: 'mweb',
          p4: 'unlock',
          p5: pin.value,
          p6: authzService.currentUserValue?.login || 'unknown',
        },
      },
    });
    $q.notify({color: 'positive', message: t('intercom.unlock_sent'), icon: 'mdi-check-circle', position: 'top'});
    showUnlock.value = false;
    pin.value = '';
  } catch (error) {
    console.error('Intercom unlock failed:', error);
    $q.notify({color: 'negative', message: t('intercom.unlock_failed'), icon: 'mdi-alert-circle', position: 'top'});
  } finally {
    unlocking.value = false;
  }
};

onMounted(() => refetch());
</script>

<style scoped>
.intercom-card {
  min-height: 140px;
  display: flex;
  flex-direction: column;
}

.intercom-header {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.intercom-event {
  min-height: 64px;
}

.ellipsis-2-lines {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
