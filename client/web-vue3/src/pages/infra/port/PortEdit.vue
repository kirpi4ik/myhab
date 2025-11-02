<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered v-if="port">
        <q-card-section class="full-width">
          <div class="row items-center q-mb-md">
            <q-icon name="mdi-ethernet" size="md" color="primary" class="q-mr-sm"/>
            <div class="text-h5">Edit port: {{ port.name }}</div>
          </div>
          
          <q-select 
            v-model="port.device"
            :options="deviceList"
            option-label="name"
            label="Device" 
            map-options 
            filled 
            dense
            :rules="[val => !!val || 'Device is required']"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-server" />
            </template>
            <q-icon 
              name="cancel" 
              @click.stop.prevent="port.device = null" 
              class="cursor-pointer text-blue"
            />
          </q-select>
          
          <q-input 
            v-model="port.internalRef" 
            label="Internal Reference" 
            clearable 
            clear-icon="close" 
            color="orange"
            :rules="[val => !!val || 'Field is required']"
            hint="Unique internal identifier for this port"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-identifier" />
            </template>
          </q-input>
          
          <q-input 
            v-model="port.name" 
            label="Name" 
            clearable 
            clear-icon="close" 
            color="orange"
            :rules="[val => !!val || 'Field is required']"
            hint="Display name for this port"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-label" />
            </template>
          </q-input>
          
          <q-input 
            v-model="port.description" 
            label="Description" 
            clearable 
            clear-icon="close" 
            color="green"
            type="textarea"
            rows="2"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-text" />
            </template>
          </q-input>
          
          <q-separator class="q-my-md"/>
          
          <div class="text-h6 q-mb-sm">Port Configuration</div>
          
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
              <q-icon name="mdi-connection" />
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
              <q-icon name="mdi-state-machine" />
            </template>
          </q-select>
          
          <q-input 
            v-model="port.value" 
            label="Current Value" 
            clearable 
            clear-icon="close" 
            color="green"
            hint="Current value or status of the port"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-numeric" />
            </template>
          </q-input>
          
          <q-separator class="q-my-md"/>
          
          <div class="text-subtitle2 text-grey-7 q-mb-sm">
            <q-icon name="info" color="blue" size="sm" class="q-mr-xs"/>
            Related Information
          </div>
          
          <q-list bordered separator class="rounded-borders">
            <q-item v-if="port.cables && port.cables.length > 0">
              <q-item-section avatar>
                <q-icon name="mdi-cable-data" color="orange" />
              </q-item-section>
              <q-item-section>
                <q-item-label>Connected Cables</q-item-label>
                <q-item-label caption>
                  {{ port.cables.length }} cable(s) connected to this port
                </q-item-label>
              </q-item-section>
            </q-item>
            
            <q-item v-if="port.peripherals && port.peripherals.length > 0">
              <q-item-section avatar>
                <q-icon name="mdi-devices" color="green" />
              </q-item-section>
              <q-item-section>
                <q-item-label>Connected Peripherals</q-item-label>
                <q-item-label caption>
                  {{ port.peripherals.length }} peripheral(s) connected to this port
                </q-item-label>
              </q-item-section>
            </q-item>
            
            <q-item>
              <q-item-section avatar>
                <q-icon name="mdi-identifier" color="primary" />
              </q-item-section>
              <q-item-section>
                <q-item-label>Port ID</q-item-label>
                <q-item-label caption>{{ port.id }}</q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
          
        </q-card-section>
        
        <q-separator/>
        
        <q-card-actions>
          <q-btn color="accent" type="submit" icon="save">
            Save
          </q-btn>
          <q-btn color="info" @click="$router.go(-1)" icon="mdi-arrow-left">
            Cancel
          </q-btn>
        </q-card-actions>
      </q-card>
    </form>
  </q-page>
</template>

<script>
import { defineComponent, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { useApolloClient } from '@vue/apollo-composable';
import { prepareForMutation } from '@/_helpers';
import _ from 'lodash';

import { PORT_EDIT_GET_BY_ID, PORT_UPDATE } from '@/graphql/queries';

export default defineComponent({
  name: 'PortEdit',
  setup() {
    const $q = useQuasar();
    const { client } = useApolloClient();
    const router = useRouter();
    const route = useRoute();
    const port = ref(null);
    const deviceList = ref([]);
    const portTypeList = ref([]);
    const portStateList = ref([]);

    const fetchData = () => {
      client.query({
        query: PORT_EDIT_GET_BY_ID,
        variables: { id: route.params.idPrimary },
        fetchPolicy: 'network-only',
      }).then(response => {
        port.value = _.cloneDeep(response.data.devicePort);
        deviceList.value = response.data.deviceList;
        portStateList.value = response.data.portStates;
        portTypeList.value = response.data.portTypes;
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: error.message || 'Failed to load port data',
          icon: 'error'
        });
      });
    };
    
    const onSave = () => {
      // Validate required fields
      if (!port.value.device) {
        $q.notify({
          color: 'negative',
          message: 'Device is required',
          icon: 'warning'
        });
        return;
      }
      
      if (!port.value.internalRef || !port.value.name) {
        $q.notify({
          color: 'negative',
          message: 'Internal Reference and Name are required',
          icon: 'warning'
        });
        return;
      }
      
      // Create a clean copy for mutation
      const cleanPort = prepareForMutation(port.value, [
        '__typename',
        'cables',
        'peripherals',
        'scenarios',
        'subscriptions',
        'configurations',
        'entityType',
        'uid'
      ]);
      
      // Remove id from top-level port object
      delete cleanPort.id;
      
      // Clean device - keep only id
      if (cleanPort.device) {
        cleanPort.device = {
          id: cleanPort.device.id
        };
      }
      
      client.mutate({
        mutation: PORT_UPDATE,
        variables: { 
          id: route.params.idPrimary, 
          port: cleanPort 
        },
        fetchPolicy: 'no-cache',
        update: () => {
          // Skip cache update to avoid normalization issues
        }
      }).then(response => {
        $q.notify({
          color: 'positive',
          message: 'Port updated successfully',
          icon: 'check_circle'
        });
        router.push({ path: `/admin/ports/${response.data.updatePort.id}/view` });
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: error.message || 'Update failed',
          icon: 'error'
        });
      });
    };
    
    onMounted(() => {
      fetchData();
    });
    
    return {
      port,
      deviceList,
      portTypeList,
      portStateList,
      onSave
    };
  }
});
</script>

<style scoped>
.q-my-md {
  margin-top: 16px;
  margin-bottom: 16px;
}
</style>
