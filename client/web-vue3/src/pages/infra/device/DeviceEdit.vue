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

          <LocationSelector
            v-model="device.zones"
            label="Zones"
            hint="Select zones where this device is located"
          />
        </q-card-section>

        <q-separator/>

        <!-- Information Panel -->
        <EntityInfoPanel
          :entity="device"
          icon="mdi-devices"
          :extra-info="[
            { icon: 'mdi-ethernet', label: 'Ports', value: device.ports?.length || 0 }
          ]"
        />

        <q-separator/>

        <!-- Actions -->
        <EntityFormActions
          :saving="saving"
          :show-view="true"
          :view-route="`/admin/devices/${$route.params.idPrimary}/view`"
          save-label="Save Device"
        />
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
import {useRoute} from "vue-router";
import {useApolloClient} from "@vue/apollo-composable";
import {useEntityCRUD} from '@/composables';
import EntityInfoPanel from '@/components/EntityInfoPanel.vue';
import EntityFormActions from '@/components/EntityFormActions.vue';
import LocationSelector from '@/components/selectors/LocationSelector.vue';

import {
  DEVICE_CATEGORIES_LIST,
  DEVICE_GET_BY_ID_CHILDS,
  DEVICE_MODEL_LIST,
  DEVICE_UPDATE_CUSTOM,
  RACK_LIST_ALL
} from '@/graphql/queries';

export default defineComponent({
  name: 'DeviceEdit',
  components: {
    EntityInfoPanel,
    EntityFormActions,
    LocationSelector
  },
  setup() {
    const route = useRoute();
    const {client} = useApolloClient();
    
    // Additional data lists
    const rackList = ref([]);
    const typeList = ref([]);
    const modelList = ref([]);

    // Use CRUD composable
    const {
      entity: device,
      loading,
      saving,
      fetchEntity,
      updateEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Device',
      entityPath: '/admin/devices',
      getQuery: DEVICE_GET_BY_ID_CHILDS,
      getQueryKey: 'device',
      updateMutation: DEVICE_UPDATE_CUSTOM,
      updateMutationKey: 'deviceUpdateCustom',
      updateVariableName: 'device',
      excludeFields: ['__typename', 'ports', 'authAccounts', 'tsCreated', 'tsUpdated'],
      transformBeforeSave: (data) => {
        // Ensure nested objects only send their ID
        const transformed = {...data};
        
        // Clean rack - only send if it has an id, otherwise remove
        if (transformed.rack) {
          transformed.rack = transformed.rack.id ? { id: transformed.rack.id } : null;
        }
        
        // Clean type - only send if it has an id, otherwise remove
        if (transformed.type) {
          transformed.type = transformed.type.id ? { id: transformed.type.id } : null;
        }
        
        // Clean zones array - keep only id
        if (transformed.zones && Array.isArray(transformed.zones)) {
          transformed.zones = transformed.zones
            .filter(zone => zone && zone.id)
            .map(zone => ({ id: zone.id }));
        }
        
        return transformed;
      }
    });

    /**
     * Fetch device data and related lists
     */
    const fetchData = async () => {
      // Fetch device data
      const response = await fetchEntity();
      
      if (response) {
        // Fetch related lists in parallel
        Promise.all([
          client.query({
            query: RACK_LIST_ALL,
            fetchPolicy: 'network-only',
          }),
          client.query({
            query: DEVICE_CATEGORIES_LIST,
            fetchPolicy: 'network-only',
          }),
          client.query({
            query: DEVICE_MODEL_LIST,
            fetchPolicy: 'network-only',
          })
        ]).then(([racksResponse, categoriesResponse, modelsResponse]) => {
          rackList.value = racksResponse.data.rackList || [];
          typeList.value = categoriesResponse.data.deviceCategoryList || [];
          modelList.value = modelsResponse.data.deviceModelList || [];
        }).catch(error => {
          console.error('Error fetching related lists:', error);
        });
      }
    };

    /**
     * Save device
     */
    const onSave = async () => {
      // Validate required fields
      if (!validateRequired(device.value, ['code', 'name', 'description'])) {
        return;
      }

      await updateEntity();
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
