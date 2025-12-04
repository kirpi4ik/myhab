<template>
  <q-card 
    class="water-pump-card text-white"
    :class="{ 'pump-on': isPumpOn, 'pump-off': !isPumpOn }"
  >
    <!-- Status Indicator Badge -->
    <div class="status-indicator" :class="{ 'indicator-on': isPumpOn, 'indicator-off': !isPumpOn }">
      <q-icon :name="isPumpOn ? 'mdi-pump' : 'mdi-pump-off'" size="16px" class="q-mr-xs"/>
      <span class="text-caption text-weight-medium">{{ isPumpOn ? 'PORNIT' : 'OPRIT' }}</span>
    </div>

    <q-item class="card-content">
      <!-- Pump Icon Avatar -->
      <q-item-section avatar>
        <q-avatar
          size="60px"
          class="pump-avatar"
          :class="isPumpOn ? 'avatar-on' : 'avatar-off'"
        >
          <q-icon
            name="mdi-water-pump"
            :color="isPumpOn ? 'light-blue-1' : 'blue-grey-1'"
            size="40px"
          />
          <div v-if="isPumpOn" class="pump-glow"></div>
        </q-avatar>
      </q-item-section>

      <!-- Pump Info -->
      <q-item-section>
        <q-item-label class="text-weight-medium text-h5 pump-name">
          Pompă Apă
        </q-item-label>

        <!-- Timer Info -->
        <q-item-label v-if="timeoutConfig || showExpiration" class="q-mt-xs">
          <q-chip
            size="sm"
            :color="isPumpOn ? 'cyan-8' : 'blue-grey-8'"
            text-color="white"
            icon="mdi-timer-outline"
            class="timer-chip"
          >
            <span v-if="timeoutConfig" class="timer-text">
              {{ formatDuration(Number(timeoutConfig.value) * 1000) }}
            </span>
            <span v-if="timeoutConfig && showExpiration" class="timer-separator">
              •
            </span>
            <span v-if="showExpiration" class="timer-text">
              <q-icon name="mdi-timer-sand" size="14px" class="q-mr-xs"/>
              {{ formatTime(expirationTime) }}
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
                @click="$router.push({ path: '/admin/peripherals/' + asset.id + '/view' })"
              >
                <q-item-section avatar>
                  <q-icon name="mdi-information" color="info"/>
                </q-item-section>
                <q-item-section>
                  <q-item-label>Detalii</q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </q-btn-dropdown>
        </q-item-label>
        <q-item-label>
          <event-logger :peripheral="asset"/>
        </q-item-label>
      </q-item-section>
    </q-item>

    <q-separator :class="isPumpOn ? 'separator-glow' : ''" />

    <!-- Toggle Switch -->
    <q-card-section class="toggle-section">
      <div class="toggle-container">
        <div v-if="asset && asset.data && asset.data.state !== undefined" class="toggle-wrapper">
          <div class="toggle-label-wrapper toggle-label-off-wrapper" :class="{ 'active': !isPumpOn }">
            <q-icon name="mdi-water-off" size="16px"/>
            <span class="label-text">OPRIT</span>
          </div>
          
          <toggle
            v-model="asset.data.state"
            :id="String(asset.id)"
            @change="handleToggle"
            class="pump-toggle"
          />
          
          <div class="toggle-label-wrapper toggle-label-on-wrapper" :class="{ 'active': isPumpOn }">
            <q-icon name="mdi-water" size="16px"/>
            <span class="label-text">PORNIT</span>
          </div>
        </div>
        <div v-else class="loading-state">
          <q-spinner-dots color="white" size="md" />
        </div>
      </div>
    </q-card-section>

    <!-- Bottom Accent Bar -->
    <div class="accent-bar" :class="{ 'accent-active': isPumpOn }"></div>
  </q-card>
</template>
<script>
import {computed, defineComponent, onMounted, ref} from 'vue';

import {useApolloClient, useMutation} from "@vue/apollo-composable";
import {useWebSocketListener} from "@/composables";

import {
  CACHE_GET_VALUE,
  CONFIGURATION_REMOVE_CONFIG_BY_KEY,
  CONFIGURATION_SET_VALUE,
  PERIPHERAL_GET_BY_ID
} from "@/graphql/queries";
import {peripheralService} from '@/_services/controls';

import _ from "lodash";
import {format} from "date-fns";
import EventLogger from "components/EventLogger";
import humanizeDuration from 'humanize-duration';
import Toggle from "@vueform/toggle";
import TimeoutSelector from "components/TimeoutSelector.vue";

export default defineComponent({
  name: 'WaterPump',
  components: {
    Toggle,
    EventLogger,
    TimeoutSelector
  },
  setup(props, {emit}) {
    const asset = ref({});
    const expirationTime = ref(null);
    const {client} = useApolloClient();
    
    const {mutate: setConfigValue} = useMutation(CONFIGURATION_SET_VALUE, {
      update: () => {
        init();
      },
    });

    const {mutate: deleteConfigValue} = useMutation(CONFIGURATION_REMOVE_CONFIG_BY_KEY, {
      update: () => {
        init();
      },
    });
    
    /**
     * Check if pump is currently on
     */
    const isPumpOn = computed(() => {
      return asset.value?.data?.state === true;
    });

    /**
     * Get timeout configuration
     */
    const timeoutConfig = computed(() => {
      return asset.value?.configurations?.find(cfg => cfg.key === 'key.on.timeout');
    });

    /**
     * Check if expiration should be shown
     */
    const showExpiration = computed(() => {
      return isPumpOn.value && !!expirationTime.value;
    });
    
    const handleSetTimeout = (timeoutValue) => {
      setConfigValue({
        key: 'key.on.timeout',
        value: timeoutValue,
        entityId: asset.value.id,
        entityType: 'PERIPHERAL'
      });
    };

    const handleDeleteTimeout = () => {
      deleteConfigValue({
        entityId: asset.value.id,
        entityType: 'PERIPHERAL',
        key: 'key.on.timeout'
      });
    };

    const init = async () => {
      try {
        const response = await client.query({
          query: PERIPHERAL_GET_BY_ID,
          variables: {id: process.env.WATER_PUMP_ID},
          fetchPolicy: 'network-only',
        });
        
        let assetData = _.cloneDeep(response.data.devicePeripheral);
        
        // Initialize with peripheralService to ensure proper structure
        assetData = peripheralService.peripheralInit(null, assetData);
        
        // Load expiration from cache
        const portId = assetData?.data?.connectedTo?.[0]?.id;
        if (portId) {
          try {
            const cacheData = await client.query({
              query: CACHE_GET_VALUE,
              variables: { cacheName: 'expiring', cacheKey: portId },
              fetchPolicy: 'network-only',
            });

            const cachedValue = cacheData.data?.cache?.cachedValue;
            if (cachedValue && cachedValue !== 'null') {
              assetData.expiration = cachedValue;
              expirationTime.value = cachedValue;
            } else {
              assetData.expiration = null;
              expirationTime.value = null;
            }
          } catch (error) {
            console.error('Failed to load cache expiration:', error);
          }
        }
        
        asset.value = assetData;
      } catch (error) {
        console.error('Failed to load peripheral:', error);
      }
    };

    const config = key => {
      if (asset.value != null) {
        return _.find(asset.value.configurations, function (cfg) {
          return cfg.key == key;
        });
      }
    };
    
    // Listen for port value updates
    useWebSocketListener('evt_port_value_persisted', (payload) => {
      if (asset.value?.data?.connectedTo?.[0]?.id == payload.p2) {
        asset.value['value'] = payload.p4;
        asset.value['state'] = payload.p4 === 'ON';
        asset.value['data']['state'] = payload.p4 === 'ON';
        
        // Refresh details to get latest cache
        setTimeout(() => {
          init();
        }, 100);
      }
    });

    // Listen for configuration changes
    useWebSocketListener('evt_cfg_value_changed', (payload) => {
      if (asset.value.id === Number(payload.p3) && payload.p2 === 'PERIPHERAL') {
        init();
      }
    });
    
    const handleToggle = () => {
      peripheralService.toggle(asset.value, 'evt_light');
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

    onMounted(() => {
      init();
    });
    
    return {
      asset,
      isPumpOn,
      timeoutConfig,
      showExpiration,
      expirationTime,
      handleSetTimeout,
      handleDeleteTimeout,
      config,
      handleToggle,
      formatDuration,
      formatTime,
    };
  }
});

</script>
<style scoped lang="scss">
// CSS Custom Properties for easier theming and maintenance
.water-pump-card {
  // Define custom properties for reusability
  --card-border-radius: 12px;
  --card-padding: 16px;
  --transition-timing: cubic-bezier(0.4, 0, 0.2, 1);
  --transition-duration: 0.3s;
  
  // Off state - darker blue/grey
  background: linear-gradient(135deg, #475569 0%, #64748b 50%, #94a3b8 100%);
  transition: all var(--transition-duration) var(--transition-timing);
  border: 2px solid rgba(255, 255, 255, 0.1);
  border-radius: var(--card-border-radius);
  overflow: hidden;
  position: relative;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.3), 
              0 2px 4px -1px rgba(0, 0, 0, 0.2),
              inset 0 1px 0 rgba(255, 255, 255, 0.1);
  will-change: transform;

  // Decorative element for OFF state
  &.pump-off::before {
    content: '';
    position: absolute;
    top: 20px;
    left: 20px;
    width: 35px;
    height: 35px;
    background: radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.15) 0%, transparent 70%);
    clip-path: circle(50% at 50% 50%);
    opacity: 0.3;
    pointer-events: none;
    z-index: 0;
  }

  // On state - vibrant blue/cyan
  &.pump-on {
    background: linear-gradient(135deg, #0891b2 0%, #06b6d4 50%, #22d3ee 100%);
    border-color: rgba(6, 182, 212, 0.3);
    box-shadow: 0 4px 6px -1px rgba(6, 182, 212, 0.3), 
                0 2px 4px -1px rgba(6, 182, 212, 0.2),
                0 0 20px rgba(6, 182, 212, 0.2);
    
    &::before {
      width: 40px;
      height: 40px;
      background: radial-gradient(circle at 50% 70%, rgba(34, 211, 238, 0.4) 0%, transparent 60%);
      opacity: 0.6;
      animation: waterFlow 2s ease-in-out infinite;
    }
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 20px -5px rgba(0, 0, 0, 0.2), 
                0 6px 10px -5px rgba(0, 0, 0, 0.1);

    &.pump-on {
      box-shadow: 0 12px 20px -5px rgba(6, 182, 212, 0.4), 
                  0 6px 10px -5px rgba(6, 182, 212, 0.3),
                  0 0 30px rgba(6, 182, 212, 0.3);
    }

    .pump-avatar {
      transform: scale(1.05);
    }

    .action-btn {
      background: rgba(255, 255, 255, 0.1);
    }
  }

  // Reduce motion for accessibility
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
      background: rgba(6, 182, 212, 0.3);
      border-color: rgba(6, 182, 212, 0.5);
    }

    &.indicator-off {
      background: rgba(71, 85, 105, 0.3);
      border-color: rgba(148, 163, 184, 0.5);
    }
  }

  .card-content {
    padding: 48px var(--card-padding) var(--card-padding);
    min-height: 120px;
  }

  .pump-avatar {
    transition: all var(--transition-duration) var(--transition-timing);
    border: 3px solid rgba(255, 255, 255, 0.2);
    position: relative;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
    will-change: transform;

    &.avatar-on {
      background: linear-gradient(135deg, #06b6d4 0%, #0891b2 100%);
      border-color: rgba(6, 182, 212, 0.5);
      box-shadow: 0 4px 8px rgba(6, 182, 212, 0.4),
                  0 0 20px rgba(6, 182, 212, 0.3);
    }

    &.avatar-off {
      background: linear-gradient(135deg, #64748b 0%, #475569 100%);
      border-color: rgba(148, 163, 184, 0.3);
    }

    .pump-glow {
      position: absolute;
      inset: -10px;
      background: radial-gradient(circle, rgba(6, 182, 212, 0.4) 0%, transparent 70%);
      animation: pulse 2s ease-in-out infinite;
      pointer-events: none;
    }
  }

  .pump-name {
    transition: color var(--transition-duration) ease;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    letter-spacing: 0.3px;
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

    .timer-separator {
      margin: 0 8px;
      opacity: 0.6;
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
      rgba(6, 182, 212, 0.5) 50%, 
      transparent 100%);
    height: 2px;
    box-shadow: 0 0 10px rgba(6, 182, 212, 0.5);
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
    min-height: 52px;
    justify-content: center;
  }

  .loading-state {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 12px;
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
      color: #67e8f9;
      background: rgba(6, 182, 212, 0.25);
      border-color: rgba(6, 182, 212, 0.5);
      box-shadow: 0 4px 12px rgba(6, 182, 212, 0.3);
    }

    &.toggle-label-off-wrapper.active {
      color: #cbd5e1;
      background: rgba(71, 85, 105, 0.25);
      border-color: rgba(71, 85, 105, 0.5);
    }
  }

  .accent-bar {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, 
      transparent 0%, 
      rgba(148, 163, 184, 0.5) 50%, 
      transparent 100%);
    transition: all var(--transition-duration) ease;

    &.accent-active {
      background: linear-gradient(90deg, 
        transparent 0%, 
        rgba(6, 182, 212, 0.8) 50%, 
        transparent 100%);
      animation: shimmer 2s ease-in-out infinite;
      box-shadow: 0 0 10px rgba(6, 182, 212, 0.5);
    }
  }

  // Animations
  @keyframes pulse {
    0%, 100% {
      opacity: 1;
    }
    50% {
      opacity: 0.6;
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

  @keyframes waterFlow {
    0%, 100% {
      opacity: 0.6;
      transform: scale(1);
    }
    50% {
      opacity: 0.8;
      transform: scale(1.1);
    }
  }
}

// Override toggle styles for better integration
:deep(.pump-toggle) {
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
      background: linear-gradient(135deg, #06b6d4 0%, #0891b2 100%);
      box-shadow: 0 0 20px rgba(6, 182, 212, 0.6),
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
