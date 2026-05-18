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

        <!-- Network Address -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">
            <q-icon name="mdi-ip-network" class="q-mr-xs"/>
            Network Address
          </div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <div class="row q-col-gutter-md items-center">
            <div class="col-12 col-md-5">
              <q-input
                v-model="ipModel"
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
                v-model="portModel"
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
                v-model="gatewayModel"
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
          <div>
            <DeviceIpUpdater
              :device="device"
              button-raised
              :dense="false"
              label="Update IP from device"
              @updated="onIpUpdated"
            />
          </div>
        </q-card-section>

        <q-separator/>

        <!-- Authentication Accounts -->
        <q-card-section>
          <div class="row items-center q-mb-sm">
            <div class="text-subtitle2 text-weight-medium">
              <q-icon name="mdi-key" class="q-mr-xs"/>
              Authentication Accounts
            </div>
            <q-space/>
            <q-btn
              flat
              dense
              no-caps
              color="primary"
              icon="mdi-plus"
              label="Add account"
              @click="addAuthAccount"
            />
          </div>
        </q-card-section>

        <q-card-section class="q-gutter-sm">
          <div v-if="!device.authAccounts || device.authAccounts.length === 0" class="text-grey-6 text-caption">
            No accounts configured. Add one if the controller requires HTTP authentication.
          </div>
          <q-card
            v-for="(acc, idx) in device.authAccounts"
            :key="acc.id ?? `new-${idx}`"
            flat
            bordered
            class="q-pa-sm"
          >
            <div class="row q-col-gutter-sm items-center">
              <div class="col-12 col-md-4">
                <q-input
                  v-model="acc.username"
                  label="Username"
                  filled
                  dense
                  clearable
                  clear-icon="close"
                />
              </div>
              <div class="col-12 col-md-4">
                <q-input
                  v-model="acc.password"
                  :type="acc._show ? 'text' : 'password'"
                  label="Password"
                  filled
                  dense
                  clearable
                  clear-icon="close"
                >
                  <template v-slot:append>
                    <q-icon
                      :name="acc._show ? 'mdi-eye-off' : 'mdi-eye'"
                      class="cursor-pointer"
                      @click="acc._show = !acc._show"
                    />
                  </template>
                </q-input>
              </div>
              <div class="col-6 col-md-2">
                <q-toggle
                  :model-value="!!acc.isDefault"
                  @update:model-value="onSetDefaultAccount(idx, $event)"
                  label="Default"
                  color="positive"
                  dense
                />
              </div>
              <div class="col-6 col-md-2 text-right">
                <q-btn
                  flat
                  dense
                  round
                  icon="mdi-delete"
                  color="negative"
                  @click="removeAuthAccount(idx)"
                >
                  <q-tooltip>Remove account</q-tooltip>
                </q-btn>
              </div>
            </div>
          </q-card>
        </q-card-section>

        <q-separator/>

        <!-- Navimow (Segway) OAuth — only visible for mower devices -->
        <template v-if="isNavimow">
          <q-card-section>
            <div class="text-subtitle2 text-weight-medium q-mb-sm">
              <q-icon name="mdi-key-link" class="q-mr-xs"/>
              Navimow Account
            </div>
            <div class="text-caption text-grey-7 q-mb-sm">
              Sign in with your Segway account to grant myHAB an access token. The token is stored
              in the device's <code>cfg.key.device.oauth.access_token</code> configuration row and
              auto-refreshes the base URL the first time. Re-authorise whenever the token expires
              (typically every 1&ndash;2 days &mdash; Navimow's token endpoint does not currently
              issue a refresh token).
            </div>
            <q-btn
              color="indigo-7"
              icon="mdi-login-variant"
              label="Connect Navimow account"
              :loading="navimowAuthorizing"
              :disable="navimowAuthorizing"
              @click="onNavimowAuthorize"
              no-caps
              unelevated
            />
          </q-card-section>

          <q-separator/>
        </template>

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
import {defineComponent, onMounted, ref, computed} from 'vue';
import {useRoute} from "vue-router";
import {useApolloClient} from "@vue/apollo-composable";
import {useQuasar} from 'quasar';
import {useEntityCRUD} from '@/composables';
import EntityInfoPanel from '@/components/EntityInfoPanel.vue';
import EntityFormActions from '@/components/EntityFormActions.vue';
import LocationSelector from '@/components/selectors/LocationSelector.vue';
import DeviceIpUpdater from '@/components/DeviceIpUpdater.vue';
import {
  DEVICE_BACKUP_CONFIG,
  DEVICE_BACKUP_LIST,
  DEVICE_CATEGORIES_LIST,
  DEVICE_GET_BY_ID_CHILDS,
  DEVICE_MODEL_LIST,
  DEVICE_RESTORE_TO_CONTROLLER,
  DEVICE_SYNC_FROM_BACKUP,
  DEVICE_UPDATE_CUSTOM,
  NAVIMOW_OAUTH_START,
  RACK_LIST_ALL
} from '@/graphql/queries';

export default defineComponent({
  name: 'DeviceEdit',
  components: {
    EntityInfoPanel,
    EntityFormActions,
    LocationSelector,
    DeviceIpUpdater
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

    // Navimow OAuth state
    const navimowAuthorizing = ref(false);
    let navimowMessageListener = null;

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
      excludeFields: ['__typename', 'ports', 'tsCreated', 'tsUpdated'],
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
        // Normalize networkAddress: drop the wrapper if all fields are blank,
        // strip __typename. The embedded NetworkAddress sub-record on the backend
        // accepts ip / gateway / port; we mirror that shape.
        if (transformed.networkAddress) {
          const na = transformed.networkAddress;
          const ip = (na.ip || '').trim();
          const gateway = (na.gateway || '').trim();
          const port = (na.port || '').toString().trim();
          if (!ip && !gateway && !port) {
            transformed.networkAddress = null;
          } else {
            transformed.networkAddress = {
              ip: ip || null,
              gateway: gateway || null,
              port: port || null,
            };
          }
        }
        // Normalize authAccounts: keep id when present (server uses it to update),
        // strip transient UI flags (_show, __typename). Empty list is allowed and
        // means "delete all accounts".
        if (transformed.authAccounts !== undefined) {
          if (!Array.isArray(transformed.authAccounts)) {
            transformed.authAccounts = [];
          }
          transformed.authAccounts = transformed.authAccounts.map(acc => {
            const out = {
              username: acc.username || null,
              password: acc.password || null,
              isDefault: !!acc.isDefault,
            };
            if (acc.id) out.id = acc.id;
            return out;
          });
        }
        return transformed;
      }
    });

    // Proxies so the IP/port/gateway inputs always have a backing object to bind to,
    // even when the device row arrived from the server with networkAddress == null.
    const ensureNetworkAddress = () => {
      if (!device.value.networkAddress) {
        device.value.networkAddress = { ip: '', gateway: '', port: '' };
      }
      return device.value.networkAddress;
    };
    const ipModel = computed({
      get: () => device.value?.networkAddress?.ip || '',
      set: (v) => { ensureNetworkAddress().ip = v; },
    });
    const portModel = computed({
      get: () => device.value?.networkAddress?.port || '',
      set: (v) => { ensureNetworkAddress().port = v; },
    });
    const gatewayModel = computed({
      get: () => device.value?.networkAddress?.gateway || '',
      set: (v) => { ensureNetworkAddress().gateway = v; },
    });

    /** Add a blank auth account row. */
    const addAuthAccount = () => {
      if (!Array.isArray(device.value.authAccounts)) {
        device.value.authAccounts = [];
      }
      device.value.authAccounts.push({
        username: '',
        password: '',
        isDefault: device.value.authAccounts.length === 0,
        _show: false,
      });
    };

    /** Remove an auth account row by index. */
    const removeAuthAccount = (idx) => {
      device.value.authAccounts.splice(idx, 1);
      // Ensure there's still at least one default if any remain.
      if (device.value.authAccounts.length > 0 && !device.value.authAccounts.some(a => a.isDefault)) {
        device.value.authAccounts[0].isDefault = true;
      }
    };

    /** Setting one account as default demotes the others (single-default invariant). */
    const onSetDefaultAccount = (idx, value) => {
      device.value.authAccounts.forEach((acc, i) => {
        acc.isDefault = value && i === idx;
      });
    };

    /** Refetch after the IpUpdater modal applies a new IP. */
    const onIpUpdated = async () => {
      await fetchEntity();
    };

    /**
     * True when the current device is a Segway Navimow mower — used to gate the
     * "Connect Navimow account" section so it doesn't clutter unrelated device
     * edit forms.
     */
    const isNavimow = computed(() => device.value?.model === 'NAVIMOW_SEGWAY');

    /**
     * Kick off the Navimow OAuth flow:
     *   1. Ask the backend for the authorize URL (includes our redirect_uri).
     *   2. Open it in a popup so the user can sign in to their Segway account.
     *   3. Listen for the postMessage the callback page emits when done, then
     *      refresh the device so the new token Configuration row shows up.
     */
    const onNavimowAuthorize = async () => {
      if (navimowAuthorizing.value) return;
      navimowAuthorizing.value = true;

      // Tear down any stale listener from a previous attempt.
      if (navimowMessageListener) {
        window.removeEventListener('message', navimowMessageListener);
        navimowMessageListener = null;
      }

      try {
        const { data } = await client.mutate({
          mutation: NAVIMOW_OAUTH_START,
          variables: { deviceId: device.value.id, callerOrigin: window.location.origin },
          fetchPolicy: 'no-cache',
        });
        const result = data?.navimowOAuthStart;
        if (!result?.success || !result.authorizeUrl) {
          $q.notify({ type: 'negative', message: `Could not start OAuth: ${result?.error || 'unknown error'}`, icon: 'mdi-alert-circle' });
          return;
        }

        // Listen for the success message before opening the popup so we don't
        // race a fast callback.
        navimowMessageListener = (ev) => {
          if (ev.origin !== window.location.origin) return;
          if (ev.data?.type !== 'navimow-oauth') return;
          if (ev.data.success) {
            $q.notify({ type: 'positive', message: 'Navimow account connected — token saved.', icon: 'mdi-check-circle' });
            fetchEntity();
          } else {
            $q.notify({ type: 'negative', message: ev.data.error || 'OAuth callback failed', icon: 'mdi-alert-circle', timeout: 8000 });
          }
          window.removeEventListener('message', navimowMessageListener);
          navimowMessageListener = null;
          navimowAuthorizing.value = false;
        };
        window.addEventListener('message', navimowMessageListener);

        const popup = window.open(result.authorizeUrl, 'navimow-oauth', 'width=540,height=720');
        if (!popup) {
          $q.notify({ type: 'warning', message: 'Popup blocked. Allow popups for this site and retry.', icon: 'mdi-alert' });
          window.removeEventListener('message', navimowMessageListener);
          navimowMessageListener = null;
          return;
        }
        // Safety: if the user closes the popup without completing, release the spinner.
        const watcher = setInterval(() => {
          if (popup.closed) {
            clearInterval(watcher);
            if (navimowMessageListener) {
              // Give the postMessage a beat to land first.
              setTimeout(() => { navimowAuthorizing.value = false; }, 500);
            }
          }
        }, 1000);
      } catch (err) {
        $q.notify({ type: 'negative', message: `OAuth start failed: ${err.message}`, icon: 'mdi-alert-circle' });
      } finally {
        // Spinner cleared by the message listener / popup-watcher in the happy path.
        // Only clear here if no listener was registered.
        if (!navimowMessageListener) navimowAuthorizing.value = false;
      }
    };

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
      // Network address proxies + IP update flow
      ipModel,
      portModel,
      gatewayModel,
      onIpUpdated,
      // Auth accounts
      addAuthAccount,
      removeAuthAccount,
      onSetDefaultAccount,
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
      // Navimow OAuth
      isNavimow,
      navimowAuthorizing,
      onNavimowAuthorize,
      formatDate
    };
  }
});

</script>
