<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="!loading && port">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-ethernet" color="primary" size="sm" class="q-mr-sm"/>
            Edit Port
          </div>
          <div class="text-subtitle2 text-weight-medium">
            {{ port.name }}
          </div>
        </q-card-section>

        <q-separator/>

        <!-- Basic Information -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Basic Information</div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          
          <q-select 
            v-model="port.device"
            :options="deviceList"
            option-label="name"
            label="Device" 
            map-options 
            filled 
            dense
            clearable
            :rules="[val => !!val || 'Device is required']"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-server"/>
            </template>
          </q-select>
          
          <q-input 
            v-model="port.internalRef" 
            label="Internal Reference" 
            clearable 
            clear-icon="close" 
            color="orange"
            filled
            dense
            :rules="[val => !!val || 'Field is required']"
            hint="Unique internal identifier for this port"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-identifier"/>
            </template>
          </q-input>
          
          <q-input 
            v-model="port.name" 
            label="Name" 
            clearable 
            clear-icon="close" 
            color="orange"
            filled
            dense
            :rules="[val => !!val || 'Field is required']"
            hint="Display name for this port"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-label"/>
            </template>
          </q-input>
          
          <q-input 
            v-model="port.description" 
            label="Description" 
            clearable 
            clear-icon="close" 
            color="green"
            filled
            dense
            type="textarea"
            rows="2"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-text"/>
            </template>
          </q-input>
        </q-card-section>

        <q-separator/>

        <!-- Port Configuration -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Port Configuration</div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          
          <q-select 
            v-model="port.type"
            :options="portTypeList"
            label="Port Type" 
            map-options 
            filled 
            dense
            clearable
          >
            <template v-slot:prepend>
              <q-icon name="mdi-connection"/>
            </template>
          </q-select>
          
          <q-select 
            v-model="port.state"
            :options="portStateList"
            label="Port State" 
            map-options 
            filled 
            dense
            clearable
          >
            <template v-slot:prepend>
              <q-icon name="mdi-state-machine"/>
            </template>
          </q-select>
          
          <q-input 
            v-model="port.value" 
            label="Current Value" 
            clearable 
            clear-icon="close" 
            color="green"
            filled
            dense
            hint="Current value or status of the port"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-numeric"/>
            </template>
          </q-input>
        </q-card-section>

        <q-separator/>

        <!-- Information Panel -->
        <EntityInfoPanel
          :entity="port"
          icon="mdi-ethernet"
          :extra-info="[
            { icon: 'mdi-cable-data', label: 'Cables', value: port.cables?.length || 0 },
            { icon: 'mdi-devices', label: 'Peripherals', value: port.peripherals?.length || 0 }
          ]"
        />

        <q-separator/>

        <!-- Actions -->
        <EntityFormActions
          :saving="saving"
          :show-view="true"
          :view-route="`/admin/ports/${$route.params.idPrimary}/view`"
          save-label="Save Port"
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
import { defineComponent, onMounted, ref, computed } from 'vue';
import { useRoute } from 'vue-router';
import { useEntityCRUD } from '@/composables';
import EntityInfoPanel from '@/components/EntityInfoPanel.vue';
import EntityFormActions from '@/components/EntityFormActions.vue';

import { PORT_EDIT_GET_BY_ID, PORT_UPDATE } from '@/graphql/queries';

export default defineComponent({
  name: 'PortEdit',
  components: {
    EntityInfoPanel,
    EntityFormActions
  },
  setup() {
    const route = useRoute();
    const deviceList = ref([]);
    const portTypeList = ref([]);
    const portStateList = ref([]);

    const {
      entity: port,
      loading,
      saving,
      fetchEntity,
      updateEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Port',
      entityPath: '/admin/ports',
      getQuery: PORT_EDIT_GET_BY_ID,
      getQueryKey: 'devicePort',
      updateMutation: PORT_UPDATE,
      updateMutationKey: 'updatePort',
      excludeFields: ['__typename', 'cables', 'peripherals', 'scenarios', 'subscriptions', 'configurations', 'entityType', 'tsCreated', 'tsUpdated'],
      transformBeforeSave: (data) => {
        const transformed = {...data};
        
        // Clean device - keep only id
        if (transformed.device) {
          transformed.device = { id: transformed.device.id };
        }
        
        return transformed;
      }
    });

    /**
     * Custom fetch to load additional data (devices, types, states)
     */
    const fetchData = async () => {
      const response = await fetchEntity();
      if (response) {
        deviceList.value = response.deviceList || [];
        portStateList.value = response.portStates || [];
        portTypeList.value = response.portTypes || [];
      }
    };

    /**
     * Save port
     */
    const onSave = async () => {
      // Prevent duplicate submissions
      if (saving.value) {
        return;
      }

      if (!validateRequired(port.value, ['device', 'internalRef', 'name'])) {
        return;
      }

      await updateEntity();
    };

    onMounted(() => {
      fetchData();
    });

    return {
      port,
      deviceList,
      portTypeList,
      portStateList,
      loading,
      saving,
      onSave
    };
  }
});
</script>
