<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-plus-circle" color="primary" size="sm" class="q-mr-sm"/>
            Create New Port
          </div>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Port Configuration</div>
        </q-card-section>

        <q-separator/>

        <q-card-section class="q-gutter-md">
          <q-select v-model="port.device"
                    :options="deviceList"
                    :disable="deviceListDisabled"
                    option-label="name"
                    label="Device *"
                    hint="Select the parent device for this port"
                    map-options
                    filled
                    dense>
            <template v-slot:prepend>
              <q-icon name="mdi-devices"/>
            </template>
            <template v-slot:append v-if="port.device && !deviceListDisabled">
              <q-icon name="mdi-close-circle" @click.stop.prevent="port.device = null" class="cursor-pointer"/>
            </template>
          </q-select>

          <q-input v-model="port.internalRef"
                   label="Internal Reference *"
                   hint="Unique identifier for the port (e.g., ETH0, GPIO_01)"
                   clearable
                   clear-icon="close"
                   color="orange"
                   filled
                   dense
                   :rules="[val => !!val || 'Field is required']">
            <template v-slot:prepend>
              <q-icon name="mdi-identifier"/>
            </template>
          </q-input>

          <q-input v-model="port.name"
                   label="Name *"
                   hint="Display name for the port"
                   clearable
                   clear-icon="close"
                   color="orange"
                   filled
                   dense
                   :rules="[val => !!val || 'Field is required']">
            <template v-slot:prepend>
              <q-icon name="mdi-label"/>
            </template>
          </q-input>

          <q-input v-model="port.description"
                   label="Description"
                   hint="Optional detailed description"
                   type="textarea"
                   rows="3"
                   clearable
                   clear-icon="close"
                   color="green"
                   filled
                   dense>
            <template v-slot:prepend>
              <q-icon name="mdi-text"/>
            </template>
          </q-input>

          <q-select v-model="port.type"
                    :options="portTypes"
                    label="Port Type"
                    hint="Type of the port (optional)"
                    clearable
                    filled
                    dense>
            <template v-slot:prepend>
              <q-icon name="mdi-format-list-bulleted-type"/>
            </template>
          </q-select>

          <q-select v-model="port.state"
                    :options="portStates"
                    label="Port State"
                    hint="Current state of the port (optional)"
                    clearable
                    filled
                    dense>
            <template v-slot:prepend>
              <q-icon name="mdi-state-machine"/>
            </template>
          </q-select>

          <q-input v-model="port.value"
                   label="Value"
                   hint="Current value of the port (optional)"
                   clearable
                   clear-icon="close"
                   color="green"
                   filled
                   dense>
            <template v-slot:prepend>
              <q-icon name="mdi-numeric"/>
            </template>
          </q-input>
        </q-card-section>

        <q-separator/>

        <q-card-actions>
          <q-btn color="primary" type="submit" icon="mdi-content-save">
            Save
          </q-btn>
          <q-btn color="grey" @click="$router.go(-1)" icon="mdi-cancel">
            Cancel
          </q-btn>
        </q-card-actions>
      </q-card>
    </form>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from 'vue';
import _ from 'lodash';

import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router/dist/vue-router";

import {useQuasar} from 'quasar';

import {PORT_EDIT_GET_BY_ID, PORT_CREATE} from '@/graphql/queries';

export default defineComponent({
  name: 'PortNew',
  setup() {
    const $q = useQuasar();
    const {client} = useApolloClient();
    const port = ref({});
    const router = useRouter();
    const route = useRoute();
    const deviceList = ref([]);
    const deviceListDisabled = ref(false);
    const portTypes = ref([]);
    const portStates = ref([]);

    const fetchData = () => {
      client.query({
        query: PORT_EDIT_GET_BY_ID,
        variables: {id: 0}, // Dummy ID to get lists
        fetchPolicy: 'network-only',
      }).then(response => {
        deviceList.value = response.data.deviceList;
        portTypes.value = response.data.portTypes || [];
        portStates.value = response.data.portStates || [];

        // Pre-select device if deviceId is provided in query
        if (route.query.deviceId != null) {
          port.value.device = _.find(response.data.deviceList, function (o) {
            return o.id == route.query.deviceId;
          });
          deviceListDisabled.value = true;
        }
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: 'Failed to load port configuration data',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching port data:', error);
      });
    };

    const onSave = () => {
      // Validate device selection
      if (!port.value.device) {
        $q.notify({
          color: 'negative',
          message: 'Please select a device',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        return;
      }

      // Validate required fields
      if (!port.value.internalRef || !port.value.name) {
        $q.notify({
          color: 'negative',
          message: 'Please fill in all required fields',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        return;
      }

      // Prepare mutation data - clean the device object to only send ID
      const portData = {
        ...port.value,
        device: {
          id: port.value.device.id
        }
      };

      client.mutate({
        mutation: PORT_CREATE,
        variables: {devicePort: portData},
        fetchPolicy: 'no-cache',
        update: () => {
          // Prevent Apollo from processing the mutation result
        }
      }).then(response => {
        $q.notify({
          color: 'positive',
          message: 'Port created successfully',
          icon: 'mdi-check-circle',
          position: 'top'
        });
        router.push({path: `/admin/ports/${response.data.devicePortCreate.id}/edit`});
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: 'Failed to create port',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error creating port:', error);
      });
    };

    onMounted(() => {
      fetchData();
    });

    return {
      port,
      deviceList,
      deviceListDisabled,
      portTypes,
      portStates,
      onSave
    };
  }
});

</script>
