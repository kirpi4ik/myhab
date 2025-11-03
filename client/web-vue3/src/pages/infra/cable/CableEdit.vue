<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="!loading && cable">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-pencil" color="primary" size="sm" class="q-mr-sm"/>
            Edit Cable
          </div>
          <div class="text-subtitle2 text-weight-medium">
            {{ cable.code }}
          </div>
        </q-card-section>

        <q-separator/>

        <!-- Basic Information -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Basic Information</div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <q-input 
            v-model="cable.code" 
            label="Code" 
            hint="Unique cable identifier"
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
            v-model="cable.description" 
            label="Description" 
            hint="Cable description or purpose"
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

          <div class="row q-col-gutter-md">
            <div class="col-12 col-md-6">
              <q-input 
                v-model.number="cable.nrWires" 
                label="Number of Wires" 
                hint="Total wire count"
                clearable 
                clear-icon="close" 
                color="orange"
                type="number"
                min="1"
                filled
                dense
              >
                <template v-slot:prepend>
                  <q-icon name="mdi-cable-data"/>
                </template>
              </q-input>
            </div>
            <div class="col-12 col-md-6">
              <q-input 
                v-model.number="cable.maxAmp" 
                label="Max Amperage" 
                hint="Maximum current capacity"
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
            </div>
          </div>

          <q-select 
            v-model="cable.category"
            :options="cableCategoryList"
            option-label="name"
            label="Category" 
            hint="Cable category type"
            map-options 
            filled 
            dense
            clearable
          >
            <template v-slot:prepend>
              <q-icon name="mdi-shape"/>
            </template>
          </q-select>

          <div class="row q-col-gutter-md">
            <div class="col-12 col-md-6">
              <q-input 
                v-model="cable.codeOld" 
                label="Code Old" 
                hint="Previous cable code"
                clearable 
                clear-icon="close" 
                color="orange"
                filled
                dense
              >
                <template v-slot:prepend>
                  <q-icon name="mdi-barcode-scan"/>
                </template>
              </q-input>
            </div>
            <div class="col-12 col-md-6">
              <q-input 
                v-model="cable.codeNew" 
                label="Code New" 
                hint="New cable code"
                clearable 
                clear-icon="close" 
                color="orange"
                filled
                dense
              >
                <template v-slot:prepend>
                  <q-icon name="mdi-barcode-scan"/>
                </template>
              </q-input>
            </div>
          </div>
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
            v-model="cable.rack"
            :options="rackList"
            option-label="name"
            label="Rack Location" 
            hint="Select the rack where cable is located"
            map-options 
            filled 
            dense
            clearable
          >
            <template v-slot:prepend>
              <q-icon name="mdi-server"/>
            </template>
          </q-select>

          <div class="row q-col-gutter-md">
            <div class="col-12 col-md-6">
              <q-input 
                v-model.number="cable.rackRowNr" 
                label="Row Number" 
                hint="Row position in rack"
                clearable 
                clear-icon="close" 
                color="orange"
                type="number"
                min="1"
                filled
                dense
              >
                <template v-slot:prepend>
                  <q-icon name="mdi-grid"/>
                </template>
              </q-input>
            </div>
            <div class="col-12 col-md-6">
              <q-input 
                v-model.number="cable.orderInRow" 
                label="Order in Row" 
                hint="Cable order within row"
                clearable 
                clear-icon="close" 
                color="orange"
                type="number"
                min="1"
                filled
                dense
              >
                <template v-slot:prepend>
                  <q-icon name="mdi-sort-numeric-ascending"/>
                </template>
              </q-input>
            </div>
          </div>
        </q-card-section>

        <q-separator/>

        <!-- Patch Panel Information -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">
            <q-icon name="mdi-lan" class="q-mr-xs"/>
            Patch Panel Information
          </div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <q-select 
            v-model="cable.patchPanel"
            :options="patchPanelList"
            option-label="name"
            label="Patch Panel" 
            hint="Select patch panel"
            map-options 
            filled 
            dense
            clearable
            @update:model-value="cable.patchPanelPort = null"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-lan"/>
            </template>
          </q-select>

          <q-select 
            v-model="cable.patchPanelPort" 
            :options="cable.patchPanel ? Array.from({length: cable.patchPanel.size}, (_, i) => i + 1) : []"
            label="Patch Panel Port"
            hint="Select port number on patch panel"
            :disable="!cable.patchPanel"
            filled
            dense
            clearable
          >
            <template v-slot:prepend>
              <q-icon name="mdi-ethernet"/>
            </template>
          </q-select>
        </q-card-section>

        <q-separator/>

        <!-- Port Connections -->
        <PortConnectCard
          v-if="cable.connectedTo"
          v-model="cable.connectedTo"
          :device-list="deviceList"
          title="Port Connections"
          table-title="Connected to ports"
        />

        <q-separator/>

        <!-- Information Panel -->
        <EntityInfoPanel
          :entity="cable"
          icon="mdi-cable-data"
          :extra-info="[
            { icon: 'mdi-ethernet', label: 'Ports', value: cable.connectedTo?.length || 0 }
          ]"
        />

        <q-separator/>

        <!-- Actions -->
        <EntityFormActions
          :saving="saving"
          :show-view="true"
          :view-route="`/admin/cables/${$route.params.idPrimary}/view`"
          save-label="Save Cable"
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

import {
  CABLE_EDIT_GET_DETAILS,
  CABLE_VALUE_UPDATE
} from '@/graphql/queries';

export default defineComponent({
  name: 'CableEdit',
  components: {
    EntityInfoPanel,
    EntityFormActions,
    PortConnectCard
  },
  setup() {
    const route = useRoute();
    const rackList = ref([]);
    const patchPanelList = ref([]);
    const cableCategoryList = ref([]);
    const deviceList = ref([]);

    const {
      entity: cable,
      loading,
      saving,
      fetchEntity,
      updateEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Cable',
      entityPath: '/admin/cables',
      getQuery: CABLE_EDIT_GET_DETAILS,
      getQueryKey: 'cable',
      updateMutation: CABLE_VALUE_UPDATE,
      updateMutationKey: 'updateCable',
      excludeFields: ['__typename', 'device', 'tsCreated', 'tsUpdated'],
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
        
        // Clean rack - keep only id
        if (transformed.rack) {
          transformed.rack = { id: transformed.rack.id };
        }
        
        // Clean category - keep only id
        if (transformed.category) {
          transformed.category = { id: transformed.category.id };
        }
        
        // Clean patchPanel - keep only id
        if (transformed.patchPanel) {
          transformed.patchPanel = { id: transformed.patchPanel.id };
        }
        
        return transformed;
      }
    });

    /**
     * Custom fetch to load additional data (racks, categories, devices, etc.)
     */
    const fetchData = async () => {
      const response = await fetchEntity();
      if (response) {
        patchPanelList.value = response.patchPanelList || [];
        rackList.value = response.rackList || [];
        cableCategoryList.value = response.cableCategoryList || [];
        deviceList.value = response.deviceList || [];
      }
    };

    /**
     * Save cable
     */
    const onSave = async () => {
      // Prevent duplicate submissions
      if (saving.value) {
        return;
      }

      if (!validateRequired(cable.value, ['code', 'description'])) {
        return;
      }

      await updateEntity();
    };

    onMounted(() => {
      fetchData();
    });

    return {
      cable,
      onSave,
      rackList,
      cableCategoryList,
      patchPanelList,
      deviceList,
      loading,
      saving
    };
  }
});
</script>
