<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-card-section>
        <q-avatar size="103px" class="absolute-center shadow-10" color="indigo-7">
          <q-icon name="mdi-devices" size="xl" color="white"/>
        </q-avatar>
        <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      </q-card-section>

      <q-card-section class="row items-center">
        <div class="column">
          <div class="row items-center q-gutter-sm">
            <div class="text-h4 text-secondary">{{ viewItem.code || 'Unnamed Device' }}</div>
            <q-badge 
              v-if="viewItem.status"
              :color="getStatusColor(viewItem.status)"
              :label="getStatusLabel(viewItem.status)"
              :icon="getStatusIcon(viewItem.status)"
              class="q-ml-sm"
            />
          </div>
          <div class="text-subtitle2 text-grey-7">{{ viewItem.name || 'No name specified' }}</div>
        </div>
        <q-space/>
        <q-btn
          v-if="adminUrl"
          outline
          round
          color="indigo-7"
          icon="mdi-open-in-new"
          type="a"
          :href="adminUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="q-mr-sm"
        >
          <q-tooltip>Open controller admin UI ({{ adminUrl }})</q-tooltip>
        </q-btn>
        <q-btn outline round color="amber-8" icon="mdi-pencil" :to="uri +'/'+ $route.params.idPrimary+'/edit'">
          <q-tooltip>Edit Device</q-tooltip>
        </q-btn>
        <q-btn outline round color="amber-8" icon="mdi-view-list"
               :to="'/admin/configurations/'+ $route.params.idPrimary+'?type=DEVICE'" class="q-ml-sm">
          <q-tooltip>View Configurations</q-tooltip>
        </q-btn>
      </q-card-section>

      <q-separator/>

      <!-- Basic Information -->
      <q-list>
        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-identifier" class="q-mr-sm"/>
              ID
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.id }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-barcode" class="q-mr-sm"/>
              Code
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge color="primary" :label="viewItem.code || 'No Code'"/>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-label" class="q-mr-sm"/>
              Name
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge color="secondary" :label="viewItem.name || 'Unnamed'"/>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.description">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-text" class="q-mr-sm"/>
              Description
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.description }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.model">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-cube-outline" class="q-mr-sm"/>
              Model
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.model }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.type">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-shape" class="q-mr-sm"/>
              Type
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge color="info" :label="viewItem.type.name"/>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.rack">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-server" class="q-mr-sm"/>
              Rack Location
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge color="secondary" :label="viewItem.rack.name"/>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-separator class="q-my-md"/>

        <!-- Network Address Section -->
        <q-item-label header class="text-h6 text-grey-8">
          <q-icon name="mdi-ip-network" class="q-mr-sm"/>
          Network
        </q-item-label>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">IP address</q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge v-if="viewItem.networkAddress?.ip" color="grey-7" :label="viewItem.networkAddress.ip"/>
              <span v-else class="text-grey-5">— not set —</span>
            </q-item-label>
          </q-item-section>
          <q-item-section side>
            <DeviceIpUpdater :device="viewItem" @updated="fetchData"/>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.networkAddress?.gateway">
          <q-item-section>
            <q-item-label class="text-h6">Gateway</q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.networkAddress.gateway }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.networkAddress?.port">
          <q-item-section>
            <q-item-label class="text-h6">Port</q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.networkAddress.port }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="adminUrl">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-open-in-new" class="q-mr-sm"/>
              Admin UI
            </q-item-label>
            <q-item-label caption class="text-body2">
              <a :href="adminUrl" target="_blank" rel="noopener noreferrer" class="text-primary">
                {{ adminUrl }}
              </a>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-separator class="q-my-md"/>

        <!-- Authentication Accounts Section -->
        <q-item-label header class="text-h6 text-grey-8">
          <q-icon name="mdi-key" class="q-mr-sm"/>
          Authentication Accounts
        </q-item-label>

        <q-item v-if="!viewItem.authAccounts || viewItem.authAccounts.length === 0">
          <q-item-section>
            <q-item-label caption class="text-grey-6">No accounts configured.</q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-for="acc in viewItem.authAccounts" :key="acc.id">
          <q-item-section>
            <q-item-label class="text-body1">
              <q-icon name="mdi-account" class="q-mr-xs"/>
              {{ acc.username || '— no username —' }}
              <q-badge v-if="acc.isDefault" color="positive" label="default" class="q-ml-sm"/>
            </q-item-label>
            <q-item-label caption class="text-body2 text-grey-7">
              Password: <code>{{ maskPassword(acc.password) }}</code>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-separator class="q-my-md"/>

        <!-- Timestamps Section -->
        <q-item-label header class="text-h6 text-grey-8">Timestamps</q-item-label>

        <q-item v-if="viewItem.tsCreated">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-calendar-plus" class="q-mr-sm"/>
              Created
            </q-item-label>
            <q-item-label caption class="text-body2">{{ formatDate(viewItem.tsCreated) }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.tsUpdated">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-calendar-edit" class="q-mr-sm"/>
              Last Updated
            </q-item-label>
            <q-item-label caption class="text-body2">{{ formatDate(viewItem.tsUpdated) }}</q-item-label>
          </q-item-section>
        </q-item>

      </q-list>

      <q-separator/>

      <!-- Connected Ports Table -->
      <div class="q-pa-md">
        <div class="row items-center q-mb-md">
          <div class="text-h6 text-grey-8">
            <q-icon name="mdi-ethernet" class="q-mr-sm"/>
            Connected Ports
          </div>
          <q-space/>
          <q-btn 
            icon="mdi-plus-circle" 
            color="positive" 
            label="Add Port" 
            @click="newPort"
            size="sm"
          >
            <q-tooltip>Add new port to this device</q-tooltip>
          </q-btn>
        </div>
        
        <q-table
          v-if="viewItem.ports && viewItem.ports.length > 0"
          :rows="viewItem.ports"
          :columns="portColumns"
          @row-click="onRowClick"
          v-model:pagination="pagination"
          row-key="id"
          flat
          bordered
        >
          <template v-slot:body="props">
            <q-tr :props="props" @click="onRowClick(props.row)" style="cursor: pointer;">
              <q-td key="id" style="max-width: 50px">
                {{ props.row.id }}
              </q-td>
              <q-td key="internalRef">
                <q-badge color="info" :label="props.row.internalRef || props.row.id"/>
              </q-td>
              <q-td key="name">
                {{ props.row.name || '-' }}
              </q-td>
              <q-td key="description">
                {{ props.row.description ? (props.row.description.length > 50 ? props.row.description.substring(0, 50) + '...' : props.row.description) : '-' }}
              </q-td>
              <q-td key="value">
                <q-badge v-if="props.row.value" color="primary" :label="props.row.value"/>
                <span v-else class="text-grey-6">-</span>
              </q-td>
              <q-td key="actions">
                <q-btn 
                  icon="mdi-delete" 
                  @click.stop="removeItem(props.row)" 
                  color="red-7" 
                  flat 
                  dense
                >
                  <q-tooltip>Delete Port</q-tooltip>
                </q-btn>
              </q-td>
            </q-tr>
          </template>
        </q-table>

        <div v-else class="text-center text-grey-6 q-pa-md">
          <q-icon name="mdi-ethernet-off" size="md"/>
          <div>No ports connected</div>
          <q-btn 
            icon="mdi-plus-circle" 
            color="positive" 
            label="Add Port" 
            @click="newPort"
            class="q-mt-md"
          />
        </div>
      </div>

      <q-separator/>

      <!-- Zones Table -->
      <div class="q-pa-md">
        <div class="text-h6 text-grey-8 q-mb-md">
          <q-icon name="mdi-map-marker-multiple" class="q-mr-sm"/>
          Located in Zones
        </div>
        
        <q-table
          v-if="viewItem.zones && viewItem.zones.length > 0"
          :rows="viewItem.zones"
          :columns="zoneColumns"
          @row-click="viewZone"
          v-model:pagination="pagination"
          row-key="id"
          flat
          bordered
        >
          <template v-slot:body="props">
            <q-tr :props="props" @click="viewZone(props.row)" style="cursor: pointer;">
              <q-td key="id" style="max-width: 50px">
                {{ props.row.id }}
              </q-td>
              <q-td key="name">
                <q-badge color="secondary" :label="props.row.name || 'Unnamed'"/>
              </q-td>
              <q-td key="description">
                {{ props.row.description || '-' }}
              </q-td>
            </q-tr>
          </template>
        </q-table>

        <div v-else class="text-center text-grey-6 q-pa-md">
          <q-icon name="mdi-map-marker-off" size="md"/>
          <div>Not assigned to any zones</div>
        </div>
      </div>

      <q-separator/>

      <!-- Actions -->
      <q-card-actions>
        <q-btn color="primary" :to="uri +'/'+ $route.params.idPrimary+'/edit'" icon="mdi-pencil">
          Edit
        </q-btn>
        <q-btn color="grey" @click="$router.go(-1)" icon="mdi-arrow-left">
          Back
        </q-btn>
      </q-card-actions>
    </q-card>

    <!-- Loading State -->
    <q-inner-loading :showing="loading">
      <q-spinner-gears size="50px" color="primary"/>
    </q-inner-loading>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from "vue";

import {useApolloClient} from "@vue/apollo-composable";
import {useRoute, useRouter} from "vue-router";

import {useQuasar} from "quasar";

import {DEVICE_GET_BY_ID_CHILDS, PORT_DELETE_BY_ID} from "@/graphql/queries";

import {format} from 'date-fns';
import DeviceIpUpdater from '@/components/DeviceIpUpdater.vue';
import {useDeviceAdminUrl} from '@/composables';

export default defineComponent({
  name: 'DeviceView',
  components: { DeviceIpUpdater },
  setup() {
    const uri = '/admin/devices';
    const $q = useQuasar();
    const viewItem = ref();
    const loading = ref(false);
    const {client} = useApolloClient();
    const router = useRouter();
    const route = useRoute();

    // Resolve the controller admin-UI URL on the frontend by reading the
    // per-model pattern from ConfigProvider (graphql `config(key:...)`) and
    // substituting placeholders from the device's own data. Recomputes when
    // viewItem changes (refetched after IP update / account edit).
    const { adminUrl } = useDeviceAdminUrl(viewItem);

    const pagination = ref({
      sortBy: 'id',
      descending: true,
      page: 1,
      rowsPerPage: 10
    });

    const portColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'internalRef', label: 'Ref ID', field: 'internalRef', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
      {name: 'value', label: 'Value', field: 'value', align: 'left', sortable: true},
      {name: 'actions', label: 'Actions', field: row => '', align: 'right', sortable: false}
    ];

    const zoneColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
    ];

    /**
     * Format date for display
     */
    const formatDate = (dateString) => {
      if (!dateString) return '-';
      try {
        const date = new Date(dateString);
        return format(date, 'dd/MM/yyyy HH:mm');
      } catch (error) {
        return '-';
      }
    };

    /**
     * Get status badge color based on device status
     */
    const getStatusColor = (status) => {
      if (!status) return 'grey';
      const statusUpper = status.toUpperCase();
      switch (statusUpper) {
        case 'ONLINE':
          return 'positive';
        case 'OFFLINE':
          return 'negative';
        case 'DISABLED':
          return 'warning';
        default:
          return 'grey';
      }
    };

    /**
     * Get status badge label
     */
    const getStatusLabel = (status) => {
      if (!status) return 'Unknown';
      return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
    };

    /**
     * Mask account passwords for display. Returns dots of fixed length so the
     * actual length isn't leaked, and a single '—' when the field is empty.
     */
    const maskPassword = (pw) => {
      if (!pw) return '—';
      return '•'.repeat(8);
    };

    /**
     * Get status badge icon
     */
    const getStatusIcon = (status) => {
      if (!status) return 'mdi-help-circle';
      const statusUpper = status.toUpperCase();
      switch (statusUpper) {
        case 'ONLINE':
          return 'mdi-check-circle';
        case 'OFFLINE':
          return 'mdi-close-circle';
        case 'DISABLED':
          return 'mdi-pause-circle';
        default:
          return 'mdi-help-circle';
      }
    };

    /**
     * Fetch device data from server
     */
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: DEVICE_GET_BY_ID_CHILDS,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        viewItem.value = response.data.device;
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load device details',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching device:', error);
      });
    };

    /**
     * Remove port with confirmation
     */
    const removeItem = (toDelete) => {
      $q.dialog({
        title: 'Confirm Deletion',
        message: `Are you sure you want to delete port "${toDelete.name}"?`,
        ok: {
          push: true,
          color: 'negative',
          label: "Delete",
          icon: 'mdi-delete'
        },
        cancel: {
          push: true,
          color: 'grey',
          label: 'Cancel'
        },
        persistent: true
      }).onOk(() => {
        client.mutate({
          mutation: PORT_DELETE_BY_ID,
          variables: {id: toDelete.id},
          fetchPolicy: 'no-cache',
        }).then(response => {
          if (response.data.devicePortDelete.success) {
            $q.notify({
              color: 'positive',
              message: 'Port deleted successfully',
              icon: 'mdi-check-circle',
              position: 'top'
            });
            fetchData();
          } else {
            $q.notify({
              color: 'negative',
              message: response.data.devicePortDelete.error || 'Failed to delete port',
              icon: 'mdi-alert-circle',
              position: 'top'
            });
          }
        }).catch(error => {
          $q.notify({
            color: 'negative',
            message: 'Failed to delete port',
            icon: 'mdi-alert-circle',
            position: 'top'
          });
          console.error('Error deleting port:', error);
        });
      });
    };

    /**
     * Navigate to port view
     */
    const onRowClick = (row) => {
      router.push({path: `/admin/ports/${row.id}/view`});
    };

    /**
     * Navigate to new port page
     */
    const newPort = () => {
      router.push({path: `/admin/ports/new`, query: {deviceId: route.params.idPrimary}});
    };

    /**
     * Navigate to zone view
     */
    const viewZone = (row) => {
      router.push({path: `/admin/zones/${row.id}/view`});
    };

    onMounted(() => {
      fetchData();
    });

    return {
      uri,
      fetchData,
      viewItem,
      adminUrl,
      loading,
      removeItem,
      portColumns,
      zoneColumns,
      pagination,
      onRowClick,
      newPort,
      viewZone,
      formatDate,
      maskPassword,
      getStatusColor,
      getStatusLabel,
      getStatusIcon
    };
  }
});

</script>
