<template>
  <q-card class="sprinklers-dash-card" :class="{ 'system-active': isSystemActive }">
    <!-- Status Indicator Badge -->
    <div class="status-indicator">
      <q-icon name="mdi-sprinkler-variant" size="16px" class="q-mr-xs"/>
      <span class="text-caption text-weight-medium">Sistem Udare</span>
    </div>

    <div class="card-header-wrapper">
      <q-card-section class="card-header">
        <div class="header-content">
          <div class="header-icon-wrapper">
            <q-icon name="mdi-sprinkler-variant" size="32px" class="header-icon"/>
            <div class="icon-glow"></div>
          </div>
          <div class="header-info">
            <div class="header-title">Sistem Udare</div>
            <!-- Timer Info -->
            <div v-if="timeoutConfig || showExpiration" class="timer-info">
              <q-chip
                size="sm"
                color="cyan-8"
                text-color="white"
                icon="mdi-timer-outline"
                class="timer-chip"
              >
                <span v-if="timeoutConfig" class="timer-text">
                  {{ formatDuration(Number(timeoutConfig.value) * 1000) }}
                </span>
                <span v-if="timeoutConfig && showExpiration" class="timer-separator">•</span>
                <span v-if="showExpiration" class="timer-text">
                  <q-icon name="mdi-timer-sand" size="12px" class="q-mr-xs"/>
                  {{ formatTime(expirationTime) }}
                </span>
              </q-chip>
            </div>
          </div>
          <!-- Actions Menu -->
          <div class="header-actions">
            <q-btn-dropdown 
              size="sm" 
              flat 
              round 
              icon="mdi-tune-variant" 
              class="action-menu-btn"
            >
              <q-list>
                <!-- Timeout Selector -->
                <timeout-selector
                  :current-timeout="timeoutConfig ? Number(timeoutConfig.value) : null"
                  @set-timeout="handleSetTimeout"
                  @delete-timeout="handleDeleteTimeout"
                />
                
                <q-separator/>
                
                <!-- Event Logger -->
                <q-item>
                  <q-item-section>
                    <event-logger :peripheral="asset"/>
                  </q-item-section>
                </q-item>
                
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
          </div>
        </div>
      </q-card-section>
    </div>

    <q-card-actions class="card-actions">
      <!-- Gazon (Lawn) Action -->
      <q-btn 
        flat
        class="action-btn" 
        no-caps 
        :to="`/zones/${zoneLanId}?category=SPRINKLER`"
      >
        <div class="action-btn-content">
          <q-icon name="mdi-grass" size="20px" class="q-mr-xs"/>
          <span class="action-label">Gazon</span>
        </div>
      </q-btn>
      
      <div class="action-divider">
        <q-separator vertical class="action-separator"/>
      </div>
      
      <!-- Gradina (Garden) Action -->
      <q-btn 
        flat
        class="action-btn" 
        no-caps 
        :to="`/zones/${zoneGardenId}?category=SPRINKLER`"
      >
        <div class="action-btn-content">
          <q-icon name="mdi-flower" size="20px" class="q-mr-xs"/>
          <span class="action-label">Grădină</span>
        </div>
      </q-btn>
    </q-card-actions>

    <!-- Bottom Accent Bar -->
    <div class="accent-bar" :class="{ 'accent-active': isSystemActive }"></div>
  </q-card>
</template>
<script>
import {computed, defineComponent, onMounted, ref} from 'vue';

import {useMutation} from "@vue/apollo-composable";

import {
  CONFIGURATION_REMOVE_CONFIG_BY_KEY,
  CONFIGURATION_SET_VALUE,
} from "@/graphql/queries";

import _ from "lodash";
import {format} from "date-fns";
import EventLogger from "components/EventLogger";
import humanizeDuration from 'humanize-duration';
import TimeoutSelector from "components/TimeoutSelector.vue";

export default defineComponent({
  name: 'SprinklersDashComponent',
  components: {
    EventLogger,
    TimeoutSelector
  },
  setup(props, {emit}) {
    const zoneLanId = process.env.VUE_APP_CONF_ZONE_LAN_ID;
    const zoneGardenId = process.env.VUE_APP_CONF_ZONE_GARDEN_ID;

    const asset = ref({});
    const expirationTime = ref(null);
    
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
     * Check if sprinkler system is active
     */
    const isSystemActive = computed(() => {
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
      return isSystemActive.value && !!expirationTime.value;
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
      // This component doesn't have a specific peripheral ID yet
      // It's a system-level component that navigates to zones
      // Keeping the structure for future enhancement
    };

    const config = key => {
      if (asset.value != null) {
        return _.find(asset.value.configurations, function (cfg) {
          return cfg.key == key;
        });
      }
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
      } catch {
        return '--:--';
      }
    };
    
    onMounted(() => {
      init();
    });

    return {
      zoneLanId,
      zoneGardenId,
      asset,
      isSystemActive,
      timeoutConfig,
      showExpiration,
      expirationTime,
      handleSetTimeout,
      handleDeleteTimeout,
      config,
      formatDuration,
      formatTime,
    };
  }
});

</script>
<style scoped lang="scss">
// CSS Custom Properties for easier theming and maintenance
.sprinklers-dash-card {
  // Define custom properties for reusability
  --card-border-radius: 12px;
  --card-padding: 16px;
  --transition-timing: cubic-bezier(0.4, 0, 0.2, 1);
  --transition-duration: 0.3s;
  
  // Sprinkler theme - green/teal nature/garden colors
  background: linear-gradient(135deg, #14b8a6 0%, #0d9488 50%, #0f766e 100%);
  transition: all var(--transition-duration) var(--transition-timing);
  border-radius: var(--card-border-radius);
  overflow: hidden;
  position: relative;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 
              0 1px 3px rgba(0, 0, 0, 0.06);
  border: 2px solid rgba(255, 255, 255, 0.1);
  will-change: transform;

  &:hover {
    transform: translateY(-4px) scale(1.01);
    box-shadow: 0 12px 24px rgba(13, 148, 136, 0.3), 
                0 6px 12px rgba(13, 148, 136, 0.2);

    .header-icon {
      transform: scale(1.15) rotate(5deg);
    }

    .icon-glow {
      opacity: 0.8;
      transform: scale(1.3);
    }

    .accent-bar {
      opacity: 1;
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
    border: 1px solid rgba(255, 255, 255, 0.3);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    backdrop-filter: blur(10px);
    background: rgba(13, 148, 136, 0.3);
    border-color: rgba(20, 184, 166, 0.5);
    transition: all var(--transition-duration) ease;
    color: white;
  }
}

.card-header-wrapper {
  position: relative;
  overflow: hidden;
}

.card-header {
  padding: 12px 16px;
  background: linear-gradient(135deg, #5eead4 0%, #2dd4bf 100%);
  position: relative;
  z-index: 1;
  min-height: 52px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon-wrapper {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: linear-gradient(135deg, #0d9488 0%, #0f766e 100%);
  box-shadow: 0 4px 12px rgba(13, 148, 136, 0.4);
  flex-shrink: 0;
}

.header-icon {
  position: relative;
  z-index: 2;
  font-size: 24px !important;
  color: white;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.icon-glow {
  position: absolute;
  inset: -6px;
  border-radius: 10px;
  opacity: 0;
  background: radial-gradient(circle, rgba(13, 148, 136, 0.5) 0%, transparent 70%);
  transition: all 0.3s ease;
  pointer-events: none;
  z-index: 1;
}

.header-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-title {
  font-size: 1.05rem;
  font-weight: 600;
  letter-spacing: 0.3px;
  color: #134e4a;
  line-height: 1.3;
}

.timer-info {
  .timer-chip {
    font-size: 0.7rem;
    padding: 2px 8px;
    height: auto;
    
    .timer-text {
      font-weight: 600;
      letter-spacing: 0.2px;
      font-size: 0.7rem;
    }

    .timer-separator {
      margin: 0 4px;
      opacity: 0.6;
    }
  }
}

.header-actions {
  .action-menu-btn {
    color: white;
    transition: all 0.2s ease;
    border-radius: 50%;

    &:hover {
      background: rgba(255, 255, 255, 0.15);
    }
  }
}

.card-actions {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-around;
  gap: 0;
  background: #ffffff;
}

.action-btn {
  flex: 1;
  min-height: 52px;
  border-radius: 8px;
  transition: all 0.2s ease;
  color: #475569;
  font-weight: 500;
  position: relative;
  overflow: hidden;
  border: 2px solid transparent;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg, rgba(0, 0, 0, 0.02) 0%, rgba(0, 0, 0, 0.04) 100%);
    opacity: 0;
    transition: opacity 0.2s ease;
  }

  &:hover {
    transform: translateY(-2px);
    color: #0f172a;
    background: rgba(13, 148, 136, 0.08);
    border-color: rgba(13, 148, 136, 0.2);
    box-shadow: 0 4px 8px rgba(13, 148, 136, 0.15);

    &::before {
      opacity: 1;
    }
  }

  &:active {
    transform: translateY(0);
  }
}

.action-divider {
  display: flex;
  align-items: center;
  padding: 0 16px;
  position: relative;

  &::before,
  &::after {
    content: '';
    position: absolute;
    top: 50%;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: rgba(13, 148, 136, 0.2);
  }

  &::before {
    left: 4px;
    transform: translateY(-50%);
  }

  &::after {
    right: 4px;
    transform: translateY(-50%);
  }
}

.action-separator {
  height: 40px;
  width: 2px;
  background: linear-gradient(180deg, 
    transparent 0%, 
    rgba(13, 148, 136, 0.3) 20%,
    rgba(13, 148, 136, 0.3) 80%,
    transparent 100%);
}

.action-btn-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.action-label {
  font-size: 0.95rem;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.accent-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, transparent 0%, rgba(20, 184, 166, 0.6) 50%, transparent 100%);
  opacity: 0;
  transition: opacity 0.3s ease;

  &.accent-active {
    opacity: 1;
    animation: shimmer 2s ease-in-out infinite;
    box-shadow: 0 0 12px rgba(20, 184, 166, 0.5);
  }
}

// Animations
@keyframes shimmer {
  0%, 100% {
    opacity: 0.6;
  }
  50% {
    opacity: 1;
  }
}

// Mobile optimization
@media (max-width: 599px) {
  .sprinklers-dash-card {
    .card-header {
      min-height: 56px;
    }

    .header-icon-wrapper {
      width: 44px;
      height: 44px;
    }

    .header-icon {
      font-size: 26px !important;
    }

    .header-title {
      font-size: 1rem;
    }

    .action-btn {
      min-height: 56px;
    }

    .action-label {
      font-size: 1rem;
    }
  }
}
</style>
