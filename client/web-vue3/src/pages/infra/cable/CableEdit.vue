<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section class="full-width" v-if="cable">
          <div class="text-h5">Edit cable {{ cable.code }}</div>
          <q-input v-model="cable.code" label="Code" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="cable.description" label="Description" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="cable.nrWires" label="Nr wires" clearable clear-icon="close" color="green"/>
          <q-input v-model="cable.maxAmp" label="Max amp" clearable clear-icon="close" color="green"/>
          <q-select v-model="cable.category"
                    :options="cableCategoryList"
                    option-label="name"
                    label="Category" map-options filled dense color="green">
            <q-icon name="cancel" @click.stop.prevent="cable.category = null" class="cursor-pointer text-blue"/>
          </q-select>
          <q-select v-model="cable.rack"
                    :options="rackList"
                    option-label="name"
                    label="Located" map-options filled dense color="green">
            <q-icon name="cancel" @click.stop.prevent="cable.rack = null" class="cursor-pointer text-blue"/>
          </q-select>
          <q-card-section>
            <q-select v-model="cable.patchPanel"
                      :options="patchPanelList"
                      option-label="name"
                      label="Patch panel" map-options filled dense color="green">
              <q-icon name="cancel" @click.stop.prevent="cable.patchPanel = null ; cable.patchPanelPort = null"
                      class="cursor-pointer text-blue"/>
            </q-select>
            <q-select v-model="cable.patchPanelPort" clearable
                      :options="cable.patchPanel!=null?Array.from({length: cable.patchPanel.size}, (_, i) => i + 1):[]"
                      label="Patch panel port"/>
          </q-card-section>
          <q-input v-model="cable.codeOld" label="Code old" clearable clear-icon="close" color="green"/>
          <q-input v-model="cable.codeNew" label="Code new" clearable clear-icon="close" color="green"/>
          <div class="row q-col-gutter-xs">
            <q-input v-model="cable.rackRowNr" label="Row Nr" clearable clear-icon="close" color="green"/>
            <q-input v-model="cable.orderInRow" label="Cable order" clearable clear-icon="close" color="green"/>
          </div>
        </q-card-section>
        <PortConnectCard
          v-model="cable.connectedTo"
          :device-list="deviceList"
          title="Port connect"
          table-title="Connected to ports"
        />
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

import {
  CABLE_BY_ID,
  CABLE_CREATE,
  CABLE_EDIT_GET_DETAILS,
  CABLE_GET_BY_ID_CHILDS,
  CABLE_VALUE_UPDATE,
  RACK_LIST_ALL
} from '@/graphql/queries';

import PortConnectCard from '@/components/cards/PortConnectCard.vue';



export default defineComponent({
  name: 'CableEdit',
  components: {
    PortConnectCard
  },
  setup() {
    const $q = useQuasar()
    const {client} = useApolloClient();
    const cable = ref({ connectedTo: [] })
    const rackList = ref([])
    const patchPanelList = ref([])
    const cableCategoryList = ref([])
    const deviceList = ref([])
    const router = useRouter();
    const route = useRoute();

    const fetchData = () => {
      client.query({
        query: CABLE_EDIT_GET_DETAILS,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        cable.value = _.cloneDeep(response.data.cable)
        patchPanelList.value = response.data.patchPanelList
        rackList.value = response.data.rackList
        cableCategoryList.value = response.data.cableCategoryList
        deviceList.value = response.data.deviceList
      })
    }
    const onSave = () => {
      if (cable.value.hasError) {
        $q.notify({
          color: 'negative',
          message: 'Failed submission'
        })
      } else {
        // Create a clean copy for mutation
        const cleanCable = prepareForMutation(cable.value, ['__typename', 'device']);
        
        // Remove id only from top-level cable object, but keep ids in nested arrays
        delete cleanCable.id;
        
        // Clean connectedTo array - remove __typename and device, but keep id
        if (cleanCable.connectedTo && Array.isArray(cleanCable.connectedTo)) {
          cleanCable.connectedTo = cleanCable.connectedTo.map(port => ({
            id: port.id
          }));
        }
        
        // Clean zones array - remove __typename but keep id
        if (cleanCable.zones && Array.isArray(cleanCable.zones)) {
          cleanCable.zones = cleanCable.zones.map(zone => ({
            id: zone.id
          }));
        }

        client.mutate({
          mutation: CABLE_VALUE_UPDATE,
          variables: {id: route.params.idPrimary, cable: cleanCable},
          // Prevent Apollo cache issues by not updating the cache automatically
          fetchPolicy: 'no-cache',
          // Provide empty update function to prevent cache normalization
          update: () => {
            // Skip cache update to avoid normalization issues with simplified nested objects
          }
        }).then(response => {
          router.push({path: `/admin/cables/${response.data.updateCable.id}/view`})
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
      cable,
      onSave,
      rackList,
      cableCategoryList,
      patchPanelList,
      deviceList
    }
  }
});

</script>
