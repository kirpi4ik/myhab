<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section class="full-width">
          <div class="text-h5">Editare : {{ peripheral.name }}</div>
          <q-select v-model="peripheral.connectedTo"
                    :options="portListFiltered"
                    :disable="portListDisabled"
                    input-debounce="0"
                    @filter="searchPortFn"
                    :option-label="opt => opt && opt.name ? (opt.name + (opt.device ? '[' + opt.device.code + ']' : '')) : ''"
                    map-options
                    stack-label
                    use-chips
                    use-input
                    filled
                    dense
                    multiple
                    label="Port">
            <q-icon name="cancel" @click.stop.prevent="peripheral.connectedTo = []" class="cursor-pointer text-blue"/>
          </q-select>
          <q-input v-model="peripheral.name" label="Name" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="peripheral.description" label="Description" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="peripheral.model" label="Model" clearable clear-icon="close" color="orange"/>
          <q-input v-model="peripheral.maxAmp" label="Max amp" clearable clear-icon="close" color="orange"/>
          <q-select v-model="peripheral.category"
                    :options="categoryList"
                    option-label="name"
                    label="Category" map-options filled dense>
            <q-icon name="cancel" @click.stop.prevent="peripheral.category = null" class="cursor-pointer text-blue"/>
          </q-select>
          <q-separator/>
          <br/>
          <q-select v-model="peripheral.zones"
                    :options="zoneListFiltered"
                    :disable="zoneListDisabled"
                    input-debounce="0"
                    @filter="searchPortFn"
                    :option-label="opt => opt && opt.name ? opt.name : ''"
                    map-options
                    stack-label
                    use-chips
                    use-input
                    filled
                    dense
                    multiple
                    label="Localizare">
            <q-icon name="cancel" @click.stop.prevent="peripheral.zones = []" class="cursor-pointer text-blue"/>
          </q-select>
        </q-card-section>
        <q-separator/>
        <q-card-actions>
          <q-btn color="accent" type="submit">
            Save
          </q-btn>
          <q-btn color="info" @click="$router.go(-1)">
            Cancel
          </q-btn>
        </q-card-actions>
      </q-card>
    </form>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from 'vue';
import {useRoute, useRouter} from "vue-router";

import {useQuasar} from 'quasar';

import {useApolloClient} from "@vue/apollo-composable";
import {prepareForMutation} from '@/_helpers';

import _ from "lodash";

import {
  PERIPHERAL_CATEGORIES,
  PERIPHERAL_GET_BY_ID,
  PERIPHERAL_UPDATE,
  PORT_LIST_ALL,
  ZONES_GET_ALL
} from '@/graphql/queries';



export default defineComponent({
    name: 'PeripheralEdit',
    setup() {
      const $q = useQuasar()
      const {client} = useApolloClient();
      const peripheral = ref({connectedTo: [], zones: [], category: null})
      const categoryList = ref([])
      const router = useRouter();
      const route = useRoute();
      const portList = ref([])
      const portListFiltered = ref([])
      const portListDisabled = ref(false)

      const zoneList = ref([])
      const zoneListFiltered = ref([])
      const zoneListDisabled = ref(false)


      const loading = ref(false)

      const fetchData = () => {
        loading.value = true;
        client.query({
          query: PERIPHERAL_GET_BY_ID,
          variables: {id: route.params.idPrimary},
          fetchPolicy: 'network-only',
        }).then(response => {
          peripheral.value = _.cloneDeep(response.data.devicePeripheral)
          loading.value = false;
        });
        client.query({
          query: PERIPHERAL_CATEGORIES,
          variables: {},
          fetchPolicy: 'network-only',
        }).then(response => {
          categoryList.value = response.data.peripheralCategoryList
        })

        client.query({
          query: PORT_LIST_ALL,
          variables: {},
          fetchPolicy: 'network-only',
        }).then(response => {
          portList.value = _.transform(response.data.devicePortList,
            function (result, value, key) {
              let port = {
                id: value.id,
                name: value.name + '[' + value.device.code + ']'
              }
              result.push(port)
            });
          if (route.query.portId != null) {
            const foundPort = _.find(portList.value, function (o) {
              return o.id == route.query.portId;
            });
            if (foundPort) {
              peripheral.value.connectedTo = [foundPort];
            }
            portListDisabled.value = true
          }
          portListFiltered.value = [...portList.value]
        })
      }
      const searchPortFn = (val, update) => {

        update(() => {
          const needle = val.toLowerCase()
          portListFiltered.value = portList.value.filter(option => {
            return option.name.toLowerCase().includes(needle)
          })
        })
      }
      client.query({
        query: ZONES_GET_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        zoneList.value = _.transform(response.data.zoneList,
          function (result, value, key) {
            let zone = {
              id: value.id,
              name: value.name
            }
            result.push(zone)
          });
        zoneListFiltered.value = [...zoneList.value]
      })

      const onSave = () => {
        if (peripheral.value.hasError) {
          $q.notify({
            color: 'negative',
            message: 'Failed submission'
          })
        } else {
          // Create a clean copy for mutation
          const cleanPeripheral = prepareForMutation(peripheral.value, ['__typename', 'device', 'configurations']);
          
          // Remove id only from top-level peripheral object, but keep ids in nested arrays
          delete cleanPeripheral.id;
          
          // Clean connectedTo array - keep only id field
          if (cleanPeripheral.connectedTo && Array.isArray(cleanPeripheral.connectedTo)) {
            cleanPeripheral.connectedTo = cleanPeripheral.connectedTo.map(port => ({
              id: port.id
            }));
          }
          
          // Clean zones array - keep only id field
          if (cleanPeripheral.zones && Array.isArray(cleanPeripheral.zones)) {
            cleanPeripheral.zones = cleanPeripheral.zones.map(zone => ({
              id: zone.id
            }));
          }

          client.mutate({
            mutation: PERIPHERAL_UPDATE,
            variables: {id: route.params.idPrimary, devicePeripheral: cleanPeripheral},
            // Prevent Apollo cache issues by not updating the cache automatically
            fetchPolicy: 'no-cache',
            // Provide empty update function to prevent cache normalization
            update: () => {
              // Skip cache update to avoid normalization issues with simplified nested objects
            }
          }).then(response => {
            fetchData()
            $q.notify({
              color: 'positive',
              message: 'Peripheral updated'
            })
          }).catch(error => {
            $q.notify({
              color: 'negative',
              message: error.message || 'Update failed'
            })
          });
        }
      }
      onMounted(() => {
        fetchData()
      })
      return {
        peripheral,
        categoryList,
        portList,
        portListFiltered,
        portListDisabled,
        zoneList,
        zoneListFiltered,
        zoneListDisabled,
        onSave,
        searchPortFn
      }
    }
  }
)
;

</script>
