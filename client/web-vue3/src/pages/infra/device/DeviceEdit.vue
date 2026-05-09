<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="!loading && device">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-pencil" color="primary" size="sm" class="q-mr-sm"/>
            Edit Device
          </div>
          <div class="text-subtitle2 text-weight-medium">
            {{ device.code }}
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

          <LocationSelector
            v-model="device.zones"
            label="Zones"
            hint="Select zones where this device is located"
          />
        </q-card-section>

        <q-separator/>

        <!-- Configuration Management -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">
            <q-icon name="mdi-cog-transfer" class="q-mr-xs"/>
            Configuration Management
          </div>
        </q-card-section>

        <q-card-section>
          <div class="row q-gutter-sm">
            <q-btn
              color="teal"
              icon="mdi-content-save-cog"
              label="Backup Config"
              :loading="backingUp"
              :disable="backingUp || restoring"
              @click="onBackupConfig"
              no-caps
              unelevated
            />
            <q-btn
              color="deep-orange"
              icon="mdi-backup-restore"
              label="Restore Config"
              :disable="backingUp || restoring"
              @click="onShowRestoreDialog"
              no-caps
              outline
            />
          </div>
        </q-card-section>

        <q-separator/>

        <!-- Information Panel -->
        <EntityInfoPanel
          :entity="device"
          icon="mdi-devices"
          :extra-info="[
            { icon: 'mdi-ethernet', label: 'Ports', value: device.ports?.length || 0 }
          ]"
        />

        <q-separator/>

        <!-- Actions -->
        <EntityFormActions
          :saving="saving"
          :show-view="true"
          :view-route="`/admin/devices/${$route.params.idPrimary}/view`"
          save-label="Save Device"
        />
      </q-card>
    </form>

    <!-- Loading State -->
    <q-inner-loading :showing="loading">
      <q-spinner-gears size="50px" color="primary"/>
    </q-inner-loading>

    <!-- Restore Dialog -->
    <q-dialog v-model="restoreDialogVisible" persistent transition-show="jump-up" transition-hide="jump-down">
      <q-card style="min-width: 560px; max-width: 660px;">
        <q-bar class="bg-deep-orange text-white">
          <q-icon name="mdi-backup-restore" size="20px"/>
          <div class="q-ml-sm text-weight-bold">Restore from Backup</div>
          <q-space/>
          <q-btn dense flat icon="close" @click="restoreDialogVisible = false" :disable="restoring"/>
        </q-bar>

        <q-card-section v-if="loadingBackups" class="text-center q-pa-lg">
          <q-spinner-dots size="40px" color="primary"/>
          <div class="q-mt-sm text-grey-7">Loading backups...</div>
        </q-card-section>

        <q-card-section v-else-if="backupsList.length === 0" class="text-center q-pa-lg">
          <q-icon name="mdi-database-off" size="48px" color="grey-5"/>
          <div class="q-mt-sm text-grey-7">No backups available for this device.</div>
          <div class="text-caption text-grey-5">Create a backup first using the "Backup Config" button.</div>
        </q-card-section>

        <template v-else>
          <!-- Step 1: Select backup -->
          <q-card-section class="q-pa-md">
            <div class="text-body2 text-grey-7 q-mb-sm">Select a backup:</div>
            <q-list bordered separator class="rounded-borders">
              <q-item
                v-for="backup in backupsList"
                :key="backup.id"
                clickable
                v-ripple
                :active="selectedBackupId === backup.id"
                active-class="bg-teal-1"
                @click="selectedBackupId = backup.id"
              >
                <q-item-section avatar>
                  <q-icon
                    :name="selectedBackupId === backup.id ? 'mdi-radiobox-marked' : 'mdi-radiobox-blank'"
                    :color="selectedBackupId === backup.id ? 'teal' : 'grey-5'"
                  />
                </q-item-section>
                <q-item-section>
                  <q-item-label>Firmware: {{ backup.frmVersion || 'Unknown' }}</q-item-label>
                  <q-item-label caption>{{ backup.configLines }} config lines</q-item-label>
                </q-item-section>
                <q-item-section side>
                  <q-item-label caption>{{ formatDate(backup.tsCreated) }}</q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </q-card-section>

          <!-- Step 2: Choose restore target -->
          <q-card-section v-if="selectedBackupId" class="q-pt-none">
            <q-separator class="q-mb-md"/>
            <div class="text-body2 text-weight-medium q-mb-sm">Choose what to restore:</div>

            <div class="q-gutter-sm">
              <!-- Push to Controller -->
              <q-card flat bordered class="q-pa-sm">
                <div class="row items-center no-wrap q-gutter-sm">
                  <q-icon name="mdi-upload-network" size="28px" color="deep-orange" class="q-ml-sm"/>
                  <div class="col">
                    <div class="text-body2 text-weight-medium">Push to Controller</div>
                    <div class="text-caption text-grey-6">
                      Write configuration to the physical device via HTTP and restart it.
                      Does not change the database.
                    </div>
                  </div>
                  <q-btn
                    unelevated
                    color="deep-orange"
                    label="Push"
                    icon="mdi-upload-network"
                    :loading="restoring && restoreTarget === 'controller'"
                    :disable="restoring"
                    @click="onPushToController"
                    no-caps
                    dense
                    class="q-px-md"
                  />
                </div>
              </q-card>

              <!-- Sync to Database -->
              <q-card flat bordered class="q-pa-sm">
                <div class="row items-center no-wrap q-gutter-sm">
                  <q-icon name="mdi-database-sync" size="28px" color="primary" class="q-ml-sm"/>
                  <div class="col">
                    <div class="text-body2 text-weight-medium">Sync to Database</div>
                    <div class="text-caption text-grey-6">
                      Update device settings and ports in the database from the backup.
                      Does not touch the physical controller.
                    </div>
                  </div>
                  <q-btn
                    unelevated
                    color="primary"
                    label="Sync"
                    icon="mdi-database-sync"
                    :loading="restoring && restoreTarget === 'database'"
                    :disable="restoring"
                    @click="onSyncFromBackup"
                    no-caps
                    dense
                    class="q-px-md"
                  />
                </div>
              </q-card>
            </div>
          </q-card-section>
        </template>

        <q-card-actions align="right" class="q-pa-md">
          <q-btn flat label="Close" color="grey-7" @click="restoreDialogVisible = false" :disable="restoring"/>
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from 'vue';
import {useRoute} from "vue-router";
import {useApolloClient} from "@vue/apollo-composable";
import {useQuasar} from 'quasar';
import {useEntityCRUD} from '@/composables';
import EntityInfoPanel from '@/components/EntityInfoPanel.vue';
import EntityFormActions from '@/components/EntityFormActions.vue';
import LocationSelector from '@/components/selectors/LocationSelector.vue';
import {
  DEVICE_BACKUP_CONFIG,
  DEVICE_BACKUP_LIST,
  DEVICE_CATEGORIES_LIST,
  DEVICE_GET_BY_ID_CHILDS,
  DEVICE_MODEL_LIST,
  DEVICE_RESTORE_TO_CONTROLLER,
  DEVICE_SYNC_FROM_BACKUP,
  DEVICE_UPDATE_CUSTOM,
  RACK_LIST_ALL
} from '@/graphql/queries';

export default defineComponent({
  name: 'DeviceEdit',
  components: {
    EntityInfoPanel,
    EntityFormActions,
    LocationSelector
  },
  setup() {
    const route = useRoute();
    const {client} = useApolloClient();
    const $q = useQuasar();

    // Additional data lists
    const rackList = ref([]);
    const typeList = ref([]);
    const modelList = ref([]);

    // Backup/Restore state
    const backingUp = ref(false);
    const restoring = ref(false);
    const restoreDialogVisible = ref(false);
    const loadingBackups = ref(false);
    const backupsList = ref([]);
    const selectedBackupId = ref(null);
    const restoreTarget = ref(null);

    // Use CRUD composable
    const {
      entity: device,
      loading,
      saving,
      fetchEntity,
      updateEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Device',
      entityPath: '/admin/devices',
      getQuery: DEVICE_GET_BY_ID_CHILDS,
      getQueryKey: 'device',
      updateMutation: DEVICE_UPDATE_CUSTOM,
      updateMutationKey: 'deviceUpdateCustom',
      updateVariableName: 'device',
      excludeFields: ['__typename', 'ports', 'authAccounts', 'tsCreated', 'tsUpdated'],
      transformBeforeSave: (data) => {
        const transformed = {...data};
        if (transformed.rack) {
          transformed.rack = transformed.rack.id ? { id: transformed.rack.id } : null;
        }
        if (transformed.type) {
          transformed.type = transformed.type.id ? { id: transformed.type.id } : null;
        }
        if (transformed.zones && Array.isArray(transformed.zones)) {
          transformed.zones = transformed.zones
            .filter(zone => zone && zone.id)
            .map(zone => ({ id: zone.id }));
        }
        return transformed;
      }
    });

    const fetchData = async () => {
      const response = await fetchEntity();
      if (response) {
        Promise.all([
          client.query({ query: RACK_LIST_ALL, fetchPolicy: 'network-only' }),
          client.query({ query: DEVICE_CATEGORIES_LIST, fetchPolicy: 'network-only' }),
          client.query({ query: DEVICE_MODEL_LIST, fetchPolicy: 'network-only' })
        ]).then(([racksResponse, categoriesResponse, modelsResponse]) => {
          rackList.value = racksResponse.data.rackList || [];
          typeList.value = categoriesResponse.data.deviceCategoryList || [];
          modelList.value = modelsResponse.data.deviceModelList || [];
        }).catch(error => {
          console.error('Error fetching related lists:', error);
        });
      }
    };

    const onSave = async () => {
      if (!validateRequired(device.value, ['code', 'name', 'description'])) return;
      await updateEntity();
    };

    const onBackupConfig = async () => {
      backingUp.value = true;
      try {
        const { data } = await client.mutate({
          mutation: DEVICE_BACKUP_CONFIG,
          variables: { deviceId: route.params.idPrimary },
          fetchPolicy: 'no-cache'
        });
        const result = data?.deviceBackupConfig;
        if (!result?.success) {
          $q.notify({
            type: 'negative',
            message: `Backup failed: ${result?.error || 'unknown error'}`,
            icon: 'mdi-alert-circle'
          });
          return;
        }
        $q.notify({
          type: 'positive',
          message: `Configuration backed up successfully (${result.configLines} lines, firmware: ${result.frmVersion})`,
          icon: 'mdi-content-save-cog'
        });
      } catch (error) {
        $q.notify({
          type: 'negative',
          message: `Backup failed: ${error.message}`,
          icon: 'mdi-alert-circle'
        });
      } finally {
        backingUp.value = false;
      }
    };

    const onShowRestoreDialog = async () => {
      restoreDialogVisible.value = true;
      loadingBackups.value = true;
      selectedBackupId.value = null;
      try {
        const { data } = await client.query({
          query: DEVICE_BACKUP_LIST,
          variables: { deviceId: route.params.idPrimary },
          fetchPolicy: 'no-cache'
        });
        backupsList.value = data?.deviceBackupList || [];
      } catch (error) {
        $q.notify({
          type: 'negative',
          message: `Failed to load backups: ${error.message}`,
          icon: 'mdi-alert-circle'
        });
        backupsList.value = [];
      } finally {
        loadingBackups.value = false;
      }
    };

    const onPushToController = async () => {
      restoring.value = true;
      restoreTarget.value = 'controller';
      try {
        const { data } = await client.mutate({
          mutation: DEVICE_RESTORE_TO_CONTROLLER,
          variables: {
            deviceId: route.params.idPrimary,
            backupId: selectedBackupId.value
          },
          fetchPolicy: 'no-cache'
        });
        const result = data?.deviceRestoreToController;
        if (!result?.success) {
          $q.notify({
            type: 'negative',
            message: `Push to controller failed: ${result?.error || 'unknown error'}`,
            icon: 'mdi-alert-circle'
          });
          return;
        }
        restoreDialogVisible.value = false;
        $q.notify({
          type: 'positive',
          message: 'Configuration pushed to controller. Device is restarting.',
          icon: 'mdi-upload-network'
        });
      } catch (error) {
        $q.notify({
          type: 'negative',
          message: `Push to controller failed: ${error.message}`,
          icon: 'mdi-alert-circle'
        });
      } finally {
        restoring.value = false;
        restoreTarget.value = null;
      }
    };

    const onSyncFromBackup = async () => {
      restoring.value = true;
      restoreTarget.value = 'database';
      try {
        const { data } = await client.mutate({
          mutation: DEVICE_SYNC_FROM_BACKUP,
          variables: {
            deviceId: route.params.idPrimary,
            backupId: selectedBackupId.value
          },
          fetchPolicy: 'no-cache'
        });
        const result = data?.deviceSyncFromBackup;
        if (!result?.success) {
          $q.notify({
            type: 'negative',
            message: `Sync from backup failed: ${result?.error || 'unknown error'}`,
            icon: 'mdi-alert-circle'
          });
          return;
        }
        restoreDialogVisible.value = false;
        $q.notify({
          type: 'positive',
          message: 'Database synced from backup. Device settings and ports updated.',
          icon: 'mdi-database-sync'
        });
        // Refresh the device data to reflect changes
        await fetchData();
      } catch (error) {
        $q.notify({
          type: 'negative',
          message: `Sync from backup failed: ${error.message}`,
          icon: 'mdi-alert-circle'
        });
      } finally {
        restoring.value = false;
        restoreTarget.value = null;
      }
    };

    const formatDate = (dateStr) => {
      if (!dateStr) return 'Unknown';
      try {
        return new Date(dateStr).toLocaleString();
      } catch {
        return dateStr;
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
      loading,
      saving,
      // Backup/Restore
      backingUp,
      restoring,
      restoreDialogVisible,
      loadingBackups,
      backupsList,
      selectedBackupId,
      restoreTarget,
      onBackupConfig,
      onShowRestoreDialog,
      onPushToController,
      onSyncFromBackup,
      formatDate
    };
  }
});

</script>
