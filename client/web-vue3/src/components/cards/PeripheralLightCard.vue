<template>
  <q-card class="peripheral-light-card text-white" :class="{ 'light-on': isLightOn, 'light-off': !isLightOn }">
    <!-- Status Indicator Badge -->
    <div class="status-indicator" :class="{ 'indicator-on': isLightOn, 'indicator-off': !isLightOn }">
      <q-icon :name="isLightOn ? 'mdi-lightbulb-on' : 'mdi-lightbulb-off'" size="16px" class="q-mr-xs"/>
      <span class="text-caption text-weight-medium">{{ isLightOn ? $t('light_card.on') : $t('light_card.off') }}</span>
    </div>

    <q-item class="card-content">
      <!-- Light Bulb Avatar -->
      <q-item-section avatar>
        <q-avatar 
          size="60px" 
          class="light-avatar"
          :class="isLightOn ? 'avatar-on' : 'avatar-off'"
        >
          <q-icon 
            :name="isLightOn ? 'mdi-lightbulb-on' : 'mdi-lightbulb-off'" 
            :color="isLightOn ? 'yellow-10' : 'white'" 
            size="40px" 
          />
          <div v-if="isLightOn" class="light-glow"></div>
        </q-avatar>
      </q-item-section>

      <!-- Light Info -->
      <q-item-section>
        <q-item-label class="text-weight-medium text-h5 light-name">
          {{ asset.data?.name || 'Unknown Light' }}
          <q-badge v-if="isDeviceOffline" color="red-7" class="q-ml-sm offline-badge">
            <q-icon name="mdi-lan-disconnect" size="12px" class="q-mr-xs"/>
            OFFLINE
          </q-badge>
        </q-item-label>
        <q-item-label v-if="asset.data?.description" class="text-weight-light text-blue-grey-2 light-description">
          <q-icon name="mdi-information-outline" size="14px" class="q-mr-xs"/>
          {{ asset.data.description }}
        </q-item-label>
        
        <!-- Timer Info -->
        <q-item-label v-if="timeoutConfig || (isLightOn && asset.expiration)" class="q-mt-xs">
          <q-chip
            size="sm"
            :color="isLightOn ? 'amber-9' : 'blue-grey-8'"
            text-color="white"
            icon="mdi-timer-outline"
            class="timer-chip"
          >
            <span v-if="timeoutConfig" class="timer-text">
              {{ formatDuration(Number(timeoutConfig.value) * 1000) }}
            </span>
            <span v-if="isLightOn && asset.expiration" class="timer-text">
              <q-icon name="mdi-clock-outline" size="14px" class="q-mr-xs"/>
              {{ formatTime(asset.expiration) }}
            </span>
          </q-chip>
        </q-item-label>
      </q-item-section>

      <!-- Actions Menu -->
      <q-item-section side>
        <q-item-label>
          <q-btn-dropdown 
            size="sm" 
            flat 
            round 
            icon="mdi-tune-variant" 
            class="text-white action-btn"
          >
            <q-list>
              <!-- Timeout Selector -->
              <timeout-selector
                :current-timeout="timeoutConfig ? Number(timeoutConfig.value) : null"
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
                  <q-item-label>{{ $t('light_card.details') }}</q-item-label>
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

    <q-separator :class="isLightOn ? 'separator-glow' : ''" />
    
    <!-- Toggle Switch -->
    <q-card-section class="toggle-section">
      <div class="toggle-container">
        <div class="toggle-wrapper">
          <div class="toggle-label-wrapper toggle-label-off-wrapper" :class="{ 'active': !isLightOn }">
            <q-icon name="mdi-lightbulb-off-outline" size="16px"/>
            <span class="label-text">{{ $t('light_card.off') }}</span>
          </div>
          
          <toggle 
            :model-value="lightState" 
            @update:model-value="handleToggle" 
            :id="String(peripheral.id)"
            :disabled="isDeviceOffline"
            class="light-toggle"
          />
          
          <div class="toggle-label-wrapper toggle-label-on-wrapper" :class="{ 'active': isLightOn }">
            <q-icon name="mdi-lightbulb-on" size="16px"/>
            <span class="label-text">{{ $t('light_card.on') }}</span>
          </div>
        </div>
        
        <div v-if="isDeviceOffline" class="offline-message">
          <q-icon name="mdi-alert-circle-outline" size="18px" class="q-mr-xs"/>
          {{ $t('light_card.device_offline') }}
        </div>
      </div>
    </q-card-section>

    <!-- Bottom Accent Bar -->
    <div class="accent-bar" :class="{ 'accent-active': isLightOn }"></div>
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
     * Check if device is offline
     * Device status comes from connectedTo[0].device.status
     */
    const isDeviceOffline = computed(() => {
      const deviceStatus = asset.value?.data?.connectedTo?.[0]?.device?.status;
      return deviceStatus === 'OFFLINE' || deviceStatus === 'DISABLED';
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
      const config = asset.value?.data?.configurations?.find(cfg => cfg.key === 'key.on.timeout');
      return config;
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
            console.error('Failed to load cache expiration:', error);
          }
        }

        // IMPORTANT: Initialize state in assetRW BEFORE assignment
        // The server doesn't return the 'state' property, so we must calculate it from portValue
        if (assetRW.connectedTo?.[0]?.value) {
          const portValue = assetRW.connectedTo[0].value;
          assetRW.state = portValue === 'ON';
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
        callback: (payload) => {
          loadDetails();
        },
        filter: (payload) => 
          asset.value.id === Number(payload.p3) && payload.p2 === 'PERIPHERAL'
      }
    ]);

    // Mutations
    const { mutate: setTimeoutMutation } = useMutation(CONFIGURATION_SET_VALUE, {
      update: () => {
        loadDetails();
      },
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
      isDeviceOffline,
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

<style scoped lang="scss">
// CSS Custom Properties for easier theming and maintenance
.peripheral-light-card {
  // Define custom properties for reusability
  --card-border-radius: 12px;
  --card-padding: 16px;
  --transition-timing: cubic-bezier(0.4, 0, 0.2, 1);
  --transition-duration: 0.3s;
  
  background: 
    radial-gradient(3px 3px at 20% 30%, rgba(255, 255, 255, 0.4), transparent),
    radial-gradient(2px 2px at 60% 70%, rgba(255, 255, 255, 0.35), transparent),
    radial-gradient(2px 2px at 50% 50%, rgba(255, 255, 255, 0.3), transparent),
    radial-gradient(2px 2px at 80% 10%, rgba(255, 255, 255, 0.45), transparent),
    radial-gradient(3px 3px at 90% 60%, rgba(255, 255, 255, 0.35), transparent),
    radial-gradient(2px 2px at 30% 80%, rgba(255, 255, 255, 0.3), transparent),
    radial-gradient(2px 2px at 15% 60%, rgba(255, 255, 255, 0.25), transparent),
    radial-gradient(3px 3px at 70% 30%, rgba(255, 255, 255, 0.4), transparent),
    radial-gradient(2px 2px at 40% 15%, rgba(255, 255, 255, 0.3), transparent),
    radial-gradient(2px 2px at 85% 85%, rgba(255, 255, 255, 0.35), transparent),
    radial-gradient(3px 3px at 25% 45%, rgba(255, 255, 255, 0.4), transparent),
    radial-gradient(2px 2px at 65% 90%, rgba(255, 255, 255, 0.3), transparent),
    linear-gradient(135deg, #1e293b 0%, #334155 50%, #475569 100%);
  background-size: 200% 200%;
  background-position: 50% 50%;
  transition: all var(--transition-duration) var(--transition-timing);
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--card-border-radius);
  overflow: hidden;
  position: relative;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.3), 
              0 2px 4px -1px rgba(0, 0, 0, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.05);
  will-change: transform; // Performance optimization for animations

  // Add a moon icon for OFF state
  &.light-off::before {
    content: '';
    position: absolute;
    top: 20px;
    left: 20px;
    width: 40px;
    height: 40px;
    background: radial-gradient(circle at 30% 30%, rgba(226, 232, 240, 0.2) 0%, transparent 70%);
    border-radius: 50%;
    opacity: 0.65;
    pointer-events: none;
    z-index: 0;
  }

  &.light-on {
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    border-color: rgba(251, 191, 36, 0.3);
    box-shadow: 0 4px 6px -1px rgba(251, 191, 36, 0.3), 
                0 2px 4px -1px rgba(251, 191, 36, 0.2),
                0 0 20px rgba(251, 191, 36, 0.2);
    
    &::before {
      display: none;
    }
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 20px -5px rgba(0, 0, 0, 0.2), 
                0 6px 10px -5px rgba(0, 0, 0, 0.1);

    &.light-on {
      box-shadow: 0 12px 20px -5px rgba(251, 191, 36, 0.4), 
                  0 6px 10px -5px rgba(251, 191, 36, 0.3),
                  0 0 30px rgba(251, 191, 36, 0.3);
    }

    .light-avatar {
      transform: scale(1.05);
    }

    .action-btn {
      background: rgba(255, 255, 255, 0.1);
    }
  }

  // Reduce motion for users who prefer it (accessibility)
  @media (prefers-reduced-motion: reduce) {
    transition: none;
    
    * {
      animation-duration: 0.01ms !important;
      animation-iteration-count: 1 !important;
      transition-duration: 0.01ms !important;
    }
  }

  .status-indicator {
    position: absolute;
    top: 12px;
    right: 12px;
    padding: 4px 12px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    z-index: 1;
    border: 1px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    backdrop-filter: blur(10px);
    transition: all var(--transition-duration) ease;

    &.indicator-on {
      background: rgba(251, 191, 36, 0.3);
      border-color: rgba(251, 191, 36, 0.5);
    }

    &.indicator-off {
      background: rgba(100, 116, 139, 0.3);
      border-color: rgba(148, 163, 184, 0.3);
    }
  }

  .card-content {
    padding: 48px var(--card-padding) var(--card-padding);
    min-height: 120px;
  }

  .light-avatar {
    transition: all var(--transition-duration) var(--transition-timing);
    border: 3px solid rgba(255, 255, 255, 0.2);
    position: relative;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
    will-change: transform; // Performance optimization

    &.avatar-on {
      background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
      border-color: rgba(251, 191, 36, 0.5);
      box-shadow: 0 4px 8px rgba(251, 191, 36, 0.4),
                  0 0 20px rgba(251, 191, 36, 0.3);
    }

    &.avatar-off {
      background: linear-gradient(135deg, #475569 0%, #334155 100%);
      border-color: rgba(148, 163, 184, 0.3);
    }

    .light-glow {
      position: absolute;
      inset: -10px;
      background: radial-gradient(circle, rgba(251, 191, 36, 0.4) 0%, transparent 70%);
      animation: pulse 2s ease-in-out infinite;
      pointer-events: none;
    }
  }

  .light-name {
    transition: color var(--transition-duration) ease;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    letter-spacing: 0.3px;
  }

  .light-description {
    display: flex;
    align-items: center;
    margin-top: 4px;
    opacity: 0.9;
    font-size: 0.875rem;
  }

  .offline-badge {
    animation: fadeIn var(--transition-duration) ease-out;
  }

  .timer-chip {
    animation: fadeInUp var(--transition-duration) ease-out;
    font-size: 0.75rem;
    font-weight: 600;
    border: 2px solid rgba(255, 255, 255, 0.5);
    
    .timer-text {
      font-weight: 600;
      letter-spacing: 0.3px;
    }
  }

  .action-btn {
    transition: all 0.2s ease;
    border-radius: 50%;

    &:hover {
      background: rgba(255, 255, 255, 0.15);
    }
  }

  .separator-glow {
    background: linear-gradient(90deg, 
      transparent 0%, 
      rgba(251, 191, 36, 0.5) 50%, 
      transparent 100%);
    height: 2px;
    box-shadow: 0 0 10px rgba(251, 191, 36, 0.5);
  }

  .toggle-section {
    padding: 6px var(--card-padding) 10px;
    background: rgba(0, 0, 0, 0.1);
    margin-bottom: 6px;
  }

  .toggle-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
  }

  .toggle-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    width: 100%;
    max-width: 340px;
    position: relative;
  }

  .toggle-label-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    padding: 5px 8px;
    border-radius: 8px;
    transition: all var(--transition-duration) var(--transition-timing);
    opacity: 0.5;
    background: rgba(255, 255, 255, 0.05);
    border: 2px solid transparent;
    min-width: 56px;
    cursor: pointer;

    .label-text {
      font-size: 0.6rem;
      font-weight: 600;
      letter-spacing: 0.3px;
      text-transform: uppercase;
    }

    &:hover {
      background: rgba(255, 255, 255, 0.1);
    }

    &.active {
      opacity: 1;
      border-color: rgba(255, 255, 255, 0.3);
      transform: scale(1.05);
      background: rgba(255, 255, 255, 0.15);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
    }

    &.toggle-label-on-wrapper.active {
      color: #fbbf24;
      background: rgba(251, 191, 36, 0.25);
      border-color: rgba(251, 191, 36, 0.5);
      box-shadow: 0 4px 12px rgba(251, 191, 36, 0.3);
    }

    &.toggle-label-off-wrapper.active {
      color: #cbd5e1;
      background: rgba(203, 213, 225, 0.15);
      border-color: rgba(203, 213, 225, 0.3);
    }
  }

  .offline-message {
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fca5a5;
    font-size: 0.875rem;
    animation: fadeIn var(--transition-duration) ease-out;
    padding: 8px var(--card-padding);
    background: rgba(220, 38, 38, 0.15);
    border-radius: 8px;
    border: 1px solid rgba(220, 38, 38, 0.3);
  }

  .accent-bar {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, 
      transparent 0%, 
      rgba(255, 255, 255, 0.3) 50%, 
      transparent 100%);
    transition: all var(--transition-duration) ease;

    &.accent-active {
      background: linear-gradient(90deg, 
        transparent 0%, 
        rgba(251, 191, 36, 0.8) 50%, 
        transparent 100%);
      animation: shimmer 2s ease-in-out infinite;
      box-shadow: 0 0 10px rgba(251, 191, 36, 0.5);
    }
  }

  // Animations - optimized with will-change
  @keyframes pulse {
    0%, 100% {
      opacity: 1;
    }
    50% {
      opacity: 0.6;
    }
  }

  @keyframes fadeIn {
    from {
      opacity: 0;
    }
    to {
      opacity: 1;
    }
  }

  @keyframes fadeInUp {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  @keyframes shimmer {
    0%, 100% {
      opacity: 0.5;
    }
    50% {
      opacity: 1;
    }
  }
}

// Override toggle styles for better integration
:deep(.light-toggle) {
  --toggle-width: 3.5rem;
  --toggle-height: 2rem;
  --toggle-border: 2px;
  --toggle-ring-width: 0;
  --toggle-handle-enabled: #ffffff;
  --toggle-handle-disabled: #9ca3af;
  
  .toggle-container {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.2);
  }

  .toggle-handle {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }

  &.toggle-on {
    .toggle-container {
      background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
      box-shadow: 0 0 20px rgba(251, 191, 36, 0.6),
                  inset 0 2px 4px rgba(0, 0, 0, 0.2);
    }

    .toggle-handle {
      transform: translateX(1.5rem);
    }
  }

  &:not(.toggle-on) {
    .toggle-container {
      background: linear-gradient(135deg, #64748b 0%, #475569 100%);
    }
  }

  // Make it more touch-friendly on mobile
  @media (max-width: 768px) {
    --toggle-width: 4.5rem;
    --toggle-height: 2.5rem;
    
    &.toggle-on {
      .toggle-handle {
        transform: translateX(2rem);
      }
    }
  }
  
  // Reduce motion for accessibility
  @media (prefers-reduced-motion: reduce) {
    .toggle-container,
    .toggle-handle {
      transition: none;
    }
  }
}
</style>

<style src="@vueform/toggle/themes/default.css"></style>
