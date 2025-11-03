<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-plus-circle" color="primary" size="sm" class="q-mr-sm"/>
            Create New Cable
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
                :rules="[val => !!val || 'Number of wires is required']"
              >
                <template v-slot:prepend>
                  <q-icon name="mdi-cable-data"/>
                </template>
              </q-input>
            </div>
            <div class="col-12 col-md-6">
              <q-input 
                v-model.number="cable.maxAmp" 
                label="Max Amperage (Optional)" 
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
                label="Row Number (Optional)" 
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
                label="Order in Row (Optional)" 
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

        <!-- Actions -->
        <q-card-actions>
          <q-btn 
            color="primary" 
            type="submit" 
            icon="mdi-content-save"
            :loading="saving"
          >
            Create Cable
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

import {CABLE_CREATE, RACK_LIST_ALL} from '@/graphql/queries';

export default defineComponent({
  name: 'CableNew',
  setup() {
    const $q = useQuasar();
    const {client} = useApolloClient();
    const cable = ref({
      code: '',
      description: '',
      nrWires: null,
      maxAmp: null,
      rack: null,
      rackRowNr: null,
      orderInRow: null
    });
    const rackList = ref([]);
    const router = useRouter();
    const saving = ref(false);

    /**
     * Fetch rack list from server
     */
    const fetchData = () => {
      client.query({
        query: RACK_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        rackList.value = response.data.rackList || [];
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: 'Failed to load rack list',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching racks:', error);
      });
    };

    /**
     * Create new cable
     */
    const onSave = () => {
      // Validate required fields
      if (!cable.value.code || !cable.value.description || !cable.value.nrWires) {
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
        mutation: CABLE_CREATE,
        variables: {cable: cable.value},
        fetchPolicy: 'no-cache',
      }).then(response => {
        saving.value = false;
        $q.notify({
          color: 'positive',
          message: 'Cable created successfully',
          icon: 'mdi-check-circle',
          position: 'top'
        });
        router.push({path: `/admin/cables/${response.data.cableCreate.id}/edit`});
      }).catch(error => {
        saving.value = false;
        $q.notify({
          color: 'negative',
          message: error.message || 'Failed to create cable',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error creating cable:', error);
      });
    };

    onMounted(() => {
      fetchData();
    });

    return {
      cable,
      onSave,
      rackList,
      saving
    };
  }
});

</script>
