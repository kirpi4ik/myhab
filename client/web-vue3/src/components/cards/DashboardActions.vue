<template>
  <div class="row q-col-gutter-md">
    <!-- Quick Access Cards -->
    <template v-if="hasAccess">
      <div 
        v-for="card in quickAccessCards" 
        :key="card.title" 
        class="col-lg-4 col-md-4 col-sm-12 col-xs-12"
      >
        <q-card class="dashboard-card">
          <q-card-section :class="`${card.bgColor} text-white text-h6`">
            <div class="row items-center">
              <div class="col">{{ card.title }}</div>
              <div class="col-auto">
                <q-icon :name="card.icon" size="40px"/>
              </div>
            </div>
          </q-card-section>
          <q-card-actions align="around" class="q-pa-md">
            <template v-for="(action, index) in card.actions" :key="action.label">
              <q-btn 
                flat 
                class="text-h6 text-grey-14" 
                no-caps 
                :to="action.route"
              >
                {{ action.label }}
              </q-btn>
              <q-separator 
                v-if="index < card.actions.length - 1" 
                vertical 
                class="q-mx-md"
              />
            </template>
          </q-card-actions>
        </q-card>
      </div>

      <!-- Peripheral Controls -->
      <div class="col-lg-4 col-md-4 col-sm-12 col-xs-12">
        <peripheral-lock class="q-mb-md"/>
        <water-pump :peripheral="{state: true}" class="q-mb-md"/>
        <sprinklers-dash-component :peripheral="{state: true}"/>
      </div>
    </template>

    <!-- Device Monitoring -->
    <div class="col-lg-4 col-md-4 col-sm-12 col-xs-12">
      <electric-meter :device-id="eMeterDeviceId"/>
    </div>
    <div class="col-lg-4 col-md-4 col-sm-12 col-xs-12">
      <heat-pump :deviceId="heatPumpDeviceId"/>
    </div>

    <q-resize-observer @resize="onResize"/>
  </div>
</template>

<script>
import {defineComponent, computed, ref} from 'vue';

import {authzService} from '@/_services';

import ElectricMeter from "components/ElectricMeter";
import HeatPump from "components/HeatPump";
import PeripheralLock from 'components/PeripheralLock.vue';
import SprinklersDashComponent from "components/SprinklersDashComponent";
import WaterPump from "components/WaterPump";

export default defineComponent({
  name: 'DashboardActions',
  components: {
    WaterPump,
    HeatPump,
    ElectricMeter,
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
        bgColor: 'bg-orange-5',
        actions: [
          {
            label: 'Interior',
            route: `/zones/${zoneIntId}?category=LIGHT`
          },
          {
            label: 'Exterior',
            route: `/zones/${zoneExtId}?category=LIGHT`
          }
        ]
      },
      {
        title: 'Climatizare',
        icon: 'fas fa-fire',
        bgColor: 'bg-deep-orange-8',
        actions: [
          {
            label: 'Parter',
            route: `/zones/${zoneParterId}?category=HEAT`
          },
          {
            label: 'Etaj',
            route: `/zones/${zoneEtajId}?category=HEAT`
          }
        ]
      },
      {
        title: 'Temperatura',
        icon: 'fas fa-temperature-high',
        bgColor: 'bg-blue-5',
        actions: [
          {
            label: 'Interior',
            route: `/zones/${zoneIntId}?category=TEMP`
          },
          {
            label: 'Exterior',
            route: `/zones/${zoneExtId}?category=TEMP`
          }
        ]
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
      onResize
    };
  }
});

</script>

<style scoped>
.dashboard-card {
  background-color: white;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.dashboard-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
</style>
