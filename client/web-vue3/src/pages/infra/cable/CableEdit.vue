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
            <q-input v-model="cable.patchPanelPort" label="Patch panel port" clearable clear-icon="close"
                     color="green"/>
          </q-card-section>
          <q-input v-model="cable.codeOld" label="Code old" clearable clear-icon="close" color="green"/>
          <q-input v-model="cable.codeNew" label="Code new" clearable clear-icon="close" color="green"/>
        </q-card-section>
        <q-card-section>
          <q-item-label>Port connect</q-item-label>
          <div class="row q-col-gutter-xs">
            <q-select v-model="newPortDevice"
                      :options="deviceList"
                      option-label="name"
                      label="Device" map-options filled dense color="green" class="col-lg-2 col-md-2"
                      @update:model-value="selectDevice">
              <q-icon name="cancel" @click.stop.prevent="newPortDevice = null" class="cursor-pointer text-blue"/>
            </q-select>
            <q-select v-if="newPortDevice != null"
                      v-model="newPort"
                      :options="newPortDevice.ports"
                      option-label="name"
                      label="Port" map-options filled dense color="green" class="col-lg-2 col-md-2">
              <q-icon name="cancel" class="cursor-pointer text-blue"
              />
            </q-select>
            <q-btn icon="mdi-link-variant-plus" @click="connectPort()" color="green" label="Connect"/>
          </div>
          <div class="row">
            <q-table
              class="col"
              title="Connected to ports"
              :rows="cable.connectedTo"
              :columns="portColumns"
              @row-click="viewPort"
              row-key="id"
            >
            </q-table>
          </div>
        </q-card-section>
        <q-separator/>
        <q-card-actions>
          <q-btn flat color="secondary" type="submit">
            Save
          </q-btn>
          <q-btn flat color="secondary" :to="$route.matched[1]">
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
  CABLE_BY_ID,
  CABLE_CREATE,
  CABLE_EDIT_GET_DETAILS,
  CABLE_GET_BY_ID_CHILDS,
  CABLE_VALUE_UPDATE,
  RACK_LIST_ALL
} from '@/graphql/queries';
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router/dist/vue-router";

export default defineComponent({
  name: 'CableEdit',
  setup() {
    const $q = useQuasar()
    const {client} = useApolloClient();
    const cable = ref({})
    const rackList = ref([])
    const patchPanelList = ref([])
    const cableCategoryList = ref([])
    const deviceList = ref([])
    const router = useRouter();
    const route = useRoute();
    const newPortDevice = ref(null)
    const newPort = ref()

    const portColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'internalRef', label: 'Int Ref', field: 'internalRef', align: 'left', sortable: true}
    ]

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
        delete cable.value.id
        cable.value.connectedTo.forEach(function (port) {
          delete port.device
        })

        client.mutate({
          mutation: CABLE_VALUE_UPDATE,
          variables: {id: route.params.idPrimary, cable: cable.value},
        }).then(response => {
          router.push({path: `/admin/cables/${response.data.updateCable.id}/view`})
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
      portColumns,
      deviceList,
      newPort,
      newPortDevice,
      viewPort: (evt, row) => {
        if (evt.target.nodeName === 'TD' || evt.target.nodeName === 'DIV') {
          router.push({path: `/admin/ports/${row.id}/view`})
        }
      },
      selectDevice: (evt) => {
        newPort.value = null
      },
      connectPort: (evt) => {
        cable.value.connectedTo.push(newPort.value)
      }
    }
  }
});
</script>
