<template>
  <div id="fullscreen">
    <!-- Swiper over DB-backed dashboard screens -->
    <swiper
      v-if="!loading && screens.length"
      :pagination="{ dynamicBullets: true }"
      :modules="modules"
      class="swiper"
    >
      <swiper-slide v-for="screen in screens" :key="screen.id">
        <DashboardScreenSvg
          :screen="screen"
          :widgets="screen.widgets"
          :peripherals="peripherals"
          :bg-url="bgUrls[screen.id] || null"
          mode="view"
          @widget-activate="onWidgetActivate"
        />
      </swiper-slide>
    </swiper>

    <!-- Empty state -->
    <div v-if="!loading && !screens.length" class="empty-state column items-center justify-center">
      <q-icon name="mdi-monitor-dashboard" size="64px" color="grey-6" />
      <div class="text-h6 text-grey-7 q-mt-md">No dashboard screens configured</div>
      <q-btn
        v-if="isAdmin"
        class="q-mt-md"
        color="primary"
        icon="mdi-pencil-ruler"
        label="Open screen manager"
        to="/admin/screens"
      />
    </div>

    <!-- Unlock Confirmation Dialog -->
    <q-dialog
      v-model="unlockDialog.show"
      transition-show="jump-up"
      transition-hide="jump-down"
    >
      <q-card class="bg-white">
        <q-bar class="bg-deep-orange-7 text-white">
          <q-icon name="mdi-lock"/>
          <div>{{ $t('mobile.unlock.title') }}</div>
          <q-space/>
          <q-btn dense flat icon="mdi-close" v-close-popup>
            <q-tooltip>{{ $t('common.close') }}</q-tooltip>
          </q-btn>
        </q-bar>

        <q-card-section>
          <div class="text-h6">
            {{ $t('mobile.unlock.message') }}
          </div>
        </q-card-section>

        <q-card-section class="q-pa-none" vertical align="center">
          <div class="q-pa-sm">
            <q-btn
              flat
              class="text-h6"
              icon="mdi-lock-open"
              :label="$t('mobile.unlock.button')"
              no-caps
              @click="handleUnlock"
              :loading="unlocking"
              :disable="unlocking"
            />
          </div>
        </q-card-section>
      </q-card>
    </q-dialog>

    <!-- Loading State -->
    <q-inner-loading :showing="loading">
      <q-spinner-gears size="50px" color="primary"/>
    </q-inner-loading>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { Pagination } from 'swiper/modules';
import { Swiper, SwiperSlide } from 'swiper/vue';
import _ from 'lodash';
import { useApolloClient } from '@vue/apollo-composable';
import { useWebSocketStore } from '@/store/websocket.store';
import { DASHBOARD_SCREENS } from '@/graphql/queries';
import { screenService, authzService } from '@/_services';
import { Role } from '@/_helpers/role';
import {
  useNotifications,
  usePeripheralState,
  usePeripheralControl,
  useWebSocketListener,
} from '@/composables';
import DashboardScreenSvg from '@/components/dashboard/DashboardScreenSvg.vue';

// Import Swiper styles
import 'swiper/css';
import 'swiper/css/pagination';

const router = useRouter();
const modules = [Pagination];
const { client } = useApolloClient();

// Composables
const { notifyError, notifySuccess } = useNotifications();
const {
  peripherals,
  loading: peripheralsLoading,
  loadPeripherals,
  updatePeripheralFromEvent,
  getPeripheral,
  hasPeripheral,
} = usePeripheralState();
const { unlockDialog, handlePeripheralAction, unlockDoor } = usePeripheralControl();

// WebSocket store
const wsStore = useWebSocketStore();

// Screens
const screens = ref([]);
const bgUrls = ref({});
const screensLoading = ref(true);

const loading = computed(() => screensLoading.value || peripheralsLoading.value);
const isAdmin = computed(() => authzService.hasAnyRole([Role.Admin]));
const unlocking = ref(false);
const stompMessage = computed(() => wsStore.ws.message);

const revokeBgUrls = () => {
  Object.values(bgUrls.value).forEach((url) => URL.revokeObjectURL(url));
  bgUrls.value = {};
};

/**
 * Load enabled screens (ordered) + their background blob URLs. Screens with
 * unparseable layout JSON are skipped with a console warning.
 */
const loadScreens = async () => {
  try {
    const response = await client.query({
      query: DASHBOARD_SCREENS,
      variables: { enabledOnly: true },
      fetchPolicy: 'network-only',
    });
    const loaded = [];
    for (const screen of response.data.dashboardScreens || []) {
      try {
        const layout = JSON.parse(screen.layoutJson || '{"version":1,"widgets":[]}');
        loaded.push({ ...screen, widgets: layout.widgets || [] });
      } catch (err) {
        console.warn(`Skipping screen ${screen.id} (${screen.name}): bad layout JSON`, err);
      }
    }
    screens.value = loaded;
    revokeBgUrls();
    await Promise.all(
      loaded.map(async (screen) => {
        const url = await screenService.fetchBackgroundBlobUrl(screen.id, screen.tsUpdated);
        if (url) bgUrls.value[screen.id] = url;
      })
    );
  } catch (err) {
    notifyError('Failed to load dashboard screens');
    console.error('Error loading screens:', err);
  } finally {
    screensLoading.value = false;
  }
};

/**
 * Widget clicked in view mode -> resolve peripheral and run the category
 * action (toggle light/heat, unlock dialog). No id parsing involved.
 * Link controls navigate instead (in-app path or absolute URL).
 */
const onWidgetActivate = (widget) => {
  if (widget.kind === 'link') {
    const href = widget.href?.trim();
    if (!href) return;
    if (/^https?:\/\//i.test(href)) {
      window.open(href, '_blank', 'noopener');
    } else {
      router.push(href);
    }
    return;
  }
  if (!hasPeripheral(widget.peripheralId)) return;
  const peripheral = getPeripheral(widget.peripheralId);
  handlePeripheralAction(peripheral).catch((err) => {
    notifyError('Failed to control peripheral');
    console.error('Error handling peripheral action:', err);
  });
};

const handleUnlock = async () => {
  unlocking.value = true;
  try {
    await unlockDoor();
    notifySuccess('Door unlocked successfully');
  } catch (err) {
    notifyError('Failed to unlock door');
    console.error('Error unlocking door:', err);
  } finally {
    unlocking.value = false;
  }
};

const initialize = async () => {
  try {
    await Promise.all([loadPeripherals(), loadScreens()]);
  } catch (err) {
    notifyError('Failed to load peripherals');
    console.error('Error initializing:', err);
  }
};

// Live port updates: mutate the peripherals map — widget components react
// individually (no full re-render, no refresh key).
watch(stompMessage, (newVal) => {
  if (newVal?.eventName === 'evt_port_value_persisted') {
    updatePeripheralFromEvent(newVal.jsonPayload);
  }
});

// Re-sync full state whenever the socket recovers. STOMP topics are not
// replayed, so events that fired while offline would otherwise be lost and
// the UI would stay stale until a manual reload. Also refetches screens,
// covering screen-changed events missed while offline.
watch(() => wsStore.connection, (state, prev) => {
  if (state === 'ONLINE' && prev === 'OFFLINE') {
    Promise.all([loadPeripherals(), loadScreens()]).catch((err) => {
      console.error('Error resyncing after reconnect:', err);
    });
  }
});

// Screen edited/created/deleted in the admin editor -> refetch. Debounced:
// the GORM afterUpdate hook fires pre-commit and reorders touch several rows.
useWebSocketListener('evt_dashboard_screen_changed', _.debounce(loadScreens, 500));

onMounted(initialize);
onUnmounted(revokeBgUrls);
</script>

<style scoped>
.swiper {
  width: 100%;
  height: 100%;
}

.swiper-slide {
  text-align: center;
  background: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
}

.empty-state {
  height: 100vh;
}

</style>
