<template>
  <q-card class="navimow-card" :class="{ 'navimow-card--offline': isOffline }">
    <!-- Header: name + device-status chip; clickable → device admin -->
    <q-item class="navimow-header" clickable @click="openDeviceView">
      <q-item-section avatar>
        <q-avatar>
          <q-icon name="mdi-robot-mower" size="lg"/>
        </q-avatar>
      </q-item-section>
      <q-item-section>
        <q-item-label class="text-h6">{{ device?.name || 'Navimow' }}</q-item-label>
        <q-item-label caption class="text-green-2">{{ device?.code || '—' }}</q-item-label>
      </q-item-section>
      <q-item-section side>
        <q-chip
          dense
          :color="deviceStatusColor"
          text-color="white"
          :icon="deviceStatusIcon"
          size="sm"
        >{{ deviceStatusLabel }}</q-chip>
      </q-item-section>
    </q-item>

    <!-- Hero: mower image on a tinted backdrop -->
    <q-card-section class="navimow-hero">
      <img src="~assets/navimow.png" alt="Navimow mower" class="navimow-hero__img"/>
    </q-card-section>

    <q-separator dark/>

    <!-- State + battery summary -->
    <q-card-section class="navimow-status">
      <div class="row items-center q-mb-md">
        <q-chip
          :color="stateChip.color"
          text-color="white"
          :icon="stateChip.icon"
          size="md"
          class="state-chip"
        >{{ stateChip.label }}</q-chip>
        <q-space/>
        <div v-if="batteryLabel" class="text-caption text-grey-3 q-ml-sm">{{ batteryLabel }}</div>
      </div>

      <div class="row items-center no-wrap q-gutter-sm">
        <q-icon :name="batteryIcon" :color="batteryColor" size="md"/>
        <q-linear-progress
          :value="batteryValue / 100"
          :color="batteryColor"
          size="14px"
          rounded
          class="col"
          track-color="grey-9"
        />
        <div class="text-weight-medium battery-pct">{{ batteryValue != null ? batteryValue + '%' : '—' }}</div>
      </div>
    </q-card-section>

    <!-- Offline banner — token expired or sync failing -->
    <q-card-section v-if="isOffline" class="q-pa-sm">
      <q-banner dense rounded class="bg-deep-orange-9 text-white">
        <template v-slot:avatar>
          <q-icon name="mdi-key-alert" color="white"/>
        </template>
        Mower is offline — token may have expired.
        <template v-slot:action>
          <q-btn flat dense color="white" label="Reconnect" no-caps @click.stop="openDeviceEdit"/>
        </template>
      </q-banner>
    </q-card-section>

    <q-separator dark/>

    <!-- Controls: Start / Pause / Resume / Dock -->
    <q-card-actions class="navimow-controls justify-around q-pa-md">
      <q-btn
        v-for="ctrl in controls"
        :key="ctrl.action"
        flat
        round
        :icon="ctrl.icon"
        :color="ctrl.color"
        :disable="isOffline || !ctrl.enabled || sendingAction !== null"
        :loading="sendingAction === ctrl.action"
        @click="onCommand(ctrl.action)"
      >
        <q-tooltip>{{ ctrl.label }}</q-tooltip>
      </q-btn>
    </q-card-actions>

    <!-- Footer -->
    <q-card-section class="navimow-footer text-center text-caption text-green-2">
      <q-spinner-dots v-if="loading && !device" size="sm" color="white"/>
      <span v-else>Last update: {{ lastUpdateLabel }}</span>
    </q-card-section>
  </q-card>
</template>

<script>
import { defineComponent, onMounted, ref, computed } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';
import { useWebSocketListener } from '@/composables';
import { DEVICE_GET_BY_ID_WITH_PORT_VALUES, MOWER_COMMAND } from '@/graphql/queries';
import _ from 'lodash';

/**
 * Canonical mower states (matches the lowercase set populated by
 * NavimowInfoSyncJob.RAW_STATE_TO_CANONICAL on the backend). Used to:
 *  - render a coloured chip with the right icon
 *  - decide which control buttons are enabled
 */
const STATE_META = {
  docked:    { label: 'Docked',    icon: 'mdi-home-import-outline', color: 'positive'    },
  charging:  { label: 'Charging',  icon: 'mdi-battery-charging',     color: 'positive'    },
  idle:      { label: 'Idle',      icon: 'mdi-pause-circle-outline', color: 'grey-6'      },
  mowing:    { label: 'Mowing',    icon: 'mdi-robot-mower',          color: 'blue-7'      },
  paused:    { label: 'Paused',    icon: 'mdi-pause',                color: 'warning'     },
  returning: { label: 'Returning', icon: 'mdi-arrow-u-left-top',     color: 'warning'     },
  error:     { label: 'Error',     icon: 'mdi-alert-circle',         color: 'negative'    },
  unknown:   { label: 'Unknown',   icon: 'mdi-help-circle-outline',  color: 'grey-6'      },
};

/**
 * Which actions are valid given the current state. Mower will reject otherwise,
 * but greying out clients-side gives clearer affordance. `dock` is allowed as a
 * fallback from almost any state so the user can always send the mower home.
 */
const ENABLED_ACTIONS = {
  docked:    new Set(['START']),
  charging:  new Set(['START']),
  idle:      new Set(['START', 'DOCK']),
  mowing:    new Set(['PAUSE', 'DOCK']),
  paused:    new Set(['RESUME', 'DOCK']),
  returning: new Set(['DOCK']),
  error:     new Set(['DOCK']),
  unknown:   new Set(['START', 'PAUSE', 'RESUME', 'DOCK']),
};

const CONTROL_DEFS = [
  { action: 'START',  label: 'Start mowing',     icon: 'mdi-play',                color: 'positive' },
  { action: 'PAUSE',  label: 'Pause',            icon: 'mdi-pause',               color: 'warning'  },
  { action: 'RESUME', label: 'Resume',           icon: 'mdi-skip-forward',        color: 'positive' },
  { action: 'DOCK',   label: 'Return to dock',   icon: 'mdi-home-import-outline', color: 'info'     },
];

export default defineComponent({
  name: 'NavimowWidget',
  props: {
    deviceId: { type: Number, required: true },
  },
  setup(props) {
    const $q = useQuasar();
    const router = useRouter();
    const { client } = useApolloClient();

    const device = ref(null);
    const devicePorts = ref({});
    const portIds = ref([]);
    const loading = ref(false);
    const sendingAction = ref(null);
    const lastUpdate = ref(null);

    const loadDetails = () => {
      loading.value = true;
      client.query({
        query: DEVICE_GET_BY_ID_WITH_PORT_VALUES,
        variables: { id: props.deviceId },
        fetchPolicy: 'network-only',
      }).then(({ data }) => {
        device.value = data.device;
        devicePorts.value = _.reduce(data.device?.ports || [], (h, p) => {
          h[p.internalRef] = p;
          return h;
        }, {});
        portIds.value = (data.device?.ports || []).map(p => p.id);
        lastUpdate.value = new Date();
      }).catch((err) => {
        console.error('NavimowWidget: failed to load device', err);
      }).finally(() => {
        loading.value = false;
      });
    };

    const getPortValue = (ref) => devicePorts.value[ref]?.value ?? null;

    // -- Derived state --------------------------------------------------------
    const isOffline = computed(() => device.value?.status !== 'ONLINE');

    const stateRaw = computed(() => {
      const v = getPortValue('navimow.state');
      return v ? String(v).toLowerCase().trim() : 'unknown';
    });
    const stateChip = computed(() => STATE_META[stateRaw.value] || STATE_META.unknown);

    const batteryValue = computed(() => {
      const v = getPortValue('navimow.battery');
      const n = v != null ? parseInt(v, 10) : null;
      return Number.isFinite(n) ? Math.max(0, Math.min(100, n)) : null;
    });
    const batteryLabel = computed(() => getPortValue('navimow.batteryLabel'));
    const batteryColor = computed(() => {
      const v = batteryValue.value;
      if (v == null) return 'grey-6';
      if (v < 20) return 'negative';
      if (v < 50) return 'warning';
      return 'positive';
    });
    const batteryIcon = computed(() => {
      const v = batteryValue.value;
      if (v == null) return 'mdi-battery-unknown';
      if (stateRaw.value === 'charging') return 'mdi-battery-charging';
      if (v >= 90) return 'mdi-battery';
      if (v >= 60) return 'mdi-battery-70';
      if (v >= 30) return 'mdi-battery-40';
      if (v >= 10) return 'mdi-battery-20';
      return 'mdi-battery-alert';
    });

    const controls = computed(() => {
      const allowed = ENABLED_ACTIONS[stateRaw.value] || ENABLED_ACTIONS.unknown;
      return CONTROL_DEFS.map(c => ({ ...c, enabled: allowed.has(c.action) }));
    });

    // -- Device-status chip (separate from mower state — this is the myHAB-side
    //    "are we successfully syncing" indicator)
    const deviceStatusColor = computed(() => {
      switch (device.value?.status) {
        case 'ONLINE':   return 'positive';
        case 'OFFLINE':  return 'negative';
        case 'DISABLED': return 'grey-6';
        default:         return 'grey-6';
      }
    });
    const deviceStatusIcon = computed(() => {
      switch (device.value?.status) {
        case 'ONLINE':   return 'mdi-cloud-check';
        case 'OFFLINE':  return 'mdi-cloud-off-outline';
        case 'DISABLED': return 'mdi-cancel';
        default:         return 'mdi-help-circle';
      }
    });
    const deviceStatusLabel = computed(() => {
      const s = device.value?.status;
      return s ? s.charAt(0) + s.slice(1).toLowerCase() : 'Loading';
    });

    const lastUpdateLabel = computed(() => {
      if (!lastUpdate.value) return '—';
      return lastUpdate.value.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
    });

    // -- Actions --------------------------------------------------------------
    const onCommand = async (action) => {
      sendingAction.value = action;
      try {
        const { data } = await client.mutate({
          mutation: MOWER_COMMAND,
          variables: { deviceId: props.deviceId, action },
          fetchPolicy: 'no-cache',
        });
        const r = data?.mowerCommand;
        if (r?.success) {
          $q.notify({
            type: 'positive',
            message: `Command sent: ${action}`,
            icon: 'mdi-check-circle',
            timeout: 2000,
          });
          // Refetch shortly so the UI catches the resulting state transition.
          setTimeout(loadDetails, 3000);
        } else {
          $q.notify({
            type: 'negative',
            message: `${action} rejected: ${r?.error || 'unknown error'}`,
            icon: 'mdi-alert-circle',
            timeout: 6000,
          });
        }
      } catch (err) {
        $q.notify({
          type: 'negative',
          message: `${action} failed: ${err.message}`,
          icon: 'mdi-alert-circle',
          timeout: 6000,
        });
      } finally {
        sendingAction.value = null;
      }
    };

    const openDeviceView = () => router.push(`/admin/devices/${props.deviceId}/view`);
    const openDeviceEdit = () => router.push(`/admin/devices/${props.deviceId}/edit`);

    // -- Live updates via the existing port-value WebSocket ------------------
    useWebSocketListener('evt_port_value_persisted', (payload) => {
      if (portIds.value.includes(Number(payload.p2))) {
        loadDetails();
      }
    });

    onMounted(loadDetails);

    return {
      device,
      loading,
      sendingAction,
      isOffline,
      stateChip,
      batteryValue,
      batteryLabel,
      batteryColor,
      batteryIcon,
      controls,
      deviceStatusColor,
      deviceStatusIcon,
      deviceStatusLabel,
      lastUpdateLabel,
      onCommand,
      openDeviceView,
      openDeviceEdit,
    };
  },
});
</script>

<style scoped>
.navimow-card {
  background: linear-gradient(135deg, #2e7d32 0%, #1b5e20 60%, #0d3a12 100%);
  color: white;
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.navimow-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.3);
}
.navimow-card--offline {
  /* Wash to a duller palette so the visual state matches the sync state. */
  background: linear-gradient(135deg, #4e5d52 0%, #2e3a32 100%);
}
.navimow-header {
  background: rgba(255, 255, 255, 0.08);
  cursor: pointer;
}
.navimow-header:hover {
  background: rgba(255, 255, 255, 0.14);
}
.navimow-hero {
  display: flex;
  justify-content: center;
  background: radial-gradient(circle at center, rgba(255,255,255,0.18) 0%, rgba(255,255,255,0) 70%);
  padding: 16px 12px;
}
.navimow-hero__img {
  max-width: 180px;
  width: 60%;
  height: auto;
  filter: drop-shadow(0 6px 12px rgba(0, 0, 0, 0.45));
  transition: transform 0.2s ease;
}
.navimow-card:hover .navimow-hero__img {
  transform: translateY(-2px) scale(1.02);
}
.navimow-status {
  background: rgba(0, 0, 0, 0.10);
}
.state-chip {
  font-weight: 600;
}
.battery-pct {
  min-width: 48px;
  text-align: right;
}
.navimow-controls {
  background: rgba(0, 0, 0, 0.18);
}
.navimow-footer {
  background: rgba(0, 0, 0, 0.20);
  padding: 8px;
  opacity: 0.85;
}

@media (max-width: 600px) {
  .navimow-hero__img {
    max-width: 140px;
  }
}
</style>
