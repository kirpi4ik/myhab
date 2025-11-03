<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="port">
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

        <EntityFormActions
          :saving="saving"
          :show-view="false"
          save-label="Create Port"
        />
      </q-card>
    </form>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from 'vue';
import _ from 'lodash';
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute} from "vue-router/dist/vue-router";
import {useEntityCRUD} from '@/composables';
import EntityFormActions from '@/components/EntityFormActions.vue';
import {PORT_EDIT_GET_BY_ID, PORT_CREATE} from '@/graphql/queries';

export default defineComponent({
  name: 'PortNew',
  components: {
    EntityFormActions
  },
  setup() {
    const {client} = useApolloClient();
    const route = useRoute();
    const deviceList = ref([]);
    const deviceListDisabled = ref(false);
    const portTypes = ref([]);
    const portStates = ref([]);

    const {
      entity: port,
      saving,
      createEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Port',
      entityPath: '/admin/ports',
      createMutation: PORT_CREATE,
      createMutationKey: 'devicePortCreate',
      createVariableName: 'devicePort',
      excludeFields: ['__typename'],
      initialData: {
        device: null,
        internalRef: '',
        name: '',
        description: '',
        type: null,
        state: null,
        value: ''
      },
      transformBeforeSave: (data) => {
        const transformed = {...data};
        // Clean device - only send ID
        if (transformed.device) {
          if (transformed.device.id) {
            transformed.device = { id: transformed.device.id };
          } else {
            delete transformed.device;
          }
        }
        return transformed;
      }
    });

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
        console.error('Error fetching port data:', error);
      });
    };

    const onSave = async () => {
      if (saving.value) return;
      
      // Custom validation for device
      if (!port.value.device) {
        return; // validateRequired will handle the notification
      }
      
      if (!validateRequired(port.value, ['device', 'internalRef', 'name'])) return;
      await createEntity();
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
      saving,
      onSave
    };
  }
});

</script>
