<template>
  <span>
    <q-btn
      icon="mdi-ip-network"
      :label="compactLabel"
      :flat="!buttonRaised"
      :unelevated="buttonRaised"
      :dense="dense"
      :size="size"
      color="primary"
      no-caps
      :disable="disabled"
      @click="openDialog"
    >
      <q-tooltip>Fetch the device's current IP and update its network address.</q-tooltip>
    </q-btn>

    <q-dialog v-model="dialogVisible" persistent transition-show="jump-up" transition-hide="jump-down">
      <q-card style="min-width: 480px; max-width: 600px;">
        <q-bar class="bg-primary text-white">
          <q-icon name="mdi-ip-network" size="20px"/>
          <div class="q-ml-sm text-weight-bold">Update IP Address</div>
          <q-space/>
          <q-btn dense flat icon="close" @click="dialogVisible = false" :disable="saving"/>
        </q-bar>

        <q-card-section>
          <div class="text-body2 q-mb-sm">
            <strong>{{ device.code }}</strong>
            <q-badge v-if="device.model" color="info" :label="device.model" class="q-ml-sm"/>
          </div>
          <div class="text-caption text-grey-7">
            Current IP:
            <span v-if="device.networkAddress?.ip">{{ device.networkAddress.ip }}</span>
            <span v-else class="text-grey-5">none</span>
          </div>
        </q-card-section>

        <q-separator/>

        <!-- ESP32: read from the MQTT-fed ip_address port, with on-demand broker fetch as a fallback -->
        <template v-if="strategy === 'mqtt-port'">
          <q-card-section>
            <div class="text-subtitle2 q-mb-sm">
              <q-icon name="mdi-broadcast" class="q-mr-xs"/>
              From MQTT
            </div>

            <!-- Cached port value (DB), if present -->
            <div v-if="mqttPortValue" class="row items-center q-gutter-md q-mb-md">
              <q-icon name="mdi-check-circle" color="positive" size="md"/>
              <div class="col">
                <div class="text-body1">{{ mqttPortValue }}</div>
                <div class="text-caption text-grey-7">
                  cached from port #{{ mqttPort.id }} (<code>{{ mqttPort.internalRef }}</code>)
                </div>
              </div>
            </div>

            <!-- Live broker fetch result (if user clicked the button) -->
            <div v-if="brokerValue" class="row items-center q-gutter-md q-mb-md">
              <q-icon name="mdi-broadcast" color="positive" size="md"/>
              <div class="col">
                <div class="text-body1">{{ brokerValue }}</div>
                <div class="text-caption text-grey-7">
                  live from MQTT broker (topic <code>{{ brokerTopic }}</code>)
                </div>
              </div>
            </div>

            <!-- Empty state + broker-fetch trigger -->
            <div v-if="!mqttPortValue && !brokerValue" class="q-mb-md">
              <div class="text-grey-7 q-mb-sm">
                <q-icon name="mdi-information-outline" class="q-mr-xs"/>
                No <code>esp_ip_address</code> port value found on this device. The device may not
                have reported its IP via MQTT yet, or the per-device port autoimport is disabled.
              </div>
              <q-btn
                outline
                color="primary"
                icon="mdi-broadcast"
                label="Fetch from MQTT broker"
                :loading="brokerFetching"
                :disable="brokerFetching || saving"
                @click="fetchFromBroker"
                no-caps
              >
                <q-tooltip>Subscribe briefly to the broker's IP-broadcast topic and read whatever value it has.</q-tooltip>
              </q-btn>
              <div v-if="brokerError" class="text-negative q-mt-sm">
                <q-icon name="mdi-alert-circle" class="q-mr-xs"/>{{ brokerError }}
              </div>
            </div>
          </q-card-section>

          <q-card-actions align="right" class="q-pa-md">
            <q-btn flat label="Close" color="grey-7" @click="dialogVisible = false" :disable="saving"/>
            <q-btn
              unelevated
              color="primary"
              icon="mdi-content-save"
              label="Apply"
              :loading="saving"
              :disable="!effectiveMqttIp || effectiveMqttIp === device.networkAddress?.ip"
              @click="applyIp(effectiveMqttIp)"
              no-caps
            />
          </q-card-actions>
        </template>

        <!-- MEGAD: run UDP discovery and let the user pick -->
        <template v-else-if="strategy === 'udp-discover'">
          <q-card-section>
            <div class="row items-center q-mb-sm">
              <div class="text-subtitle2">
                <q-icon name="mdi-radar" class="q-mr-xs"/>
                UDP discovery
              </div>
              <q-space/>
              <q-btn
                flat
                dense
                no-caps
                icon="mdi-refresh"
                label="Re-scan"
                color="primary"
                :loading="discovering"
                :disable="discovering || saving"
                @click="runDiscovery"
              />
            </div>

            <div v-if="discovering" class="text-center q-pa-md">
              <q-spinner-radio size="32px" color="primary"/>
              <div class="text-caption text-grey-7 q-mt-sm">Broadcasting on port 52000…</div>
            </div>

            <div v-else-if="discoverError" class="text-negative">
              <q-icon name="mdi-alert-circle" class="q-mr-xs"/>{{ discoverError }}
            </div>

            <div v-else-if="!discovered.length" class="text-grey-6">
              No MegaD controllers responded. Make sure the device is powered on and on the same network.
            </div>

            <q-list v-else bordered separator class="rounded-borders">
              <q-item
                v-for="(d, idx) in discovered"
                :key="idx"
                clickable
                v-ripple
                :active="selectedIp === d.ip"
                active-class="bg-blue-1"
                @click="selectedIp = d.ip"
              >
                <q-item-section avatar>
                  <q-icon
                    :name="selectedIp === d.ip ? 'mdi-radiobox-marked' : 'mdi-radiobox-blank'"
                    :color="selectedIp === d.ip ? 'primary' : 'grey-5'"
                  />
                </q-item-section>
                <q-item-section>
                  <q-item-label>{{ d.ip }}</q-item-label>
                  <q-item-label caption>
                    <q-badge
                      :color="d.bootloaderMode ? 'orange' : 'teal'"
                      :label="d.bootloaderMode ? 'Bootloader' : 'MegaD'"
                      class="q-mr-xs"
                    />
                    <span v-if="d.mqttId">
                      MQTT ID: <strong>{{ d.mqttId }}</strong>
                      <q-badge
                        v-if="d.mqttId === device.code"
                        color="positive"
                        label="matches device.code"
                        class="q-ml-xs"
                      />
                    </span>
                  </q-item-label>
                </q-item-section>
                <q-item-section side v-if="d.ip === device.networkAddress?.ip">
                  <q-badge color="grey-6" label="current"/>
                </q-item-section>
              </q-item>
            </q-list>
          </q-card-section>

          <q-card-actions align="right" class="q-pa-md">
            <q-btn flat label="Close" color="grey-7" @click="dialogVisible = false" :disable="saving"/>
            <q-btn
              unelevated
              color="primary"
              icon="mdi-content-save"
              label="Apply"
              :loading="saving"
              :disable="!selectedIp || selectedIp === device.networkAddress?.ip"
              @click="applyIp(selectedIp)"
              no-caps
            />
          </q-card-actions>
        </template>

        <!-- Manual fallback for models without an automatic source -->
        <template v-else>
          <q-card-section>
            <q-input
              v-model="manualIp"
              label="IP address"
              hint="Enter the IP manually for this model"
              filled
              dense
            >
              <template v-slot:prepend>
                <q-icon name="mdi-ip-network"/>
              </template>
            </q-input>
          </q-card-section>
          <q-card-actions align="right" class="q-pa-md">
            <q-btn flat label="Close" color="grey-7" @click="dialogVisible = false" :disable="saving"/>
            <q-btn
              unelevated
              color="primary"
              icon="mdi-content-save"
              label="Apply"
              :loading="saving"
              :disable="!manualIp || manualIp === device.networkAddress?.ip"
              @click="applyIp(manualIp)"
              no-caps
            />
          </q-card-actions>
        </template>
      </q-card>
    </q-dialog>
  </span>
</template>

<script>
import { defineComponent, ref, computed } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { useQuasar } from 'quasar';
import { DEVICES_DISCOVER, DEVICE_FETCH_IP_FROM_MQTT, DEVICE_UPDATE_CUSTOM } from '@/graphql/queries';

/**
 * Per-model strategy for fetching the device's current IP.
 *
 * Add new models here when a new auto-detect path is available. Falls back to
 * 'manual' (free-text input) when the model isn't listed.
 */
const STRATEGY_BY_MODEL = {
  ESP32: 'mqtt-port',
  MEGAD_2561_RTC: 'udp-discover',
};

const MQTT_IP_PORT_REFS = ['ip_address', 'esp_ip_address'];

export default defineComponent({
  name: 'DeviceIpUpdater',
  props: {
    /** Device payload from DEVICE_GET_BY_ID_CHILDS (must include id, code, model, ports, networkAddress). */
    device: { type: Object, required: true },
    /** Render the button as raised/unelevated instead of flat. */
    buttonRaised: { type: Boolean, default: false },
    /** Compact label (defaults to "Update IP"). Set to '' for icon-only. */
    label: { type: String, default: 'Update IP' },
    dense: { type: Boolean, default: false },
    size: { type: String, default: 'md' },
    disabled: { type: Boolean, default: false },
  },
  emits: ['updated'],
  setup(props, { emit }) {
    const { client } = useApolloClient();
    const $q = useQuasar();

    const dialogVisible = ref(false);
    const saving = ref(false);

    // UDP discover state
    const discovering = ref(false);
    const discoverError = ref(null);
    const discovered = ref([]);
    const selectedIp = ref(null);

    // Manual fallback
    const manualIp = ref('');

    // On-demand MQTT broker fetch state (fallback when no DevicePort cached the IP)
    const brokerFetching = ref(false);
    const brokerError = ref(null);
    const brokerValue = ref(null);
    const brokerTopic = ref(null);

    const strategy = computed(() => STRATEGY_BY_MODEL[props.device?.model] || 'manual');

    const mqttPort = computed(() => {
      return props.device?.ports?.find(p => MQTT_IP_PORT_REFS.includes(p.internalRef)) || null;
    });

    const mqttPortValue = computed(() => {
      const v = mqttPort.value?.value;
      return v ? String(v).trim() : null;
    });

    /** Apply target for the mqtt-port strategy: prefer the live broker fetch if present, else the cached port value. */
    const effectiveMqttIp = computed(() => brokerValue.value || mqttPortValue.value);

    const compactLabel = computed(() => props.label);

    const openDialog = () => {
      dialogVisible.value = true;
      selectedIp.value = null;
      manualIp.value = props.device?.networkAddress?.ip || '';
      discovered.value = [];
      discoverError.value = null;
      brokerValue.value = null;
      brokerTopic.value = null;
      brokerError.value = null;
      brokerFetching.value = false;
      if (strategy.value === 'udp-discover') {
        runDiscovery();
      }
    };

    /**
     * Subscribe briefly to the broker (server-side) for the device's
     * IP-broadcast topic and use whatever value arrives. Used as a fallback
     * when no `esp_ip_address` DevicePort has cached the IP yet — e.g. for
     * a freshly added device whose per-device port autoimport is off.
     */
    const fetchFromBroker = async () => {
      brokerFetching.value = true;
      brokerError.value = null;
      brokerValue.value = null;
      brokerTopic.value = null;
      try {
        const { data } = await client.query({
          query: DEVICE_FETCH_IP_FROM_MQTT,
          variables: { deviceCode: props.device.code },
          fetchPolicy: 'no-cache',
        });
        const r = data?.deviceFetchIpFromMqtt;
        if (r?.ip) {
          brokerValue.value = r.ip;
          brokerTopic.value = r.topic;
        } else {
          brokerError.value = r?.error || 'No value received from the broker.';
        }
      } catch (err) {
        brokerError.value = err.message;
      } finally {
        brokerFetching.value = false;
      }
    };

    const runDiscovery = async () => {
      discovering.value = true;
      discoverError.value = null;
      discovered.value = [];
      selectedIp.value = null;
      try {
        const { data } = await client.query({
          query: DEVICES_DISCOVER,
          fetchPolicy: 'no-cache',
        });
        discovered.value = data?.discoveredDevices || [];
        // Pre-select the entry that matches device.code (mqttId), if any.
        const match = discovered.value.find(d => d.mqttId && d.mqttId === props.device.code);
        if (match) selectedIp.value = match.ip;
      } catch (error) {
        discoverError.value = error.message;
      } finally {
        discovering.value = false;
      }
    };

    const applyIp = async (newIp) => {
      if (!newIp) return;
      saving.value = true;
      try {
        const currentAddr = props.device?.networkAddress || {};
        await client.mutate({
          mutation: DEVICE_UPDATE_CUSTOM,
          variables: {
            id: props.device.id,
            device: {
              networkAddress: {
                ip: newIp,
                gateway: currentAddr.gateway || null,
                port: currentAddr.port || null,
              },
            },
          },
          fetchPolicy: 'no-cache',
        });
        $q.notify({
          type: 'positive',
          message: `IP updated to ${newIp}`,
          icon: 'mdi-check-circle',
        });
        dialogVisible.value = false;
        emit('updated', newIp);
      } catch (error) {
        $q.notify({
          type: 'negative',
          message: `Failed to update IP: ${error.message}`,
          icon: 'mdi-alert-circle',
        });
      } finally {
        saving.value = false;
      }
    };

    return {
      dialogVisible,
      saving,
      discovering,
      discoverError,
      discovered,
      selectedIp,
      manualIp,
      strategy,
      mqttPort,
      mqttPortValue,
      effectiveMqttIp,
      compactLabel,
      brokerFetching,
      brokerError,
      brokerValue,
      brokerTopic,
      fetchFromBroker,
      openDialog,
      runDiscovery,
      applyIp,
    };
  },
});
</script>
