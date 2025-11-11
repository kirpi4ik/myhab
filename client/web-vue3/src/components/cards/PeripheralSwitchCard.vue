<template>
  <q-card class="peripheral-switch-card text-white">
    <q-item>
      <!-- Switch Avatar -->
      <q-item-section avatar>
        <q-avatar 
          size="60px" 
          :class="isSwitchOn ? 'shadow-10 bg-green-4' : 'shadow-10 bg-grey-8'"
        >
          <q-icon 
            :name="isSwitchOn ? 'mdi-electric-switch' : 'mdi-electric-switch-closed'" 
            :color="isSwitchOn ? 'green-10' : 'white'" 
            size="40px" 
          />
        </q-avatar>
      </q-item-section>

      <!-- Switch Info -->
      <q-item-section>
        <q-item-label class="text-weight-medium text-h5">
          {{ asset.data?.name || 'Unknown Switch' }}
        </q-item-label>
        <q-item-label class="text-weight-light text-blue-grey-3">
          {{ asset.data?.description || '' }}
        </q-item-label>
        <q-item-label v-if="timeoutConfig || asset.expiration" class="text-weight-light text-teal-2 text-caption">
          <span v-if="timeoutConfig">
            [ timer: {{ formatDuration(Number(timeoutConfig.value) * 1000) }}
          </span>
          <span v-if="asset.expiration" class="text-weight-light text-blue-grey-3">
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
          v-model="switchState" 
          @change="handleToggle" 
          :id="String(peripheral.id)"
        />
      </div>
    </q-card-section>
  </q-card>
</template>

<script>
import {computed, defineComponent, toRefs} from 'vue';
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
import {lightService} from '@/_services/controls';

import _ from 'lodash';
import {format} from 'date-fns';
import EventLogger from 'components/EventLogger.vue';
import humanizeDuration from 'humanize-duration';
import Toggle from '@vueform/toggle';
import TimeoutSelector from 'components/TimeoutSelector.vue';

export default defineComponent({
  name: 'PeripheralSwitchCard',
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
     * Check if switch is currently on
     */
    const isSwitchOn = computed(() => asset.value?.state === true);

    /**
     * Get/Set switch state for toggle
     */
    const switchState = computed({
      get: () => asset.value?.data?.state === true,
      set: (value) => {
        if (asset.value?.data) {
          asset.value.data.state = value;
        }
      }
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

            if (cacheData.data?.cache?.cachedValue) {
              assetRW.expiration = cacheData.data.cache.cachedValue;
            }
          } catch (error) {
            console.warn('Failed to load cache expiration:', error);
          }
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
          if (asset.value.data) {
            asset.value.data.state = newState;
          }
          loadDetails();
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
     * Handle switch toggle
     */
    const handleToggle = () => {
      lightService.toggle(asset.value);
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

    return {
      asset,
      isSwitchOn,
      switchState,
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
.peripheral-switch-card {
  background: linear-gradient(135deg, #2e7d32, #66bb6a);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.peripheral-switch-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
}
</style>

<style src="@vueform/toggle/themes/default.css"></style>

