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
      patchPanelList
    }
  }
});
</script>
