<template>
  <q-page class="q-pa-sm">
    <div class="row q-col-gutter-lg">
      <div class="col-lg-4 col-md-4 col-xs-12 col-sm-12" v-for="zone in childZones" v-bind:key="zone.id">
        <zone-card :zone="zone"/>
      </div>
      <div class="col-lg-4 col-md-4 col-xs-12 col-sm-12" v-for="peripheral in peripheralList" v-bind:key="peripheral.id">
        <peripheral-light-card :peripheral="peripheral" v-if="category === 'LIGHT'" @onUpdate="updatedPeripheral($event)"/>
        <peripheral-heat-card :peripheral="peripheral" v-if="category === 'HEAT'"/>
      </div>
    </div>
  </q-page>
</template>

<script>
  import {peripheralService} from '@/_services/controls';
  import ZoneCard from '@/components/cards/ZoneCard';
  import PeripheralLightCard from '@/components/cards/PeripheralLightCard';
  import PeripheralHeatCard from '@/components/cards/PeripheralHeatCard';
  import {CACHE_GET_ALL_VALUES, ZONE_GET_BY_ID} from '@/graphql/queries';
  import {useApolloClient, useQuery} from '@vue/apollo-composable';
  import {useRoute} from 'vue-router';
  import _ from 'lodash';
  import {ref} from 'vue';

  export default {
    components: {
      ZoneCard,
      PeripheralLightCard,
      PeripheralHeatCard,
    },
    setup() {
      const {client} = useApolloClient();
      const route = useRoute();
      const category = route.query.category;
      let currentZone = {};
      let childZones = ref([]);
      let peripheralList = ref([]);
      let cacheMap = ref({});


      const {onResult: onCacheResult} = useQuery(CACHE_GET_ALL_VALUES, {}, {fetchPolicy: 'no-cache'});
      onCacheResult(queryResult => {
        cacheMap.value = _.reduce(
          queryResult.data.cacheAll,
          function (hash, value) {
            const key = value['cacheKey'];
            const cacheName = value['cacheName'];
            hash[key] = {[cacheName]: value};
            return hash;
          },
          {},
        );
      });

      const peripheralFilter = peripheral => {
        return peripheral.category.name === category;
      };
      const peripheralInitCallback = peripheral => {
        peripheralList.value.push(peripheralService.peripheralInit(cacheMap, peripheral));
      };

      const loadZones = () => {
        client.query({
          query: ZONE_GET_BY_ID,
          variables: {id: route.params.zoneId},
          fetchPolicy: 'network-only',
        }).then(queryResult => {
          let localPList;
          if (route.params.zoneId) {
            let data = _.cloneDeep(queryResult.data);
            peripheralList.value = [];
            currentZone = data.zoneById;
            if (currentZone.peripherals) {
              localPList = currentZone.peripherals.filter(peripheralFilter);
              localPList.sort((a, b) => (a.name > b.name ? 1 : -1));
              localPList.forEach(peripheralInitCallback);
            }
            childZones.value = currentZone.zones;
          }
        })
      };


      loadZones();

      const updatedPeripheral = peripheral => {
        let index = _.findIndex(peripheralList.value, item => {
          return item.id == peripheral.id;
        });
        peripheralList.value[index] = peripheralService.peripheralInit(null, peripheral);
      };

      return {
        childZones,
        peripheralList,
        category,
        cacheMap,
        updatedPeripheral,
      };
    },
  };
</script>
