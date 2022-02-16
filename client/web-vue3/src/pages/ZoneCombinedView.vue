<template>
  <q-page class="q-pa-sm">
    <div class="row q-col-gutter-lg">

      <div class="col-lg-4 col-md-4 col-xs-12 col-sm-12" v-for="zone in childZones" v-bind:key="zone.id">
        <q-card class="text-white" style="background: linear-gradient(#546e82, #7c919f); height: 100px">
          <q-item>
            <q-item-section avatar>
              <q-avatar size="60px" class="shadow-10">
                <q-icon name="mdi-lightbulb" color="accent"/>
              </q-avatar>
            </q-item-section>

            <q-item-section>
              <q-item-label class="text-weight-bold text-h5">{{ zone.name }}</q-item-label>
              <q-item-label caption>
                {{ des }}
              </q-item-label>
              <q-item-label class="text-grey-8">
                {{ email }}
              </q-item-label>
            </q-item-section>

            <q-item-section side>
              <q-item-label>
                <q-btn size="sm" flat round icon="fab fa-facebook" class="bg-indigo-7 text-white"/>
              </q-item-label>
              <q-item-label>
                <q-btn size="sm" flat round icon="fab fa-twitter" class="bg-info text-white"/>
              </q-item-label>

            </q-item-section>
          </q-item>

        </q-card>
      </div>
      <div class="col-lg-4 col-md-4 col-xs-12 col-sm-12" v-for="peripheral in peripheralList" v-bind:key="peripheral.id">
        <q-card class="text-white" style="background: linear-gradient(#156cb8, #60a3c2)">
          <q-item>
            <q-item-section avatar>
              <q-avatar size="60px" class="shadow-10 bg-blue">
                <q-icon name="mdi-lightbulb"/>
              </q-avatar>
            </q-item-section>

            <q-item-section>
              <q-item-label class="text-weight-medium text-h5">{{ peripheral.data.name }}</q-item-label>
            </q-item-section>

            <q-item-section side>
              <q-item-label>
                <q-btn size="sm" flat round icon="settings" class="text-white"/>
              </q-item-label>
              <q-item-label>
                <q-btn size="sm" flat round icon="settings" class="text-white"/>
              </q-item-label>

            </q-item-section>
          </q-item>

          <q-separator></q-separator>
          <q-card-section>
            <div class="q-pa-sm text-grey-8">
              <toggle v-model="toggleMe"/>
            </div>
          </q-card-section>
        </q-card>
      </div>

    </div>
  </q-page>
</template>

<script>
  import {ZONE_GET_BY_ID, ZONES_GET_ROOT} from "@/graphql/queries";
  import {useGlobalQueryLoading, useQuery} from '@vue/apollo-composable';
  import {useRoute} from 'vue-router';
  import _ from "lodash";
  import {ref} from "vue";
  import Toggle from '@vueform/toggle'

  export default {
    components: {
      Toggle,
    },
    setup() {
      console.log("SETUP2333")
      const route = useRoute();
      const category = route.query.category
      let currentZone = {}
      let childZones = ref([])
      let peripheralList = ref([])
      let breadcrumb = ref([])
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
          console.log(childZones)
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
          console.log(childZones)
        })
      }

      const loading = useGlobalQueryLoading()
      return {
        loading,
        childZones,
        peripheralList,
        breadcrumb,
        category,
        toggleMe
      }
    }
  };
</script>
<style>
  .toggle, .toggle-container {
    width: 100% !important;
    height: 40px !important;
  }

  .toggle-handle {
    height: 40px !important;
    width: 40px !important;
  }

  .toggle-off {
    background-color: red !important;
  }

  .toggle-on .toggle-handle {
    background: linear-gradient(rgb(0, 191, 54), rgb(53, 113, 61)) !important;
    border: 1px solid #386668;
  }

  .toggle-off .toggle-handle {
    background: linear-gradient(rgb(191, 0, 0), rgb(255, 190, 98)) !important;
    border: 1px solid #386668;
  }
</style>
<style src="@vueform/toggle/themes/default.css"></style>
