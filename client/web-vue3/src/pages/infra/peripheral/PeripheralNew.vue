<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="peripheral">
      <q-card flat bordered>
        <q-card-section class="full-width">
          <div class="text-h5">Create new peripheral</div>
          <q-select v-model="selectedDevice"
                    :options="deviceList"
                    :disable="deviceListDisabled"
                    option-label="name"
                    label="Devices" map-options emit-value filled dense clearable>
            <q-icon name="cancel" class="cursor-pointer text-blue"/>
          </q-select>
          <q-select v-if="selectedDevice != null"
                    v-model="peripheral.connectedTo"
                    :options="selectedDevice.ports"
                    :disable="portListDisabled"
                    input-debounce="0"
                    option-label="name"
                    map-options
                    stack-label
                    use-chips
                    use-input
                    filled
                    dense
                    multiple
                    label="Port">
            <q-icon name="cancel" @click.stop.prevent="peripheral.connectedTo = null" class="cursor-pointer text-blue"/>
          </q-select>
          <q-input v-model="peripheral.name" label="Name" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="peripheral.description" label="Description" clearable clear-icon="close" color="orange"
                   :rules="[val => !!val || 'Field is required']"/>
          <q-input v-model="peripheral.model" label="Model" clearable clear-icon="close" color="orange"/>
          <q-select v-model="peripheral.category"
                    :options="categoryList"
                    option-label="name"
                    label="Category" map-options filled dense>
            <q-icon name="cancel" @click.stop.prevent="peripheral.category = null" class="cursor-pointer text-blue"/>
          </q-select>
        </q-card-section>
        <q-separator/>
        <EntityFormActions
          :saving="saving"
          :show-view="false"
          save-label="Create Peripheral"
        />
      </q-card>
    </form>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from 'vue';
import {useApolloClient} from "@vue/apollo-composable";
import {useRoute} from "vue-router/dist/vue-router";
import _ from "lodash";
import {useEntityCRUD} from '@/composables';
import EntityFormActions from '@/components/EntityFormActions.vue';
import {
  DEVICE_LIST_ALL_WITH_PORTS,
  PERIPHERAL_CATEGORIES,
  PERIPHERAL_CREATE
} from '@/graphql/queries';

export default defineComponent({
  name: 'PeripheralNew',
  components: {
    EntityFormActions
  },
  setup() {
    const {client} = useApolloClient();
    const route = useRoute();
    const deviceList = ref([]);
    const selectedDevice = ref(null);
    const deviceListDisabled = ref(false);
    const categoryList = ref([]);
    const portListDisabled = ref(false);

    const {
      entity: peripheral,
      saving,
      createEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Peripheral',
      entityPath: '/admin/peripherals',
      createMutation: PERIPHERAL_CREATE,
      createMutationKey: 'peripheralCreate',
      createVariableName: 'devicePeripheral',
      excludeFields: ['__typename'],
      initialData: {
        name: '',
        description: '',
        model: '',
        category: null,
        connectedTo: []
      },
      transformBeforeSave: (data) => {
        const transformed = {...data};
        // Clean category - only send ID
        if (transformed.category) {
          if (transformed.category.id) {
            transformed.category = { id: transformed.category.id };
          } else {
            delete transformed.category;
          }
        }
        // Clean connectedTo ports - only send IDs
        if (transformed.connectedTo && Array.isArray(transformed.connectedTo)) {
          transformed.connectedTo = transformed.connectedTo
            .filter(port => port && port.id)
            .map(port => ({ id: port.id }));
        }
        return transformed;
      }
    });

    const fetchData = () => {
      client.query({
        query: PERIPHERAL_CATEGORIES,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        categoryList.value = response.data.peripheralCategoryList;
      });
      
      client.query({
        query: DEVICE_LIST_ALL_WITH_PORTS,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        deviceList.value = response.data.deviceList;
        if (route.query.deviceId != null) {
          selectedDevice.value = _.find(response.data.deviceList, function (o) {
            return o.id == route.query.deviceId;
          });
          deviceListDisabled.value = true;
          if (route.query.portId != null) {
            portListDisabled.value = true;
          }
        }
      });
    };

    const onSave = async () => {
      if (saving.value) return;
      if (!validateRequired(peripheral.value, ['name', 'description'])) return;
      await createEntity();
    };

    onMounted(() => {
      fetchData();
    });

    return {
      selectedDevice,
      deviceList,
      deviceListDisabled,
      peripheral,
      categoryList,
      portListDisabled,
      saving,
      onSave
    };
  }
});

</script>
