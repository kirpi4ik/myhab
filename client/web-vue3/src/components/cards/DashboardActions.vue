<template>
  <div class="dashboard-layout">
    <!-- Quick Access Cards Section -->
    <div class="row q-col-gutter-md q-mb-md">
      <template v-if="hasAccess">
        <div 
          v-for="card in quickAccessCards" 
          :key="card.title || card.component" 
          class="col-lg-4 col-md-4 col-sm-12 col-xs-12"
        >
          <!-- Standard action card -->
          <q-card v-if="card.actions" class="dashboard-card small-card" :class="card.cardClass">
            <div class="card-header-wrapper">
              <q-card-section class="card-header">
                <div class="header-content">
                  <div class="header-icon-wrapper">
                    <q-icon :name="card.icon" size="32px" class="header-icon"/>
                    <div class="icon-glow"></div>
                  </div>
                  <div class="header-title">
                    {{ card.title }}
                  </div>
                </div>
              </q-card-section>
            </div>
            
            <q-card-actions class="card-actions">
              <template v-for="(action, index) in card.actions" :key="action.label">
                <q-btn 
                  flat
                  class="action-btn" 
                  no-caps 
                  :to="action.route"
                >
                  <div class="action-btn-content">
                    <q-icon v-if="action.icon" :name="action.icon" size="20px" class="q-mr-xs"/>
                    <span class="action-label">{{ action.label }}</span>
                  </div>
                </q-btn>
                <div 
                  v-if="index < card.actions.length - 1" 
                  class="action-divider"
                >
                  <q-separator vertical class="action-separator"/>
                </div>
              </template>
            </q-card-actions>
            
            <!-- Bottom accent bar -->
            <div class="accent-bar"></div>
          </q-card>
          
          <!-- Component card -->
          <component 
            v-else-if="card.component" 
            :is="card.component" 
            v-bind="card.props"
            class="small-card"
          />
        </div>
      </template>
    </div>

    <!-- Device Monitoring Section -->
    <div class="row q-col-gutter-md">
      <!-- Weather Station -->
      <div class="col-lg-4 col-md-6 col-sm-12 col-xs-12">
        <meteo-station-card :device-id="meteoStationDeviceId" :location-name="'Halchiu, Romania'"/>
      </div>
      <!-- Solar Power Plant -->
      <div class="col-lg-4 col-md-6 col-sm-12 col-xs-12">
        <solar-plant-widget :device-id="solarPlantDeviceId" :meter-device-id="solarMeterDeviceId"/>
      </div>
      <!-- Heat Pump -->
      <div class="col-lg-4 col-md-6 col-sm-12 col-xs-12">
        <nibe-heat-pump-widget :device-id="heatPumpDeviceId"/>
      </div>
    </div>

    <q-resize-observer @resize="onResize"/>
  </div>
</template>

<script>
import {defineComponent, computed} from 'vue';

import {authzService} from '@/_services';

import ElectricMeter from "components/ElectricMeter";
import HeatPump from "components/HeatPump";
import MeteoStationCard from "components/MeteoStationCard.vue";
import SolarPlantWidget from "components/SolarPlantWidget.vue";
import NibeHeatPumpWidget from "components/NibeHeatPumpWidget.vue";
import PeripheralLock from 'components/PeripheralLock.vue';
import SprinklersDashComponent from "components/SprinklersDashComponent";
import WaterPump from "components/WaterPump";

export default defineComponent({
  name: 'DashboardActions',
  components: {
    WaterPump,
    HeatPump,
    ElectricMeter,
    MeteoStationCard,
    SolarPlantWidget,
    NibeHeatPumpWidget,
    PeripheralLock,
    SprinklersDashComponent
  },
  setup() {
    // Zone IDs from environment
    const zoneIntId = Number(process.env.VUE_APP_CONF_ZONE_INT_ID);
    const zoneExtId = Number(process.env.VUE_APP_CONF_ZONE_EXT_ID);
    const zoneEtajId = Number(process.env.VUE_APP_CONF_ZONE_ETAJ_ID);
    const zoneParterId = Number(process.env.VUE_APP_CONF_ZONE_PARTER_ID);
    
    // Device IDs from environment
    const heatPumpDeviceId = Number(process.env.HEAT_PUMP_DEVICE_ID);
    const eMeterDeviceId = Number(process.env.ELECTRIC_METER_01_DEVICE_ID);
    const meteoStationDeviceId = 2000; // Virtual Meteo Station device
    const solarPlantDeviceId = 1000; // Huawei Solar Inverter device
    const solarMeterDeviceId = 1001; // Huawei Solar Meter device

    /**
     * Check if current user has any of the specified roles
     */
    const hasRole = (roles) => {
      if (!authzService.currentUserValue?.permissions) {
        return false;
      }
      return authzService.currentUserValue.permissions.some(userRole => 
        roles.includes(userRole)
      );
    };

    /**
     * Check if user has access to quick action cards
     */
    const hasAccess = computed(() => hasRole(['ROLE_ADMIN', 'ROLE_USER']));

    /**
     * Quick access cards configuration
     */
    const quickAccessCards = computed(() => [
      {
        title: 'Iluminat',
        icon: 'fas fa-lightbulb',
        cardClass: 'card-light',
        actions: [
          {
            label: 'Interior',
            icon: 'mdi-home-outline',
            route: `/zones/${zoneIntId}?category=LIGHT`
          },
          {
            label: 'Exterior',
            icon: 'mdi-home-city-outline',
            route: `/zones/${zoneExtId}?category=LIGHT`
          }
        ]
      },
      {
        title: 'Climatizare',
        icon: 'fas fa-fire',
        cardClass: 'card-heat',
        actions: [
          {
            label: 'Parter',
            icon: 'mdi-stairs-down',
            route: `/zones/${zoneParterId}?category=HEAT`
          },
          {
            label: 'Etaj',
            icon: 'mdi-stairs-up',
            route: `/zones/${zoneEtajId}?category=HEAT`
          }
        ]
      },
      {
        title: 'Switches',
        icon: 'mdi-electric-switch',
        cardClass: 'card-switch',
        actions: [
          {
            label: 'Interior',
            icon: 'mdi-home-outline',
            route: `/zones/${zoneIntId}?category=SWITCH`
          },
          {
            label: 'Exterior',
            icon: 'mdi-home-city-outline',
            route: `/zones/${zoneExtId}?category=SWITCH`
          }
        ]
      },
      {
        title: 'Temperatura',
        icon: 'fas fa-temperature-high',
        cardClass: 'card-temp',
        actions: [
          {
            label: 'Interior',
            icon: 'mdi-home-thermometer-outline',
            route: `/zones/${zoneIntId}?category=TEMP`
          },
          {
            label: 'Exterior',
            icon: 'mdi-thermometer',
            route: `/zones/${zoneExtId}?category=TEMP`
          }
        ]
      },
      // Peripheral control components
      {
        component: 'peripheral-lock'
      },
      {
        component: 'water-pump',
        props: { peripheral: { state: true } }
      },
      {
        component: 'sprinklers-dash-component',
        props: { peripheral: { state: true } }
      }
    ]);

    /**
     * Handle resize events
     */
    const onResize = () => {
      // Placeholder for future resize handling logic
    };

    return {
      hasAccess,
      quickAccessCards,
      heatPumpDeviceId,
      eMeterDeviceId,
      meteoStationDeviceId,
      solarPlantDeviceId,
      solarMeterDeviceId,
      onResize
    };
  }
});

</script>

<style scoped lang="scss">
.dashboard-layout {
  width: 100%;
}

.dashboard-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 
              0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.05);
  will-change: transform;

  &:hover {
    transform: translateY(-4px) scale(1.01);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12), 
                0 6px 12px rgba(0, 0, 0, 0.08);

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
}

.card-header-wrapper {
  position: relative;
  overflow: hidden;
}

.card-header {
  padding: 12px 16px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(248, 250, 252, 0.95) 100%);
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
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
}

.header-icon {
  position: relative;
  z-index: 2;
  font-size: 24px !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.icon-glow {
  position: absolute;
  inset: -6px;
  border-radius: 10px;
  opacity: 0;
  transition: all 0.3s ease;
  pointer-events: none;
  z-index: 1;
}

.header-title {
  flex: 1;
  font-size: 1.05rem;
  font-weight: 600;
  letter-spacing: 0.3px;
  color: #1e293b;
  line-height: 1.3;
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
    background: rgba(0, 0, 0, 0.05);
    border-color: rgba(0, 0, 0, 0.08);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.08);

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
    background: rgba(0, 0, 0, 0.08);
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
    rgba(0, 0, 0, 0.15) 20%,
    rgba(0, 0, 0, 0.15) 80%,
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
  background: linear-gradient(90deg, transparent 0%, rgba(0, 0, 0, 0.1) 50%, transparent 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

// Theme-specific colors
.card-light {
  .card-header {
    background: linear-gradient(135deg, #fcd34d 0%, #fbbf24 100%);
  }

  .header-icon-wrapper {
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    box-shadow: 0 4px 12px rgba(217, 119, 6, 0.4);
  }

  .header-icon {
    color: #ffffff;
  }

  .icon-glow {
    background: radial-gradient(circle, rgba(217, 119, 6, 0.5) 0%, transparent 70%);
  }

  .header-title {
    color: #78350f;
  }

  .accent-bar {
    background: linear-gradient(90deg, transparent 0%, #d97706 50%, transparent 100%);
  }

  &:hover .accent-bar {
    box-shadow: 0 0 12px rgba(217, 119, 6, 0.6);
  }
}

.card-switch {
  .card-header {
    background: linear-gradient(135deg, #86efac 0%, #4ade80 100%);
  }

  .header-icon-wrapper {
    background: linear-gradient(135deg, #059669 0%, #047857 100%);
    box-shadow: 0 4px 12px rgba(4, 120, 87, 0.4);
  }

  .header-icon {
    color: #ffffff;
  }

  .icon-glow {
    background: radial-gradient(circle, rgba(4, 120, 87, 0.5) 0%, transparent 70%);
  }

  .header-title {
    color: #064e3b;
  }

  .accent-bar {
    background: linear-gradient(90deg, transparent 0%, #047857 50%, transparent 100%);
  }

  &:hover .accent-bar {
    box-shadow: 0 0 12px rgba(4, 120, 87, 0.6);
  }
}

.card-heat {
  .card-header {
    background: linear-gradient(135deg, #fdba74 0%, #fb923c 100%);
  }

  .header-icon-wrapper {
    background: linear-gradient(135deg, #ea580c 0%, #dc2626 100%);
    box-shadow: 0 4px 12px rgba(220, 38, 38, 0.4);
  }

  .header-icon {
    color: #ffffff;
  }

  .icon-glow {
    background: radial-gradient(circle, rgba(220, 38, 38, 0.5) 0%, transparent 70%);
  }

  .header-title {
    color: #7c2d12;
  }

  .accent-bar {
    background: linear-gradient(90deg, transparent 0%, #dc2626 50%, transparent 100%);
  }

  &:hover .accent-bar {
    box-shadow: 0 0 12px rgba(220, 38, 38, 0.6);
  }
}

.card-temp {
  .card-header {
    background: linear-gradient(135deg, #93c5fd 0%, #60a5fa 100%);
  }

  .header-icon-wrapper {
    background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
    box-shadow: 0 4px 12px rgba(29, 78, 216, 0.4);
  }

  .header-icon {
    color: #ffffff;
  }

  .icon-glow {
    background: radial-gradient(circle, rgba(29, 78, 216, 0.5) 0%, transparent 70%);
  }

  .header-title {
    color: #1e3a8a;
  }

  .accent-bar {
    background: linear-gradient(90deg, transparent 0%, #1d4ed8 50%, transparent 100%);
  }

  &:hover .accent-bar {
    box-shadow: 0 0 12px rgba(29, 78, 216, 0.6);
  }
}

.small-card {
  height: 100%;
  min-height: 140px;
}

/* Mobile optimization - stack cards vertically */
@media (max-width: 599px) {
  .dashboard-layout > .row:first-child {
    margin-bottom: 16px;
  }
  
  .small-card {
    margin-bottom: 8px;
  }

  .action-btn {
    min-height: 56px;
  }

  .action-label {
    font-size: 1rem;
  }

  .header-title {
    font-size: 1rem;
  }

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
</style>
