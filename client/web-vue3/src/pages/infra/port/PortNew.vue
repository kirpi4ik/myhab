<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section class="full-width">
          <div class="text-h5">Create new port</div>
          <q-select v-model="port.device"
                    :options="deviceList"
                    :disable="deviceListDisabled"
                    option-label="name"
                    label="Devices" map-options filled dense>
            <q-icon name="cancel" @click.stop.prevent="cable.rack = null" class="cursor-pointer text-blue"/>
          </q-select>
          <q-input v-model="port.internalRef" label="Internal ref" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="port.name" label="Name" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="port.description" label="Description" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="port.value" label="Value" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
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
import {DEVICE_LIST_ALL, PORT_CREATE} from '@/graphql/queries';
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router/dist/vue-router";

export default defineComponent({
  name: 'PortNew',
  setup() {
    const $q = useQuasar()
    const {client} = useApolloClient();
    const port = ref({})
    const router = useRouter();
    const route = useRoute();
    const deviceList = ref([])
    const deviceListDisabled = ref(false)
    const fetchData = () => {
      client.query({
        query: DEVICE_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        deviceList.value = response.data.deviceList
        if (route.query.deviceId != null) {
          port.value.device = _.find(response.data.deviceList, function (o) {
            return o.id == route.query.deviceId;
          });
          deviceListDisabled.value = true
        }
      });
    }

    const onSave = () => {
      if (port.value.hasError) {
        $q.notify({
          color: 'negative',
          message: 'Failed submission'
        })
      } else {
        client.mutate({
          mutation: PORT_CREATE,
          variables: {devicePort: port.value},
        }).then(response => {
          router.push({path: `/admin/ports/${response.data.devicePortCreate.id}/edit`})
        });
      }
    }
    onMounted(() => {
      fetchData()
    })
    return {
      port,
      deviceList,
      deviceListDisabled,
      onSave
    }
  }
});
</script>
