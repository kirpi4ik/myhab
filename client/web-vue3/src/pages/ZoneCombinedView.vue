<template>
  <q-page class="q-pa-sm">
    <div class="row q-col-gutter-lg">
      <div class="col-lg-4 col-md-4 col-xs-12 col-sm-12" v-for="zone in childZones" v-bind:key="zone.id">
        <zone-card :zone="zone"/>
      </div>

      <div class="col-lg-4 col-md-4 col-xs-12 col-sm-12" v-for="peripheral in peripheralList" v-bind:key="peripheral.id">
        <peripheral-light-card :peripheral="peripheral" v-if="category ==='LIGHT'"/>
        <peripheral-heat-card :peripheral="peripheral" v-if="category ==='HEAT'"/>
      </div>
    </div>
  </q-page>
</template>

<script>
  import ZoneCard from "@/components/cards/ZoneCard";
  import PeripheralLightCard from "@/components/cards/PeripheralLightCard";
  import PeripheralHeatCard from "@/components/cards/PeripheralHeatCard";
  import {ZONE_GET_BY_ID, ZONES_GET_ROOT, NAV_BREADCRUMB} from "@/graphql/queries";
  import {useGlobalQueryLoading, useQuery} from '@vue/apollo-composable';
  import {useRoute} from 'vue-router';
  import _ from "lodash";
  import {ref} from "vue";

  export default {
    components: {
      ZoneCard,
      PeripheralLightCard,
      PeripheralHeatCard
    },
    setup() {
      const route = useRoute();
      const category = route.query.category
      let currentZone = {}
      let childZones = ref([])
      let peripheralList = ref([])
      const toggleMe = ref(true)

      const peripheralFilter = (peripheral) => {
        return peripheral.category.name === category
      }
      const peripheralInitCallback = (peripheral) => {
        if (peripheral != null) {
          if (peripheral.connectedTo && peripheral.connectedTo.length > 0) {
            let port = peripheral.connectedTo[0];
            if (port != null && port.device != null) {
              peripheral['value'] = peripheral.connectedTo[0].value;
              peripheral['state'] = peripheral.connectedTo[0].value === 'OFF';
              peripheral['deviceState'] = peripheral.connectedTo[0].device.status;
            } else {
              peripheral['deviceState'] = 'OFFLINE';
            }
          }
          peripheralList.value.push(
            {
              data: peripheral,
              id: peripheral['id'],
              value: peripheral['value'],
              state: peripheral['state'],
              deviceState: peripheral['deviceState']
            }
          )
        }
      }

      let localPList
      if (route.params.zoneId) {
        const {onResult: onResultById} = useQuery(ZONE_GET_BY_ID, {id: route.params.zoneId}, {
          fetchPolicy: "network-only",   // Used for first execution
          nextFetchPolicy: "cache-and-network" // Used for subsequent executions
        })
        onResultById(queryResult => {
          let data = _.cloneDeep(queryResult.data)
          currentZone = data.zoneById;

          if (currentZone.peripherals) {
            localPList = currentZone.peripherals.filter(peripheralFilter)
            localPList.sort((a, b) => (a.name > b.name) ? 1 : -1)
            localPList.forEach(peripheralInitCallback);
          }
          childZones.value = currentZone.zones;
          // [...childZones].sort((a, b) => (a.name > b.name) ? 1 : -1)
        })
      }
      if (route.params.zoneId == null) {
        const {onResult: onResultRoot} = useQuery(ZONES_GET_ROOT)
        onResultRoot(queryResult => {
          let data = _.cloneDeep(queryResult.data)

          currentZones = [];
          if (data.zonesRoot.peripherals) {
            localPList = data.zonesRoot.peripherals.filter(peripheralFilter)
            localPList.sort((a, b) => (a.name > b.name) ? 1 : -1)
            localPList.forEach(peripheralInitCallback);
          }
          childZones = data.zonesRoot.zones;
          // [...childZones].sort((a, b) => (a.name > b.name) ? 1 : -1)
        })
      }


      const loading = useGlobalQueryLoading()
      return {
        loading,
        childZones,
        peripheralList,
        category,
        toggleMe
      }
    }
  };
</script>
