<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section class="full-width">
          <div class="text-h5">Register new device</div>
          <q-select v-model="device.type"
                    :options="typeList"
                    option-label="name"
                    label="Category" map-options filled dense>
            <q-icon name="cancel" @click.stop.prevent="device.type = null" class="cursor-pointer text-blue"/>
          </q-select>
          <q-separator/>
          <br/>
          <q-select v-model="device.model"
                    :options="modelList"
                    label="Model" map-options filled dense>
            <q-icon name="cancel" @click.stop.prevent="device.model = null" class="cursor-pointer text-blue"/>
          </q-select>
          <q-input v-model="device.code" label="Code" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="device.name" label="Name" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="device.description" label="Description" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-select v-model="device.rack"
                    :options="rackList"
                    option-label="name"
                    label="Located" map-options filled dense>
            <q-icon name="cancel" @click.stop.prevent="device.rack = null" class="cursor-pointer text-blue"/>
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

import {useApolloClient} from "@vue/apollo-composable";
import {useRouter} from "vue-router/dist/vue-router";

import {DEVICE_CATEGORIES_LIST, DEVICE_CREATE, DEVICE_MODEL_LIST, RACK_LIST_ALL} from '@/graphql/queries';



export default defineComponent({
  name: 'DeviceNew',
  setup() {
    const $q = useQuasar()
    const {client} = useApolloClient();
    const device = ref({})
    const rackList = ref([])
    const typeList = ref([])
    const modelList = ref([])
    const router = useRouter();

    const fetchData = () => {
      client.query({
        query: RACK_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        rackList.value = response.data.rackList
      })
      client.query({
        query: DEVICE_CATEGORIES_LIST,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        typeList.value = [...response.data.deviceCategoryList]
      })
      client.query({
        query: DEVICE_MODEL_LIST,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        modelList.value = [...response.data.deviceModelList]
      })
    }
    const onSave = () => {
      if (device.value.hasError) {
        $q.notify({
          color: 'negative',
          message: 'Failed submission'
        })
      } else {
        client.mutate({
          mutation: DEVICE_CREATE,
          variables: {device: device.value},
        }).then(response => {
          router.push({path: `/admin/devices/${response.data.deviceCreate.id}/edit`})
        });
      }
    }
    onMounted(() => {
      fetchData()
    })
    return {
      device,
      onSave,
      rackList,
      typeList,
      modelList
    }
  }
});

</script>
