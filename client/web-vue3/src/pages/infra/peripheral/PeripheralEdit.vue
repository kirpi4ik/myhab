<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="!loading && peripheral">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-pencil" color="primary" size="sm" class="q-mr-sm"/>
            Edit Peripheral
          </div>
          <div class="text-subtitle2 text-weight-medium">
            {{ peripheral.name }}
          </div>
        </q-card-section>

        <q-separator/>

        <!-- Basic Information -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Basic Information</div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <q-input 
            v-model="peripheral.name" 
            label="Name" 
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
            v-model="peripheral.description" 
            label="Description" 
            clearable 
            clear-icon="close" 
            color="orange"
            filled
            dense
            type="textarea"
            rows="2"
            :rules="[val => !!val || 'Description is required']"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-text"/>
            </template>
          </q-input>

          <q-input 
            v-model="peripheral.model" 
            label="Model" 
            clearable 
            clear-icon="close" 
            color="orange"
            filled
            dense
          >
            <template v-slot:prepend>
              <q-icon name="mdi-chip"/>
            </template>
          </q-input>

          <q-input 
            v-model.number="peripheral.maxAmp" 
            label="Max Amperage" 
            clearable 
            clear-icon="close" 
            color="orange"
            type="number"
            min="0"
            step="0.1"
            filled
            dense
          >
            <template v-slot:prepend>
              <q-icon name="mdi-lightning-bolt"/>
            </template>
          </q-input>

          <q-select 
            v-model="peripheral.category"
            :options="categoryList"
            option-label="name"
            label="Category" 
            map-options 
            filled 
            dense
            clearable
          >
            <template v-slot:prepend>
              <q-icon name="mdi-shape"/>
            </template>
          </q-select>
        </q-card-section>

        <q-separator/>

        <!-- Location -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Location</div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <LocationSelector
            v-model="peripheral.zones"
            label="Zones"
            hint="Select zones where this peripheral is located"
          />
        </q-card-section>

        <q-separator/>

        <!-- Cable Connections -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Cable Connections</div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <CableSelector
            v-model="peripheral.cables"
            label="Cables"
            hint="Select cables connected to this peripheral"
          />
        </q-card-section>

        <q-separator/>

        <!-- Port Connections -->
        <PortConnectCard
          v-model="peripheral.connectedTo"
          :device-list="deviceList"
          title="Port Connections"
          table-title="Connected Ports"
        />

        <q-separator/>

        <!-- Information Panel -->
        <EntityInfoPanel
          :entity="peripheral"
          icon="mdi-devices"
          :extra-info="[
            { icon: 'mdi-ethernet', label: 'Ports', value: peripheral.connectedTo?.length || 0 },
            { icon: 'mdi-map-marker', label: 'Zones', value: peripheral.zones?.length || 0 },
            { icon: 'mdi-cable-data', label: 'Cables', value: peripheral.cables?.length || 0 }
          ]"
        />

        <q-separator/>

        <!-- Actions -->
        <EntityFormActions
          :saving="saving"
          :show-view="true"
          :view-route="`/admin/peripherals/${$route.params.idPrimary}/view`"
          save-label="Save Peripheral"
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
import { defineComponent, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useEntityCRUD } from '@/composables';
import EntityInfoPanel from '@/components/EntityInfoPanel.vue';
import EntityFormActions from '@/components/EntityFormActions.vue';
import PortConnectCard from '@/components/cards/PortConnectCard.vue';
import LocationSelector from '@/components/selectors/LocationSelector.vue';
import CableSelector from '@/components/selectors/CableSelector.vue';

import {
  PERIPHERAL_CATEGORIES,
  PERIPHERAL_GET_BY_ID,
  PERIPHERAL_UPDATE
} from '@/graphql/queries';

import { useApolloClient } from '@vue/apollo-composable';

export default defineComponent({
  name: 'PeripheralEdit',
  components: {
    EntityInfoPanel,
    EntityFormActions,
    PortConnectCard,
    LocationSelector,
    CableSelector
  },
  setup() {
    const route = useRoute();
    const { client } = useApolloClient();
    const categoryList = ref([]);
    const deviceList = ref([]);

    const {
      entity: peripheral,
      loading,
      saving,
      fetchEntity,
      updateEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Peripheral',
      entityPath: '/admin/peripherals',
      getQuery: PERIPHERAL_GET_BY_ID,
      getQueryKey: 'devicePeripheral',
      updateMutation: PERIPHERAL_UPDATE,
      updateMutationKey: 'updatePeripheral',
      updateVariableName: 'devicePeripheral',
      excludeFields: ['__typename', 'device', 'configurations', 'tsCreated', 'tsUpdated'],
      transformBeforeSave: (data) => {
        const transformed = {...data};
        
        // Clean connectedTo array - keep only id
        if (transformed.connectedTo && Array.isArray(transformed.connectedTo)) {
          transformed.connectedTo = transformed.connectedTo.map(port => ({ id: port.id }));
        }
        
        // Clean zones array - keep only id
        if (transformed.zones && Array.isArray(transformed.zones)) {
          transformed.zones = transformed.zones.map(zone => ({ id: zone.id }));
        }
        
        // Clean cables array - keep only id
        if (transformed.cables && Array.isArray(transformed.cables)) {
          transformed.cables = transformed.cables.map(cable => ({ id: cable.id }));
        }
        
        // Clean category - keep only id
        if (transformed.category) {
          transformed.category = { id: transformed.category.id };
        }
        
        return transformed;
      }
    });

    /**
     * Custom fetch to load additional data
     */
    const fetchData = async () => {
      const response = await fetchEntity();
      if (response) {
        deviceList.value = response.deviceList || [];
        
        // Handle portId from query parameter
        if (route.query.portId != null) {
          for (const device of deviceList.value) {
            const foundPort = device.ports.find(port => port.id == route.query.portId);
            if (foundPort) {
              peripheral.value.connectedTo = [foundPort];
              break;
            }
          }
        }
      }

      // Fetch categories
      client.query({
        query: PERIPHERAL_CATEGORIES,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        categoryList.value = response.data.peripheralCategoryList;
      });
    };

    /**
     * Save peripheral
     */
    const onSave = async () => {
      // Prevent duplicate submissions
      if (saving.value) {
        return;
      }

      if (!validateRequired(peripheral.value, ['name', 'description'])) {
        return;
      }

      await updateEntity();
    };

    onMounted(() => {
      fetchData();
    });

    return {
      peripheral,
      categoryList,
      deviceList,
      loading,
      saving,
      onSave
    };
  }
});
</script>
