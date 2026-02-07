<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="device">
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
        <EntityFormActions
          :saving="saving"
          :show-view="false"
          save-label="Create Device"
        />
      </q-card>
    </form>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from 'vue';
import {useApolloClient} from "@vue/apollo-composable";
import {useEntityCRUD} from '@/composables';
import EntityFormActions from '@/components/EntityFormActions.vue';
import {DEVICE_CATEGORIES_LIST, DEVICE_CREATE, DEVICE_MODEL_LIST, RACK_LIST_ALL} from '@/graphql/queries';

export default defineComponent({
  name: 'DeviceNew',
  components: {
    EntityFormActions
  },
  setup() {
    const {client} = useApolloClient();
    
    const rackList = ref([]);
    const typeList = ref([]);
    const modelList = ref([]);

    // Use CRUD composable for create
    const {
      entity: device,
      saving,
      createEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Device',
      entityPath: '/admin/devices',
      createMutation: DEVICE_CREATE,
      createMutationKey: 'deviceCreate',
      createVariableName: 'device',
      excludeFields: ['__typename'],
      initialData: {
        code: '',
        name: '',
        description: '',
        type: null,
        model: null,
        rack: null
      },
      transformBeforeSave: (data) => {
        const transformed = {...data};
        // Clean nested objects - only send IDs if they exist
        if (transformed.rack) {
          if (transformed.rack.id) {
            transformed.rack = { id: transformed.rack.id };
          } else {
            delete transformed.rack;
          }
        }
        if (transformed.type) {
          if (transformed.type.id) {
            transformed.type = { id: transformed.type.id };
          } else {
            delete transformed.type;
          }
        }
        return transformed;
      }
    });

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
    const onSave = async () => {
      // Prevent duplicate submissions
      if (saving.value) return;
      
      // Validate required fields
      if (!validateRequired(device.value, ['code', 'name', 'description'])) {
        return;
      }

      await createEntity();
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
