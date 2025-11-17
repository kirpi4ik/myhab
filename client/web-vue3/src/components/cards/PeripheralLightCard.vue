<template>
  <q-card class="peripheral-light-card text-white">
    <q-item>
      <!-- Light Bulb Avatar -->
      <q-item-section avatar>
        <q-avatar 
          size="60px" 
          :class="isLightOn ? 'shadow-10 bg-yellow-4' : 'shadow-10 bg-blue'"
        >
          <q-icon 
            :name="isLightOn ? 'mdi-lightbulb-on' : 'mdi-lightbulb-off'" 
            :color="isLightOn ? 'yellow-10' : 'white'" 
            size="40px" 
          />
        </q-avatar>
      </q-item-section>

      <!-- Light Info -->
      <q-item-section>
        <q-item-label class="text-weight-medium text-h5">
          {{ asset.data?.name || 'Unknown Light' }}
        </q-item-label>
        <q-item-label class="text-weight-light text-blue-grey-3">
          {{ asset.data?.description || '' }}
        </q-item-label>
        <q-item-label v-if="timeoutConfig || (isLightOn && asset.expiration)" class="text-weight-light text-teal-2 text-caption">
          <span v-if="timeoutConfig">
            [ timer: {{ formatDuration(Number(timeoutConfig.value) * 1000) }}
          </span>
          <span v-if="isLightOn && asset.expiration" class="text-weight-light text-blue-grey-3">
            | off at: {{ formatTime(asset.expiration) }}
          </span>
          <span v-if="timeoutConfig">]</span>
        </q-item-label>
      </q-item-section>

      <!-- Actions Menu -->
      <q-item-section side>
        <q-item-label>
          <q-btn-dropdown size="sm" flat round icon="settings" class="text-white">
            <q-list>
              <!-- Timeout Selector -->
              <timeout-selector
                @set-timeout="handleSetTimeout"
                @delete-timeout="handleDeleteTimeout"
              />
              
              <q-separator/>
              
              <!-- View Details -->
              <q-item 
                clickable 
                v-close-popup 
                @click="viewDetails"
              >
                <q-item-section avatar>
                  <q-icon name="mdi-information" color="info"/>
                </q-item-section>
                <q-item-section>
                  <q-item-label>Details</q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </q-btn-dropdown>
        </q-item-label>
        <q-item-label>
          <event-logger :peripheral="peripheral" />
        </q-item-label>
      </q-item-section>
    </q-item>

    <q-separator/>
    
    <!-- Toggle Switch -->
    <q-card-section>
      <div class="q-pa-sm text-grey-8">
        <toggle 
          :model-value="lightState" 
          @update:model-value="handleToggle" 
          :id="String(peripheral.id)"
        />
      </div>
    </q-card-section>
  </q-card>
</template>
<script>
import {computed, defineComponent, toRefs, watch, onMounted} from 'vue';
import {useRouter} from 'vue-router';

import {useApolloClient, useGlobalQueryLoading, useMutation} from '@vue/apollo-composable';
import {useWebSocketListeners} from '@/composables';

import {
  CACHE_DELETE,
  CACHE_GET_VALUE,
  CONFIGURATION_REMOVE_CONFIG_BY_KEY,
  CONFIGURATION_SET_VALUE,
  PERIPHERAL_GET_BY_ID,
} from '@/graphql/queries';
import {peripheralService} from '@/_services/controls';

import _ from 'lodash';
import {format} from 'date-fns';
import EventLogger from 'components/EventLogger.vue';
import humanizeDuration from 'humanize-duration';
import Toggle from '@vueform/toggle';
import TimeoutSelector from 'components/TimeoutSelector.vue';

export default defineComponent({
  name: 'PeripheralLightCard',
  components: {
    Toggle,
    EventLogger,
    TimeoutSelector,
  },
  props: {
    peripheral: {
      type: Object,
      required: true
    }
  },
  emits: ['onUpdate'],
  setup(props, { emit }) {
    const router = useRouter();
    const { client } = useApolloClient();
    const { peripheral: asset } = toRefs(props);

    /**
     * Get the port ID from connected ports
     */
    const portId = computed(() => {
      if (asset.value?.data?.connectedTo?.length > 0) {
        return asset.value.data.connectedTo[0].id;
      }
      return -1;
    });

    /**
     * Check if light is currently on
     * Reads from connectedTo[0].value which contains "ON" or "OFF"
     */
    const isLightOn = computed(() => {
      const portValue = asset.value?.data?.connectedTo?.[0]?.value;
      return portValue === 'ON';
    });

    /**
     * Get light state for toggle (read-only)
     * Reads from connectedTo[0].value which contains "ON" or "OFF"
     */
    const lightState = computed(() => {
      const portValue = asset.value?.data?.connectedTo?.[0]?.value;
      return portValue === 'ON';
    });

    /**
     * Get timeout configuration
     */
    const timeoutConfig = computed(() => {
      return asset.value?.data?.configurations?.find(cfg => cfg.key === 'key.on.timeout');
    });

    /**
     * Computed peripheral for emit
     */
    const compPeripheral = computed({
      get: () => props.peripheral,
      set: value => emit('onUpdate', value),
    });

    /**
     * Initialize state from connectedTo port value
     * This ensures peripheral.state is set correctly for peripheralService.toggle()
     */
    const initializeState = () => {
      if (asset.value?.data?.connectedTo?.[0]?.value) {
        const portValue = asset.value.data.connectedTo[0].value;
        const newState = portValue === 'ON';
        asset.value.state = newState;
        if (asset.value.data) {
          asset.value.data.state = newState;
        }
      }
    };

    // Initialize state when component mounts
    initializeState();

    // Watch for prop changes from parent and re-initialize state
    watch(
      () => props.peripheral,
      () => {
        initializeState();
      },
      { deep: true }
    );

    /**
     * Load peripheral details and expiration from cache
     */
    const loadDetails = async () => {
      try {
        const { data } = await client.query({
          query: PERIPHERAL_GET_BY_ID,
          variables: { id: asset.value.id },
          fetchPolicy: 'network-only',
        });

        let assetRW = _.cloneDeep(data.devicePeripheral);

        // Load expiration from cache
        if (portId.value !== -1) {
          try {
            const cacheData = await client.query({
              query: CACHE_GET_VALUE,
              variables: { cacheName: 'expiring', cacheKey: portId.value },
              fetchPolicy: 'network-only',
            });

            const cachedValue = cacheData.data?.cache?.cachedValue;
            // Only set expiration if cachedValue exists and is not null/empty
            if (cachedValue && cachedValue !== 'null') {
              assetRW.expiration = cachedValue;
            } else {
              assetRW.expiration = null;
            }
          } catch (error) {
            console.warn('Failed to load cache expiration:', error);
          }
        }

        // IMPORTANT: Initialize state in assetRW BEFORE assignment
        // The server doesn't return the 'state' property, so we must calculate it from portValue
        if (assetRW.connectedTo?.[0]?.value) {
          const portValue = assetRW.connectedTo[0].value;
          const calculatedState = portValue === 'ON';
          assetRW.state = calculatedState;
        }

        compPeripheral.value = assetRW;
      } catch (error) {
        console.error('Failed to load peripheral details:', error);
      }
    };

    // WebSocket event listeners
    useWebSocketListeners([
      {
        eventName: 'evt_port_value_persisted',
        callback: (payload) => {
          const newState = payload.p4 === 'ON';
          asset.value.value = payload.p4;
          asset.value.state = newState;
          
          // Update the actual port value that we read from
          if (asset.value.data?.connectedTo?.[0]) {
            asset.value.data.connectedTo[0].value = payload.p4;
          }
          if (asset.value.data) {
            asset.value.data.state = newState;
          }
          
          // Refresh details to get latest cache (expiration, etc.)
          // Use setTimeout to avoid race condition with state updates
          setTimeout(() => {
            loadDetails();
          }, 100);
        },
        filter: (payload) => portId.value === Number(payload.p2)
      },
      {
        eventName: 'evt_cfg_value_changed',
        callback: () => loadDetails(),
        filter: (payload) => 
          asset.value.id === Number(payload.p3) && payload.p2 === 'PERIPHERAL'
      }
    ]);

    // Mutations
    const { mutate: setTimeoutMutation } = useMutation(CONFIGURATION_SET_VALUE, {
      update: () => loadDetails(),
    });

    const { mutate: deleteCacheMutation } = useMutation(CACHE_DELETE);

    const { mutate: deleteTimeoutMutation } = useMutation(CONFIGURATION_REMOVE_CONFIG_BY_KEY, {
      update: () => {
        if (portId.value !== -1) {
          deleteCacheMutation({ cacheName: 'expiring', cacheKey: portId.value });
        }
        loadDetails();
      },
    });

    /**
     * Handle light toggle
     */
    const handleToggle = () => {
      peripheralService.toggle(asset.value, 'evt_light');
    };

    /**
     * Handle set timeout
     */
    const handleSetTimeout = (timeoutValue) => {
      setTimeoutMutation({
        key: 'key.on.timeout',
        value: timeoutValue,
        entityId: asset.value.id,
        entityType: 'PERIPHERAL'
      });
    };

    /**
     * Handle delete timeout
     */
    const handleDeleteTimeout = () => {
      deleteTimeoutMutation({
        entityId: asset.value.id,
        entityType: 'PERIPHERAL',
        key: 'key.on.timeout'
      });
    };

    /**
     * Navigate to peripheral details
     */
    const viewDetails = () => {
      router.push({ path: `/admin/peripherals/${props.peripheral.id}/view` });
    };

    /**
     * Format duration in human-readable format
     */
    const formatDuration = (milliseconds) => {
      return humanizeDuration(milliseconds, { largest: 2, language: 'en', round: true });
    };

    /**
     * Format time from timestamp
     */
    const formatTime = (timestamp) => {
      try {
        return format(new Date(Number(timestamp)), 'HH:mm');
      } catch (error) {
        return '--:--';
      }
    };

    // Lifecycle hook: Load details on mount
    onMounted(() => {
      loadDetails();
    });

    return {
      asset,
      isLightOn,
      lightState,
      timeoutConfig,
      portId,
      handleToggle,
      handleSetTimeout,
      handleDeleteTimeout,
      viewDetails,
      formatDuration,
      formatTime,
      loading: useGlobalQueryLoading(),
    };
  },
});

</script>

<style scoped>
.peripheral-light-card {
  background: linear-gradient(135deg, #156cb8, #60a3c2);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.peripheral-light-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
}
</style>

<style src="@vueform/toggle/themes/default.css"></style>
