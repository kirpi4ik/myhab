<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered v-if="port != null">
        <q-card-section class="full-width">
          <div class="row">
            <div class="text-h5">Edit port:&nbsp;</div>
            <div class="text-grey-7 text-h5"> {{ port.name }}</div>
          </div>
          <q-select v-model="port.device"
                    :options="deviceList"
                    option-label="name"
                    label="Devices" map-options filled dense>
            <q-icon name="cancel" @click.stop.prevent="cable.rack = null" class="cursor-pointer text-blue"/>
          </q-select>
          <q-input v-model="port.internalRef" label="Internal ref" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="port.name" label="Name" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="port.description" label="Description" clearable clear-icon="close" color="green"/>
          <q-select v-model="port.type"
                    :options="portTypeList"
                    label="Port types" map-options filled dense>
            <q-icon name="cancel" @click.stop.prevent="port.type = null" class="cursor-pointer text-blue"/>
          </q-select>
          <q-select v-model="port.state"
                    :options="portStateList"
                    label="Port state" map-options filled dense>
            <q-icon name="cancel" @click.stop.prevent="port.state = null" class="cursor-pointer text-blue"/>
          </q-select>
          <q-input v-model="port.value" label="Value" clearable clear-icon="close" color="green"/>
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

import {useApolloClient} from "@vue/apollo-composable";
import {prepareForMutation} from '@/_helpers';
import {useRoute, useRouter} from "vue-router/dist/vue-router";

import {useQuasar} from 'quasar';

import {PORT_EDIT_GET_BY_ID, PORT_UPDATE} from '@/graphql/queries';



export default defineComponent({
  name: 'PortEdit',
  setup() {
    const $q = useQuasar()
    const {client} = useApolloClient();
    const router = useRouter();
    const route = useRoute();
    const port = ref({})
    const deviceList = ref([])
    const portTypeList = ref([])
    const portStateList = ref([])

    const fetchData = () => {
      client.query({
        query: PORT_EDIT_GET_BY_ID,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        port.value = _.cloneDeep(response.data.devicePort)
        deviceList.value = response.data.deviceList
        portStateList.value = response.data.portStates
        portTypeList.value = response.data.portTypes
      })
    }
    const onSave = () => {
      if (port.value.hasError) {
        $q.notify({
          color: 'negative',
          message: 'Failed submission'
        })
      } else {
        // Create a clean copy for mutation
        const cleanPort = prepareForMutation(port.value, ['__typename']);
        
        // Remove id only from top-level port object, but keep ids in nested arrays
        delete cleanPort.id;
        
        // Clean cables array - keep only id field (backend expects this for relationship management)
        if (cleanPort.cables && Array.isArray(cleanPort.cables)) {
          cleanPort.cables = cleanPort.cables.map(cable => ({
            id: cable.id
          }));
        }
        
        // Clean peripherals array - keep only id field (backend expects this for relationship management)
        if (cleanPort.peripherals && Array.isArray(cleanPort.peripherals)) {
          cleanPort.peripherals = cleanPort.peripherals.map(peripheral => ({
            id: peripheral.id
          }));
        }
        
        client.mutate({
          mutation: PORT_UPDATE,
          variables: {id: route.params.idPrimary, port: cleanPort},
        }).then(response => {
          router.push({path: `/admin/ports/${response.data.updatePort.id}/view`})
        });
      }
    }
    onMounted(() => {
      fetchData()
    })
    return {
      port,
      deviceList,
      portTypeList,
      portStateList,
      onSave
    }
  }
});

</script>
