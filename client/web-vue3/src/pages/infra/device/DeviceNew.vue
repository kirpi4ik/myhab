<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-plus-circle" color="primary" size="sm" class="q-mr-sm"/>
            Register New Device
          </div>
        </q-card-section>

        <q-separator/>

        <!-- Basic Information -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Basic Information</div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <q-input 
            v-model="device.code" 
            label="Code" 
            hint="Unique device identifier"
            clearable 
            clear-icon="close" 
            color="orange"
            filled
            dense
            :rules="[val => !!val || 'Code is required']"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-barcode"/>
            </template>
          </q-input>

          <q-input 
            v-model="device.name" 
            label="Name" 
            hint="Device name or label"
            clearable 
            clear-icon="close" 
            color="orange"
            filled
            dense
            :rules="[val => !!val || 'Name is required']"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-label"/>
            </template>
          </q-input>

          <q-input 
            v-model="device.description" 
            label="Description" 
            hint="Device description or purpose"
            clearable 
            clear-icon="close" 
            color="orange"
            filled
            dense
            type="textarea"
            rows="3"
            :rules="[val => !!val || 'Description is required']"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-text"/>
            </template>
          </q-input>
        </q-card-section>

        <q-separator/>

        <!-- Device Classification -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">
            <q-icon name="mdi-shape" class="q-mr-xs"/>
            Device Classification
          </div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <q-select 
            v-model="device.type"
            :options="typeList"
            option-label="name"
            label="Category" 
            hint="Select device category/type"
            map-options 
            filled 
            dense
            clearable
          >
            <template v-slot:prepend>
              <q-icon name="mdi-shape"/>
            </template>
          </q-select>

          <q-select 
            v-model="device.model"
            :options="modelList"
            label="Model" 
            hint="Select device model"
            map-options 
            filled 
            dense
            clearable
          >
            <template v-slot:prepend>
              <q-icon name="mdi-cube-outline"/>
            </template>
          </q-select>
        </q-card-section>

        <q-separator/>

        <!-- Location Information -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">
            <q-icon name="mdi-map-marker" class="q-mr-xs"/>
            Location Information
          </div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <q-select 
            v-model="device.rack"
            :options="rackList"
            option-label="name"
            label="Rack Location" 
            hint="Select the rack where device is located"
            map-options 
            filled 
            dense
            clearable
          >
            <template v-slot:prepend>
              <q-icon name="mdi-server"/>
            </template>
          </q-select>
        </q-card-section>

        <q-separator/>

        <!-- Actions -->
        <q-card-actions>
          <q-btn 
            color="primary" 
            type="submit" 
            icon="mdi-content-save"
            :loading="saving"
          >
            Create Device
          </q-btn>
          <q-btn 
            color="grey" 
            @click="$router.go(-1)" 
            icon="mdi-cancel"
            :disable="saving"
          >
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
import {useRouter} from "vue-router";

import {useQuasar} from 'quasar';

import {DEVICE_CATEGORIES_LIST, DEVICE_CREATE, DEVICE_MODEL_LIST, RACK_LIST_ALL} from '@/graphql/queries';

export default defineComponent({
  name: 'DeviceNew',
  setup() {
    const $q = useQuasar();
    const {client} = useApolloClient();
    const device = ref({
      code: '',
      name: '',
      description: '',
      type: null,
      model: null,
      rack: null
    });
    const rackList = ref([]);
    const typeList = ref([]);
    const modelList = ref([]);
    const router = useRouter();
    const saving = ref(false);

    /**
     * Fetch all required lists from server
     */
    const fetchData = () => {
      // Fetch racks
      client.query({
        query: RACK_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        rackList.value = response.data.rackList || [];
      }).catch(error => {
        console.error('Error fetching racks:', error);
      });

      // Fetch device categories
      client.query({
        query: DEVICE_CATEGORIES_LIST,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        typeList.value = response.data.deviceCategoryList || [];
      }).catch(error => {
        console.error('Error fetching device categories:', error);
      });

      // Fetch device models
      client.query({
        query: DEVICE_MODEL_LIST,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        modelList.value = response.data.deviceModelList || [];
      }).catch(error => {
        console.error('Error fetching device models:', error);
      });
    };

    /**
     * Create new device
     */
    const onSave = () => {
      // Validate required fields
      if (!device.value.code || !device.value.name || !device.value.description) {
        $q.notify({
          color: 'negative',
          message: 'Please fill in all required fields',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        return;
      }

      saving.value = true;

      client.mutate({
        mutation: DEVICE_CREATE,
        variables: {device: device.value},
        fetchPolicy: 'no-cache',
      }).then(response => {
        saving.value = false;
        $q.notify({
          color: 'positive',
          message: 'Device created successfully',
          icon: 'mdi-check-circle',
          position: 'top'
        });
        router.push({path: `/admin/devices/${response.data.deviceCreate.id}/edit`});
      }).catch(error => {
        saving.value = false;
        $q.notify({
          color: 'negative',
          message: error.message || 'Failed to create device',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error creating device:', error);
      });
    };

    onMounted(() => {
      fetchData();
    });

    return {
      device,
      onSave,
      rackList,
      typeList,
      modelList,
      saving
    };
  }
});

</script>
