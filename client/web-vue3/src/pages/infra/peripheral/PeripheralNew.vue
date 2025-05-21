<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section class="full-width">
          <div class="text-h5">Create new peripheral</div>
          <q-select v-model="selectedDevice"
                    :options="deviceList"
                    :disable="deviceListDisabled"
                    option-label="name"
                    label="Devices" map-options emit-value filled dense clearable>
            <q-icon name="cancel" class="cursor-pointer text-blue"/>
          </q-select>
          <q-select v-if="selectedDevice != null"
                    v-model="peripheral.connectedTo"
                    :options="selectedDevice.ports"
                    :disable="portListDisabled"
                    input-debounce="0"
                    option-label="name"
                    map-options
                    stack-label
                    use-chips
                    use-input
                    filled
                    dense
                    multiple
                    label="Port">
            <q-icon name="cancel" @click.stop.prevent="peripheral.connectedTo = null" class="cursor-pointer text-blue"/>
          </q-select>
          <q-input v-model="peripheral.name" label="Name" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="peripheral.description" label="Description" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="peripheral.model" label="Model" clearable clear-icon="close" color="orange"/>
          <q-select v-model="peripheral.category"
                    :options="categoryList"
                    option-label="name"
                    label="Category" map-options filled dense>
            <q-icon name="cancel" @click.stop.prevent="peripheral.category = null" class="cursor-pointer text-blue"/>
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
import {useQuasar} from 'quasar'
import {defineComponent, onMounted, ref} from 'vue';
import {
  DEVICE_LIST_ALL_WITH_PORTS,
  PERIPHERAL_CATEGORIES,
  PERIPHERAL_CREATE,
  PORT_LIST_ALL
} from '@/graphql/queries';
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router/dist/vue-router";
import _ from "lodash";

export default defineComponent({
    name: 'PeripheralNew',
    setup() {
      const $q = useQuasar()
      const {client} = useApolloClient();
      const deviceList = ref([])
      const selectedDevice = ref(null)
      const deviceListDisabled = ref(false)
      const peripheral = ref({connectedTo: []})
      const categoryList = ref([])
      const router = useRouter();
      const route = useRoute();
      const portList = ref([])
      const portListDisabled = ref(false)

      const fetchData = () => {
        client.query({
          query: PERIPHERAL_CATEGORIES,
          variables: {},
          fetchPolicy: 'network-only',
        }).then(response => {
          categoryList.value = response.data.peripheralCategoryList
        })
        client.query({
          query: DEVICE_LIST_ALL_WITH_PORTS,
          variables: {},
          fetchPolicy: 'network-only',
        }).then(response => {
          deviceList.value = response.data.deviceList
          if (route.query.deviceId != null) {
            port.value.device = _.find(response.data.deviceList, function (o) {
              return o.id == route.query.deviceId;
            });
            deviceListDisabled.value = true
            if (route.query.portId != null) {
              portListDisabled.value = true
            }
          }
        });
      }
      const onSave = () => {
        if (peripheral.value.hasError) {
          $q.notify({
            color: 'negative',
            message: 'Failed submission'
          })
        } else {
          client.mutate({
            mutation: PERIPHERAL_CREATE,
            variables: {devicePeripheral: peripheral.value},
          }).then(response => {
            router.push({path: `/admin/peripherals/${response.data.peripheralCreate.id}/edit`})
          });
        }
      }
      onMounted(() => {
        fetchData()
      })
      return {
        selectedDevice,
        deviceList,
        deviceListDisabled,
        peripheral,
        categoryList,
        portList,
        portListDisabled,
        onSave
      }
    }
  }
)
;
</script>
