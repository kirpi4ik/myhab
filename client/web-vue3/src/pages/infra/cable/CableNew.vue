<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section class="full-width">
          <div class="text-h5">Create new cable</div>
          <q-input v-model="cable.code" label="Code" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="cable.description" label="Description" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="cable.nrWires" label="Nr wires" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-select v-model="cable.rack"
                    :options="rackList"
                    option-label="name"
                    label="Located" map-options filled dense>
            <q-icon name="cancel" @click.stop.prevent="cable.rack = null" class="cursor-pointer text-blue"/>
          </q-select>
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
import {CABLE_CREATE, RACK_LIST_ALL} from '@/graphql/queries';
import {useApolloClient} from "@vue/apollo-composable";
import {useRouter} from "vue-router/dist/vue-router";

export default defineComponent({
  name: 'CableNew',
  setup() {
    const $q = useQuasar()
    const {client} = useApolloClient();
    const cable = ref({})
    const rackList = ref([])
    const router = useRouter();

    const fetchData = () => {
      client.query({
        query: RACK_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
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
        client.mutate({
          mutation: CABLE_CREATE,
          variables: {cable: cable.value},
        }).then(response => {
          router.push({path: `/admin/cables/${response.data.cableCreate.id}/edit`})
        });
      }
    }
    onMounted(() => {
      fetchData()
    })
    return {
      cable,
      onSave,
      rackList
    }
  }
});
</script>
