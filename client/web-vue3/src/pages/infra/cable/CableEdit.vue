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
        <q-card-section class="bg-blue-grey-1">
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Information</div>
          <div class="row q-gutter-md">
            <div class="col">
              <q-icon name="mdi-identifier" class="q-mr-xs"/>
              <strong>ID:</strong> {{ cable.id }}
            </div>
            <div class="col" v-if="cable.uid">
              <q-icon name="mdi-key" class="q-mr-xs"/>
              <strong>UID:</strong> {{ cable.uid }}
            </div>
            <div class="col">
              <q-icon name="mdi-ethernet" class="q-mr-xs"/>
              <strong>Ports:</strong> {{ cable.connectedTo?.length || 0 }}
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
            :to="`/admin/cables/${$route.params.idPrimary}/view`" 
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
import {useRoute, useRouter} from "vue-router";

import {useQuasar} from 'quasar';

import {useApolloClient} from "@vue/apollo-composable";
import {prepareForMutation} from '@/_helpers';

import {
  CABLE_EDIT_GET_DETAILS,
  CABLE_VALUE_UPDATE
} from '@/graphql/queries';

import PortConnectCard from '@/components/cards/PortConnectCard.vue';

import _ from 'lodash';

export default defineComponent({
  name: 'CableEdit',
  components: {
    PortConnectCard
  },
  setup() {
    const $q = useQuasar();
    const {client} = useApolloClient();
    const cable = ref({ connectedTo: [] });
    const rackList = ref([]);
    const patchPanelList = ref([]);
    const cableCategoryList = ref([]);
    const deviceList = ref([]);
    const router = useRouter();
    const route = useRoute();
    const loading = ref(false);
    const saving = ref(false);

    /**
     * Fetch cable data and related lists
     */
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: CABLE_EDIT_GET_DETAILS,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        cable.value = _.cloneDeep(response.data.cable);
        patchPanelList.value = response.data.patchPanelList || [];
        rackList.value = response.data.rackList || [];
        cableCategoryList.value = response.data.cableCategoryList || [];
        deviceList.value = response.data.deviceList || [];
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load cable data',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching cable:', error);
      });
    };

    /**
     * Update cable
     */
    const onSave = () => {
      // Validate required fields
      if (!cable.value.code || !cable.value.description) {
        $q.notify({
          color: 'negative',
          message: 'Please fill in all required fields',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        return;
      }

      saving.value = true;

      // Create a clean copy for mutation
      const cleanCable = prepareForMutation(cable.value, ['__typename', 'device']);
      
      // Remove id only from top-level cable object, but keep ids in nested arrays
      delete cleanCable.id;
      
      // Clean connectedTo array - remove __typename and device, but keep id
      if (cleanCable.connectedTo && Array.isArray(cleanCable.connectedTo)) {
        cleanCable.connectedTo = cleanCable.connectedTo.map(port => ({
          id: port.id
        }));
      }
      
      // Clean zones array - remove __typename but keep id
      if (cleanCable.zones && Array.isArray(cleanCable.zones)) {
        cleanCable.zones = cleanCable.zones.map(zone => ({
          id: zone.id
        }));
      }

      client.mutate({
        mutation: CABLE_VALUE_UPDATE,
        variables: {id: route.params.idPrimary, cable: cleanCable},
        fetchPolicy: 'no-cache',
        update: () => {
          // Skip cache update to avoid normalization issues with simplified nested objects
        }
      }).then(response => {
        saving.value = false;
        $q.notify({
          color: 'positive',
          message: 'Cable updated successfully',
          icon: 'mdi-check-circle',
          position: 'top'
        });
        router.push({path: `/admin/cables/${response.data.updateCable.id}/view`});
      }).catch(error => {
        saving.value = false;
        $q.notify({
          color: 'negative',
          message: error.message || 'Update failed',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error updating cable:', error);
      });
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
