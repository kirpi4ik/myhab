<template>
  <q-card flat bordered class="intercom-card small-card">
    <q-card-section class="intercom-header row items-center q-py-sm">
      <q-icon name="mdi-doorbell-video" size="22px" class="q-mr-sm"/>
      <div class="text-subtitle2 ellipsis">{{ title }}</div>
      <q-space/>
      <q-btn flat dense round icon="mdi-refresh" :loading="loadingSnap"
             :disable="loadingSnap" @click="refresh">
        <q-tooltip>{{ $t('intercom.refresh') }}</q-tooltip>
      </q-btn>
    </q-card-section>

    <div class="intercom-snapshot" @click="openVideo">
      <img v-if="snapshotUrl" :src="snapshotUrl" class="intercom-img" alt="intercom snapshot"/>
      <div v-else class="intercom-placeholder column items-center justify-center text-grey">
        <q-icon name="mdi-cctv-off" size="32px"/>
        <div class="text-caption q-mt-xs">{{ $t('intercom.camera_offline') }}</div>
      </div>
      <div v-if="snapshotUrl" class="intercom-play-overlay">
        <q-icon name="mdi-play-circle-outline" size="42px"/>
      </div>
    </div>

    <q-card-actions align="between" class="q-px-md">
      <q-btn unelevated color="primary" icon="mdi-lock-open-variant"
             :label="$t('intercom.unlock')" @click="showUnlock = true"/>
      <q-btn flat color="primary" icon="mdi-video"
             :label="$t('intercom.view_camera')" @click="openVideo"/>
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

    <!-- Live video popup -->
    <q-dialog v-model="showVideo" :maximized="$q.platform.is.mobile" @show="startPlayback" @hide="closeVideo">
      <q-card class="intercom-video-card">
        <q-bar class="bg-dark text-white">
          <q-icon name="mdi-doorbell-video"/>
          <div class="q-ml-sm">{{ title }}</div>
          <q-space/>
          <q-btn dense flat round icon="mdi-close" v-close-popup/>
        </q-bar>
        <div class="intercom-video-wrap">
          <video ref="videoEl" class="intercom-video" autoplay playsinline controls muted/>
          <div v-if="videoError" class="intercom-video-error column items-center justify-center text-grey">
            <q-icon name="mdi-video-off" size="40px"/>
            <div class="q-mt-sm">{{ $t('intercom.stream_unavailable') }}</div>
          </div>
        </div>
      </q-card>
    </q-dialog>
  </q-card>
</template>

<script setup>
import {computed, onMounted, onUnmounted, ref} from 'vue';
import {useApolloClient, useQuery} from '@vue/apollo-composable';
import {useQuasar} from 'quasar';
import {useI18n} from 'vue-i18n';

import {PERIPHERAL_GET_BY_ID, PUSH_EVENT} from '@/graphql/queries';
import {authzService} from '@/_services';
import {intercomService} from '@/_services/intercom.service';
import {useWebSocketListeners} from '@/composables';

/**
 * Dashboard intercom tile: live camera snapshot, gate unlock (PIN-gated, fires its
 * own evt_intercom_unlock — never the DOOR_LOCK gate), snapshot refresh, and a
 * live-video popup (HLS). Media comes from the backend IntercomController, which
 * proxies go2rtc (the real Tuya-WebRTC feed); when go2rtc is unconfigured the
 * snapshot endpoint returns 204 and the tile shows a "camera offline" placeholder.
 */
const props = defineProps({
  peripheralId: {type: Number, required: true},
});

const $q = useQuasar();
const {t} = useI18n();
const {client} = useApolloClient();

// --- peripheral name (nice-to-have header) ---------------------------------
const {result} = useQuery(PERIPHERAL_GET_BY_ID, () => ({id: props.peripheralId}), {fetchPolicy: 'cache-first'});
const title = computed(() => result.value?.devicePeripheral?.name || t('dashboard.widgets.intercom'));

// --- snapshot --------------------------------------------------------------
const snapshotUrl = ref(null);
const loadingSnap = ref(false);

const setSnapshot = (next) => {
  if (snapshotUrl.value) URL.revokeObjectURL(snapshotUrl.value);
  snapshotUrl.value = next;
};

const loadSnapshot = async () => {
  loadingSnap.value = true;
  try {
    const next = await intercomService.fetchSnapshotBlobUrl(props.peripheralId);
    if (next) setSnapshot(next);
  } finally {
    loadingSnap.value = false;
  }
};

// Refresh = a fresh live frame from go2rtc.
const refresh = () => loadSnapshot();

// A doorbell/motion notification is a good moment to refresh the still.
useWebSocketListeners([{eventName: 'evt_user_notification', callback: () => loadSnapshot()}]);

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

// --- live video ------------------------------------------------------------
const showVideo = ref(false);
const videoEl = ref(null);
const videoError = ref(false);
let hls = null;

const teardownHls = () => {
  if (hls) {
    hls.destroy();
    hls = null;
  }
};

// Just open the dialog; playback starts from its @show, when the <video> the
// dialog teleports/animates into the DOM is actually mounted (attaching hls.js
// before that races the ref to null and silently shows nothing).
const openVideo = () => {
  videoError.value = false;
  showVideo.value = true;
};

const startPlayback = async () => {
  videoError.value = false;
  const video = videoEl.value;
  if (!video) {
    videoError.value = true;
    return;
  }
  // The playlist and its segments are served by the backend under JWT, so we must
  // send the Bearer on every request — only hls.js (xhrSetup) can do that. Native
  // <video src> HLS (iOS Safari) can't carry the header, so it's unsupported here.
  const url = intercomService.streamPlaylistUrl(props.peripheralId);
  const token = intercomService.authToken();
  try {
    const Hls = (await import('hls.js')).default;
    if (!Hls.isSupported()) {
      console.warn('hls.js unsupported (no MSE) — cannot play authenticated HLS here');
      videoError.value = true;
      return;
    }
    teardownHls();
    hls = new Hls({
      enableWorker: true,
      lowLatencyMode: true,
      xhrSetup: (xhr) => {
        if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`);
      },
    });
    hls.on(Hls.Events.MANIFEST_PARSED, () => video.play?.().catch(() => {}));
    hls.on(Hls.Events.ERROR, (_evt, data) => {
      if (data?.fatal) {
        console.error('HLS fatal error:', data.type, data.details);
        videoError.value = true;
      }
    });
    hls.loadSource(url);
    hls.attachMedia(video);
  } catch (error) {
    console.error('HLS playback failed:', error);
    videoError.value = true;
  }
};

const closeVideo = () => {
  teardownHls();
  if (videoEl.value) videoEl.value.removeAttribute('src');
  showVideo.value = false;
};

onMounted(() => loadSnapshot());
onUnmounted(() => {
  setSnapshot(null);
  teardownHls();
});
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

.intercom-snapshot {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #000;
  cursor: pointer;
  overflow: hidden;
}

.intercom-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.intercom-placeholder {
  width: 100%;
  height: 100%;
}

.intercom-play-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.85);
  opacity: 0;
  transition: opacity 0.15s ease;
}

.intercom-snapshot:hover .intercom-play-overlay {
  opacity: 1;
}

.intercom-video-card {
  width: 800px;
  max-width: 95vw;
}

.intercom-video-wrap {
  position: relative;
  background: #000;
}

.intercom-video {
  width: 100%;
  max-height: 80vh;
  display: block;
}

.intercom-video-error {
  position: absolute;
  inset: 0;
}
</style>
