<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="!loading && device">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-pencil" color="primary" size="sm" class="q-mr-sm"/>
            Edit Device
          </div>
          <div class="text-subtitle2 text-weight-medium">
            {{ device.code }}
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

        <!-- Information Panel -->
        <q-card-section class="bg-blue-grey-1">
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Information</div>
          <div class="row q-gutter-md">
            <div class="col">
              <q-icon name="mdi-identifier" class="q-mr-xs"/>
              <strong>ID:</strong> {{ device.id }}
            </div>
            <div class="col" v-if="device.uid">
              <q-icon name="mdi-key" class="q-mr-xs"/>
              <strong>UID:</strong> {{ device.uid }}
            </div>
            <div class="col">
              <q-icon name="mdi-ethernet" class="q-mr-xs"/>
              <strong>Ports:</strong> {{ device.ports?.length || 0 }}
            </div>
          </div>
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
            Save
          </q-btn>
          <q-btn 
            color="grey" 
            @click="$router.go(-1)" 
            icon="mdi-cancel"
            :disable="saving"
          >
            Cancel
          </q-btn>
          <q-space/>
          <q-btn 
            color="info" 
            :to="`/admin/devices/${$route.params.idPrimary}/view`" 
            icon="mdi-eye" 
            outline
            :disable="saving"
          >
            View
          </q-btn>
        </q-card-actions>
      </q-card>
    </form>

    <!-- Loading State -->
    <q-inner-loading :showing="loading">
      <q-spinner-gears size="50px" color="primary"/>
    </q-inner-loading>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from 'vue';

import {useApolloClient} from "@vue/apollo-composable";
import {prepareForMutation} from '@/_helpers';
import {useRoute, useRouter} from "vue-router";

import {useQuasar} from 'quasar';

import {
  DEVICE_CATEGORIES_LIST,
  DEVICE_GET_BY_ID_CHILDS,
  DEVICE_MODEL_LIST,
  DEVICE_UPDATE,
  RACK_LIST_ALL
} from '@/graphql/queries';

import _ from "lodash";

export default defineComponent({
  name: 'DeviceEdit',
  setup() {
    const $q = useQuasar();
    const {client} = useApolloClient();
    const device = ref({});
    const rackList = ref([]);
    const typeList = ref([]);
    const modelList = ref([]);
    const router = useRouter();
    const route = useRoute();
    const loading = ref(false);
    const saving = ref(false);

    /**
     * Fetch device data and related lists
     */
    const fetchData = () => {
      loading.value = true;

      // Fetch all data in parallel
      Promise.all([
        client.query({
          query: RACK_LIST_ALL,
          variables: {},
          fetchPolicy: 'network-only',
        }),
        client.query({
          query: DEVICE_CATEGORIES_LIST,
          variables: {},
          fetchPolicy: 'network-only',
        }),
        client.query({
          query: DEVICE_MODEL_LIST,
          variables: {},
          fetchPolicy: 'network-only',
        }),
        client.query({
          query: DEVICE_GET_BY_ID_CHILDS,
          variables: {id: route.params.idPrimary},
          fetchPolicy: 'network-only',
        })
      ]).then(([racksResponse, categoriesResponse, modelsResponse, deviceResponse]) => {
        rackList.value = racksResponse.data.rackList || [];
        typeList.value = categoriesResponse.data.deviceCategoryList || [];
        modelList.value = modelsResponse.data.deviceModelList || [];
        device.value = _.cloneDeep(deviceResponse.data.device);
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load device data',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching device:', error);
      });
    };

    /**
     * Update device
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

      // Create a clean copy for mutation, removing Apollo-specific fields
      const cleanDevice = prepareForMutation(device.value, ['__typename', 'id', 'ports']);

      client.mutate({
        mutation: DEVICE_UPDATE,
        variables: {id: route.params.idPrimary, device: cleanDevice},
        fetchPolicy: 'no-cache',
      }).then(response => {
        saving.value = false;
        $q.notify({
          color: 'positive',
          message: 'Device updated successfully',
          icon: 'mdi-check-circle',
          position: 'top'
        });
        fetchData();
      }).catch(error => {
        saving.value = false;
        $q.notify({
          color: 'negative',
          message: error.message || 'Update failed',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error updating device:', error);
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
      loading,
      saving
    };
  }
});

</script>
