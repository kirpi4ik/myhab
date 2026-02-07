<template>
  <q-card 
    class="zone-card text-white" 
    @click="navigateToZone"
  >
    <!-- Zone Indicator Badge -->
    <div class="zone-indicator">
      <q-icon name="mdi-map-marker" size="18px" class="q-mr-xs"/>
      <span class="text-caption text-weight-medium">{{ $t('zone_card.location') }}</span>
    </div>

    <q-item clickable v-ripple class="zone-content">
      <!-- Zone Icon Avatar -->
      <q-item-section avatar>
        <q-avatar
          size="70px"
          class="shadow-10 zone-avatar"
          :style="{ background: getZoneGradient() }"
        >
          <q-icon
            name="mdi-map-marker-radius"
            color="white"
            size="42px"
          />
        </q-avatar>
      </q-item-section>

      <!-- Zone Info -->
      <q-item-section>
        <q-item-label class="text-weight-bold text-h5 zone-name">
          {{ zone?.name || 'Unknown Zone' }}
        </q-item-label>
        <q-item-label v-if="zone?.description" caption class="text-blue-grey-2 zone-description">
          <q-icon name="mdi-information-outline" size="14px" class="q-mr-xs"/>
          {{ zone.description }}
        </q-item-label>

        <!-- Child Count Indicator -->
        <q-item-label v-if="childCount > 0" class="q-mt-xs">
          <q-chip
            size="sm"
            color="rgba(255, 255, 255, 0.2)"
            text-color="white"
            icon="mdi-view-grid"
            class="zone-chip"
          >
            {{ childCount }} {{ childCount === 1 ? $t('zone_card.item') : $t('zone_card.items') }}
          </q-chip>
        </q-item-label>
      </q-item-section>

      <!-- Right Side Section -->
      <q-item-section side>
        <!-- Heat Scheduler (for HEAT/TEMP categories) -->
        <q-item-label v-if="showHeatScheduler">
          <heat-scheduler :zone="zone" />
        </q-item-label>

        <!-- Temperature Display (for HEAT/TEMP categories) -->
        <q-item-label v-if="showTempDisplay">
          <temp-display :zone="zone" :name="zone.name" />
        </q-item-label>

        <!-- Navigate Icon -->
        <q-item-label v-if="!showHeatScheduler && !showTempDisplay" class="navigate-icon">
          <q-icon name="mdi-chevron-right" size="32px" class="text-white"/>
        </q-item-label>
      </q-item-section>
    </q-item>

    <!-- Bottom Accent Bar -->
    <div class="zone-accent-bar"></div>
  </q-card>
</template>
<script>
import {computed, defineComponent} from 'vue';
import {useRoute, useRouter} from 'vue-router';

import {authzService} from '@/_services';
import {Role} from '@/_helpers';
import HeatScheduler from '@/components/HeatScheduler';
import TempDisplay from '@/components/TempDisplay';

export default defineComponent({
  name: 'ZoneCard',
  components: {
    HeatScheduler,
    TempDisplay
  },
  props: {
    zone: {
      type: Object,
      required: true
    }
  },
  setup(props) {
    const router = useRouter();
    const route = useRoute();

    /**
     * Get child count (peripherals + child zones)
     */
    const childCount = computed(() => {
      const peripheralsCount = props.zone?.data?.peripherals?.length || 0;
      const childZonesCount = props.zone?.data?.childZones?.length || 0;
      return peripheralsCount + childZonesCount;
    });

    /**
     * Generate gradient based on zone properties
     */
    const getZoneGradient = () => {
      const category = route.query.category;
      const gradients = {
        'LIGHT': 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
        'HEAT': 'linear-gradient(135deg, #dc2626 0%, #991b1b 100%)',
        'SWITCH': 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
        'TEMP': 'linear-gradient(135deg, #06b6d4 0%, #0891b2 100%)',
        'default': 'linear-gradient(135deg, #6366f1 0%, #4f46e5 100%)'
      };
      return gradients[category] || gradients.default;
    };

    /**
     * Check if current category is HEAT or TEMP
     */
    const isHeatOrTempCategory = computed(() => {
      const category = route.query.category;
      return ['HEAT', 'TEMP'].includes(category);
    });

    /**
     * Check if user has permission to see heat scheduler
     */
    const hasSchedulerPermission = computed(() => {
      return authzService.hasAnyRole([Role.User, Role.Admin]);
    });

    /**
     * Show heat scheduler if user has permission and category is HEAT/TEMP
     */
    const showHeatScheduler = computed(() => {
      return hasSchedulerPermission.value && isHeatOrTempCategory.value;
    });

    /**
     * Show temperature display for HEAT/TEMP categories
     */
    const showTempDisplay = computed(() => {
      return isHeatOrTempCategory.value;
    });

    /**
     * Navigate to zone details page
     */
    const navigateToZone = () => {
      if (!props.zone?.id) {
        console.warn('Cannot navigate: zone ID is missing');
        return;
      }

      router.push({
        name: 'zoneById',
        params: { zoneId: String(props.zone.id) },
        query: { category: route.query.category }
      });
    };

    return {
      childCount,
      getZoneGradient,
      showHeatScheduler,
      showTempDisplay,
      navigateToZone
    };
  }
});

</script>

<style scoped lang="scss">
.zone-card {
  background: url('~assets/layer-1.png'), linear-gradient(135deg, #546e82 0%, #7c919f 100%);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  border: 2px solid rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 
              0 2px 4px -1px rgba(0, 0, 0, 0.06);

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, 
      rgba(255, 255, 255, 0.4) 0%, 
      rgba(255, 255, 255, 0.8) 50%, 
      rgba(255, 255, 255, 0.4) 100%);
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  &:hover {
    transform: translateY(-4px) scale(1.02);
    border-color: rgba(255, 255, 255, 0.3);
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.2), 
                0 10px 10px -5px rgba(0, 0, 0, 0.1),
                0 0 0 3px rgba(255, 255, 255, 0.1);

    &::before {
      opacity: 1;
    }

    .zone-avatar {
      transform: scale(1.1) rotate(5deg);
    }

    .navigate-icon {
      transform: translateX(4px);
    }

    .zone-name {
      color: #fef3c7;
    }
  }

  &:active {
    transform: translateY(-2px) scale(1.01);
  }

  .zone-indicator {
    position: absolute;
    top: 12px;
    left: 12px;
    background: rgba(255, 255, 255, 0.25);
    backdrop-filter: blur(10px);
    padding: 4px 12px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    z-index: 1;
    border: 1px solid rgba(255, 255, 255, 0.3);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }

  .zone-content {
    padding: 48px 16px 16px;
    min-height: 140px;
  }

  .zone-avatar {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    border: 3px solid rgba(255, 255, 255, 0.3);
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2),
                0 0 0 4px rgba(255, 255, 255, 0.1);
  }

  .zone-name {
    transition: color 0.3s ease;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    letter-spacing: 0.5px;
  }

  .zone-description {
    display: flex;
    align-items: center;
    margin-top: 4px;
    opacity: 0.9;
  }

  .zone-chip {
    animation: fadeInUp 0.4s ease-out;
  }

  .navigate-icon {
    transition: transform 0.3s ease;
    opacity: 0.8;
  }

  .zone-accent-bar {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 6px;
    background: linear-gradient(90deg, 
      transparent 0%, 
      rgba(255, 255, 255, 0.4) 50%, 
      transparent 100%);
    animation: shimmer 3s ease-in-out infinite;
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
      opacity: 0.3;
    }
    50% {
      opacity: 0.7;
    }
  }
}
</style>
