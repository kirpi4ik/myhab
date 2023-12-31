<template>
  <div class="row q-col-gutter-xs">
    <div class="col-lg-4 col-md-4 col-sm-12 col-xs-12" v-if="hasRole(['ROLE_ADMIN','ROLE_USER'])">
      <q-card class="q-ma-md-xs" style="background-color: white">
        <q-card-section class="bg-orange-5 text-amber-1 text-h6">
          Iluminat
          <q-icon name="fas fa-lightbulb" class="float-right" size="40px"/>
        </q-card-section>
        <q-card-actions align="around">
          <q-btn flat class="text-h6 text-grey-14" no-caps :to="'/zones/' + zoneIntId + '?category=LIGHT'">Interior
          </q-btn>
          <q-separator vertical></q-separator>
          <q-btn flat class="text-h6 text-grey-14" no-caps :to="'/zones/' + zoneExtId + '?category=LIGHT'">Exterior
          </q-btn>
        </q-card-actions>
      </q-card>
    </div>
    <div class="col-lg-4 col-md-4 col-sm-12 col-xs-12" v-if="hasRole(['ROLE_ADMIN','ROLE_USER'])">
      <q-card class="q-ma-md-xs" style="background-color: white">
        <q-card-section class="bg-deep-orange-8 text-amber-1 text-h6">
          Climatizare
          <q-icon name="fas fa-fire" class="float-right" size="40px"/>
        </q-card-section>
        <q-card-actions align="around">
          <q-btn flat class="text-h6 text-grey-14" no-caps :to="'/zones/' + zoneParterId + '?category=HEAT'">Parter</q-btn>
          <q-separator vertical></q-separator>
            <q-btn flat class="text-h6 text-grey-14" no-caps :to="'/zones/' + zoneEtajId + '?category=HEAT'">Etaj</q-btn>
        </q-card-actions>
      </q-card>
    </div>
    <div class="col-lg-4 col-md-4 col-sm-12 col-xs-12" v-if="hasRole(['ROLE_ADMIN','ROLE_USER'])">
      <q-card class="q-ma-md-xs" style="background-color: white">
        <q-card-section class="bg-blue-5 text-amber-1 text-h6">
          Temperatura
          <q-icon name="fas fa-temperature-high" class="float-right" size="40px"/>
        </q-card-section>
        <q-card-actions align="around">
          <q-btn flat class="text-h6 text-grey-14" no-caps :to="'/zones/' + zoneIntId + '?category=TEMP'">Interior
          </q-btn>
          <q-separator vertical></q-separator>
          <q-btn flat class="text-h6 text-grey-14" no-caps :to="'/zones/' + zoneExtId + '?category=TEMP'">Exterior
          </q-btn>
        </q-card-actions>
      </q-card>
    </div>
    <div class="col-lg-4 col-md-4 col-sm-12 col-xs-12" v-if="hasRole(['ROLE_ADMIN','ROLE_USER'])">
      <peripheral-lock/>
      &nbsp;
      <water-pump :peripheral="{state: true}"/>
    </div>
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
import {defineComponent} from 'vue';
import PeripheralLock from 'components/PeripheralLock.vue';
import {authzService} from '@/_services';
import ElectricMeter from "components/ElectricMeter";
import HeatPump from "components/HeatPump";
import WaterPump from "components/WaterPump";


export default defineComponent({
  name: 'DashboardActions',
  components: {
    WaterPump,
    HeatPump,
    ElectricMeter,
    PeripheralLock,
  },
  setup() {
    const zoneIntId = parseInt(process.env.VUE_APP_CONF_ZONE_INT_ID);
    const zoneExtId = parseInt(process.env.VUE_APP_CONF_ZONE_EXT_ID);
    const zoneEtajId = parseInt(process.env.VUE_APP_CONF_ZONE_ETAJ_ID);
    const zoneParterId = parseInt(process.env.VUE_APP_CONF_ZONE_PARTER_ID);
    const heatPumpDeviceId = parseInt(process.env.HEAT_PUMP_DEVICE_ID);
    const eMeterDeviceId = parseInt(process.env.ELECTRIC_METER_01_DEVICE_ID);
    return {
      zoneIntId,
      zoneExtId,
      zoneEtajId,
      zoneParterId,
      heatPumpDeviceId,
      eMeterDeviceId
    };
  },
  mounted() {
    this.init();
  },
  methods: {
    init() {
    },
    onResize() {
    },
    hasRole: function (roles) {
      return authzService.currentUserValue.permissions.filter(function (userRole) {
        return roles.includes(userRole);
      }).length > 0;
    }
  },
});
</script>
