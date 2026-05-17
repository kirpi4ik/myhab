<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="device">
      <q-card flat bordered>
        <q-card-section>
          <div class="row items-center justify-between">
            <div class="text-h5">
              <q-icon name="mdi-plus-circle" color="primary" size="sm" class="q-mr-sm"/>
              Register New Device
            </div>
            <q-btn
              color="primary"
              icon="mdi-radar"
              label="Discover Devices"
              @click="onOpenDiscover"
              no-caps
              unelevated
            />
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

        <!-- Network Address -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">
            <q-icon name="mdi-ip-network" class="q-mr-xs"/>
            Network Address
          </div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <div class="row q-col-gutter-md">
            <div class="col-12 col-md-5">
              <q-input
                v-model="netIp"
                label="IP"
                hint="IPv4 address used to reach the controller"
                clearable
                clear-icon="close"
                color="orange"
                filled
                dense
              >
                <template v-slot:prepend>
                  <q-icon name="mdi-ip"/>
                </template>
              </q-input>
            </div>
            <div class="col-12 col-md-3">
              <q-input
                v-model="netPort"
                label="Port"
                hint="HTTP port (default 80)"
                clearable
                clear-icon="close"
                color="orange"
                filled
                dense
              >
                <template v-slot:prepend>
                  <q-icon name="mdi-numeric"/>
                </template>
              </q-input>
            </div>
            <div class="col-12 col-md-4">
              <q-input
                v-model="netGateway"
                label="Gateway"
                hint="Optional default gateway"
                clearable
                clear-icon="close"
                color="orange"
                filled
                dense
              >
                <template v-slot:prepend>
                  <q-icon name="mdi-router-network"/>
                </template>
              </q-input>
            </div>
          </div>
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

    <!-- Discover Devices Dialog -->
    <q-dialog v-model="discoverDialogVisible" persistent transition-show="jump-up" transition-hide="jump-down">
      <q-card style="min-width: 550px; max-width: 650px;">
        <q-bar class="bg-primary text-white">
          <q-icon name="mdi-radar" size="20px"/>
          <div class="q-ml-sm text-weight-bold">Discover Devices</div>
          <q-space/>
          <q-btn dense flat icon="close" @click="discoverDialogVisible = false" :disable="discovering || initializing"/>
        </q-bar>

        <!-- Scanning State -->
        <q-card-section v-if="discovering" class="text-center q-pa-lg">
          <q-spinner-radio size="60px" color="primary"/>
          <div class="q-mt-md text-body1">Scanning network for devices...</div>
          <div class="text-caption text-grey-6 q-mt-xs">Broadcasting UDP discovery on port 52000</div>
          <q-linear-progress indeterminate color="primary" class="q-mt-lg"/>
        </q-card-section>

        <!-- Results -->
        <template v-else>
          <q-card-section v-if="discoveredDevices.length === 0 && !discoverError" class="text-center q-pa-lg">
            <q-icon name="mdi-access-point-network-off" size="48px" color="grey-5"/>
            <div class="q-mt-sm text-grey-7">No devices found on the network.</div>
            <div class="text-caption text-grey-5">Make sure devices are powered on and connected to the same network.</div>
            <q-btn
              flat
              color="primary"
              icon="mdi-refresh"
              label="Scan Again"
              class="q-mt-md"
              @click="startDiscovery"
              no-caps
            />
          </q-card-section>

          <q-card-section v-else-if="discoverError" class="text-center q-pa-lg">
            <q-icon name="mdi-alert-circle" size="48px" color="negative"/>
            <div class="q-mt-sm text-negative">{{ discoverError }}</div>
            <q-btn
              flat
              color="primary"
              icon="mdi-refresh"
              label="Retry"
              class="q-mt-md"
              @click="startDiscovery"
              no-caps
            />
          </q-card-section>

          <template v-else>
            <q-card-section class="q-pa-md q-pb-none">
              <div class="text-body2 text-grey-7">Found {{ discoveredDevices.length }} device(s):</div>
            </q-card-section>

            <q-card-section class="q-pa-md">
              <q-list bordered separator class="rounded-borders">
                <q-item
                  v-for="(dev, index) in discoveredDevices"
                  :key="index"
                >
                  <q-item-section avatar>
                    <q-icon
                      :name="dev.bootloaderMode ? 'mdi-chip' : 'mdi-developer-board'"
                      :color="dev.bootloaderMode ? 'orange' : 'teal'"
                      size="sm"
                    />
                  </q-item-section>
                  <q-item-section>
                    <q-item-label class="text-weight-medium">{{ dev.ip }}</q-item-label>
                    <q-item-label caption>
                      <q-badge
                        :color="dev.bootloaderMode ? 'orange' : 'teal'"
                        :label="dev.bootloaderMode ? 'Bootloader' : 'MegaD'"
                        class="q-mr-xs"
                      />
                      <span v-if="dev.mqttId" class="text-grey-7">MQTT ID: {{ dev.mqttId }}</span>
                    </q-item-label>
                  </q-item-section>
                  <q-item-section side>
                    <q-btn
                      v-if="!dev.bootloaderMode"
                      flat
                      dense
                      color="primary"
                      icon="mdi-download-network"
                      label="Initialize"
                      :loading="initializing && initializingIp === dev.ip"
                      :disable="initializing"
                      @click="onInitFromDevice(dev)"
                      no-caps
                    />
                    <q-badge v-else color="orange-3" text-color="orange-9" label="In bootloader mode"/>
                  </q-item-section>
                </q-item>
              </q-list>
            </q-card-section>

            <!-- Initialize form (shown after clicking Initialize) -->
            <q-card-section v-if="initFormVisible" class="q-pt-none">
              <q-separator class="q-mb-md"/>
              <div class="text-subtitle2 text-weight-medium q-mb-sm">
                Initialize from {{ initializingIp }}
              </div>
              <q-input
                v-model="initPassword"
                label="Device Password"
                hint="Password for HTTP access (default: sec)"
                filled
                dense
                class="q-mb-md"
              >
                <template v-slot:prepend>
                  <q-icon name="mdi-key"/>
                </template>
              </q-input>
              <div class="row q-gutter-sm">
                <q-btn
                  unelevated
                  color="primary"
                  icon="mdi-download-network"
                  label="Read & Create Device"
                  :loading="initializing"
                  :disable="initializing"
                  @click="doInitFromDevice"
                  no-caps
                />
                <q-btn
                  flat
                  color="grey-7"
                  label="Cancel"
                  @click="initFormVisible = false"
                  :disable="initializing"
                  no-caps
                />
              </div>
            </q-card-section>
          </template>
        </template>

        <!-- Available actions / help -->
        <q-card-section v-if="!discovering" class="q-pa-md q-pt-none">
          <q-banner dense rounded class="bg-blue-1 text-blue-10">
            <template v-slot:avatar>
              <q-icon name="mdi-information-outline" color="blue-10"/>
            </template>
            <div class="text-body2 text-weight-medium q-mb-xs">Available actions</div>
            <ul class="q-my-none q-pl-md text-caption">
              <li>
                <strong>Initialize</strong> &mdash; click next to a discovered controller to read
                its full configuration over HTTP and create a Device record in the database.
                Enter the device password (default <code>sec</code>) when prompted.
              </li>
              <li>
                <strong>Bootloader mode</strong> &mdash; the controller is waiting for a firmware
                upload and cannot be initialized until it boots into normal mode.
              </li>
              <li>
                <strong>Scan Again</strong> &mdash; re-broadcast the UDP discovery packet
                (port&nbsp;52000) to refresh the list.
              </li>
            </ul>
          </q-banner>
        </q-card-section>

        <q-card-actions v-if="!discovering" align="right" class="q-pa-md">
          <q-btn
            flat
            label="Scan Again"
            color="primary"
            icon="mdi-refresh"
            @click="startDiscovery"
            :disable="initializing"
            no-caps
          />
          <q-btn flat label="Close" color="grey-7" @click="discoverDialogVisible = false" :disable="initializing"/>
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref, computed} from 'vue';
import {useRouter} from 'vue-router';
import {useApolloClient} from "@vue/apollo-composable";
import {useQuasar} from 'quasar';
import {useEntityCRUD} from '@/composables';
import EntityFormActions from '@/components/EntityFormActions.vue';
import {
  DEVICE_CATEGORIES_LIST,
  DEVICE_CREATE,
  DEVICE_INIT_FROM_CONTROLLER,
  DEVICE_MODEL_LIST,
  DEVICES_DISCOVER,
  RACK_LIST_ALL
} from '@/graphql/queries';

export default defineComponent({
  name: 'DeviceNew',
  components: {
    EntityFormActions
  },
  setup() {
    const {client} = useApolloClient();
    const $q = useQuasar();
    const router = useRouter();

    const rackList = ref([]);
    const typeList = ref([]);
    const modelList = ref([]);

    // Discovery state
    const discoverDialogVisible = ref(false);
    const discovering = ref(false);
    const discoveredDevices = ref([]);
    const discoverError = ref(null);

    // Init from device state
    const initializing = ref(false);
    const initializingIp = ref('');
    const initFormVisible = ref(false);
    const initPassword = ref('sec');

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
        rack: null,
        networkAddress: { ip: '', gateway: '', port: '' }
      },
      transformBeforeSave: (data) => {
        const transformed = {...data};
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
        // Drop networkAddress when all sub-fields are blank; otherwise normalize
        // to plain { ip, gateway, port }.
        if (transformed.networkAddress) {
          const na = transformed.networkAddress;
          const ip = (na.ip || '').trim();
          const gateway = (na.gateway || '').trim();
          const port = (na.port || '').toString().trim();
          if (!ip && !gateway && !port) {
            delete transformed.networkAddress;
          } else {
            transformed.networkAddress = {
              ip: ip || null,
              gateway: gateway || null,
              port: port || null,
            };
          }
        }
        return transformed;
      }
    });

    // Bindings for the Network Address inputs (lazily create the sub-object).
    const ensureNetworkAddress = () => {
      if (!device.value.networkAddress) {
        device.value.networkAddress = { ip: '', gateway: '', port: '' };
      }
      return device.value.networkAddress;
    };
    const netIp = computed({
      get: () => device.value?.networkAddress?.ip || '',
      set: (v) => { ensureNetworkAddress().ip = v; },
    });
    const netPort = computed({
      get: () => device.value?.networkAddress?.port || '',
      set: (v) => { ensureNetworkAddress().port = v; },
    });
    const netGateway = computed({
      get: () => device.value?.networkAddress?.gateway || '',
      set: (v) => { ensureNetworkAddress().gateway = v; },
    });

    const fetchData = () => {
      client.query({
        query: RACK_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        rackList.value = response.data.rackList || [];
      }).catch(error => {
        console.error('Error fetching racks:', error);
      });

      client.query({
        query: DEVICE_CATEGORIES_LIST,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        typeList.value = response.data.deviceCategoryList || [];
      }).catch(error => {
        console.error('Error fetching device categories:', error);
      });

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

    const onSave = async () => {
      if (saving.value) return;
      if (!validateRequired(device.value, ['code', 'name', 'description'])) return;
      await createEntity();
    };

    // --- Discovery ---

    const onOpenDiscover = () => {
      discoverDialogVisible.value = true;
      initFormVisible.value = false;
      startDiscovery();
    };

    const startDiscovery = async () => {
      discovering.value = true;
      discoverError.value = null;
      discoveredDevices.value = [];
      initFormVisible.value = false;
      try {
        const { data } = await client.query({
          query: DEVICES_DISCOVER,
          fetchPolicy: 'no-cache'
        });
        discoveredDevices.value = data?.discoveredDevices || [];
      } catch (error) {
        discoverError.value = error.message;
      } finally {
        discovering.value = false;
      }
    };

    const onInitFromDevice = (dev) => {
      initializingIp.value = dev.ip;
      initPassword.value = 'sec';
      initFormVisible.value = true;
    };

    const doInitFromDevice = async () => {
      initializing.value = true;
      try {
        const { data } = await client.mutate({
          mutation: DEVICE_INIT_FROM_CONTROLLER,
          variables: { ip: initializingIp.value, password: initPassword.value },
          fetchPolicy: 'no-cache'
        });
        const result = data?.deviceInitFromController;

        if (!result?.success) {
          // Code-already-exists → offer to open the existing device.
          if (result?.existingDeviceId) {
            const existingId = result.existingDeviceId;
            const existingCode = result.existingDeviceCode;
            $q.notify({
              type: 'warning',
              message: `Device "${existingCode}" is already registered. Open it to backup or update its config.`,
              icon: 'mdi-database-alert',
              timeout: 8000,
              actions: [
                {
                  label: 'Open',
                  color: 'white',
                  handler: () => {
                    discoverDialogVisible.value = false;
                    router.push(`/admin/devices/${existingId}/edit`);
                  }
                },
                { label: 'Dismiss', color: 'white' }
              ]
            });
          } else {
            $q.notify({
              type: 'negative',
              message: `Initialization failed: ${result?.error || 'unknown error'}`,
              icon: 'mdi-alert-circle'
            });
          }
          return;
        }

        discoverDialogVisible.value = false;
        $q.notify({
          type: 'positive',
          message: `Device "${result.deviceCode}" created with ${result.portCount} ports`,
          icon: 'mdi-check-circle'
        });
        router.push(`/admin/devices/${result.deviceId}/edit`);
      } catch (error) {
        $q.notify({
          type: 'negative',
          message: `Initialization failed: ${error.message}`,
          icon: 'mdi-alert-circle'
        });
      } finally {
        initializing.value = false;
      }
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
      saving,
      // Network address bindings
      netIp,
      netPort,
      netGateway,
      // Discovery
      discoverDialogVisible,
      discovering,
      discoveredDevices,
      discoverError,
      onOpenDiscover,
      startDiscovery,
      // Init from device
      initializing,
      initializingIp,
      initFormVisible,
      initPassword,
      onInitFromDevice,
      doInitFromDevice
    };
  }
});

</script>
