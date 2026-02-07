<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-card-section>
        <q-avatar size="103px" class="absolute-center shadow-10">
          <q-icon name="mdi-ethernet" size="xl"/>
        </q-avatar>
        <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      </q-card-section>

      <q-card-section class="row items-center">
        <div class="text-h4 text-secondary">{{ viewItem.name }}</div>
        <q-space/>
        <q-btn outline round color="amber-8" icon="mdi-pencil" :to="uri +'/'+ $route.params.idPrimary+'/edit'">
          <q-tooltip>Edit Port</q-tooltip>
        </q-btn>
        <q-btn outline round color="amber-8" icon="mdi-view-list" 
               :to="'/admin/configurations/'+ $route.params.idPrimary+'?type=PORT'" class="q-ml-sm">
          <q-tooltip>View Configurations</q-tooltip>
        </q-btn>
      </q-card-section>

      <!-- Device Expansion -->
      <q-expansion-item v-if="viewItem.device" class="bg-blue-2"
                        expand-separator
                        icon="mdi-devices"
                        label="Device"
                        :caption="viewItem.device.name"
                        default-opened>
        <q-item-section style="padding: 15px" class="bg-blue-1">
          <q-item-label class="text-h6 text-grey-8">
            <q-icon name="mdi-identifier" class="q-mr-sm"/>
            {{ viewItem.device.code }}
          </q-item-label>
          <q-item-label class="text-subtitle1 text-grey-7 q-mt-sm">
            {{ viewItem.device.name }}
          </q-item-label>
          <q-item-label caption class="q-mt-sm">
            {{ viewItem.device.description }}
          </q-item-label>
          <q-item-label class="q-mt-md">
            <q-btn icon="mdi-eye" :to="'/admin/devices/'+viewItem.device.id+'/view'" 
                   size="sm" label="View Device" color="blue-6" outline/>
          </q-item-label>
        </q-item-section>
      </q-expansion-item>

      <q-separator/>

      <!-- Port Information -->
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
              <q-icon name="mdi-tag" class="q-mr-sm"/>
              Internal Reference
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge color="primary" :label="viewItem.internalRef"/>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-label" class="q-mr-sm"/>
              Name
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.name }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-text" class="q-mr-sm"/>
              Description
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.description || '-' }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-format-list-bulleted-type" class="q-mr-sm"/>
              Type
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge v-if="viewItem.type" color="info" :label="viewItem.type"/>
              <span v-else class="text-grey-6">Not specified</span>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-state-machine" class="q-mr-sm"/>
              State
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge v-if="viewItem.state" :color="getStateColor(viewItem.state)" :label="viewItem.state"/>
              <span v-else class="text-grey-6">Not specified</span>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-numeric" class="q-mr-sm"/>
              Value
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.value || '-' }}</q-item-label>
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

      <!-- Connected Peripherals Table -->
      <div class="q-pa-md" v-if="viewItem.peripherals && viewItem.peripherals.length > 0">
        <q-table
          title="Connected Peripherals"
          :rows="viewItem.peripherals"
          :columns="peripheralColumns"
          @row-click="onPeripheralRowClick"
          :pagination="pagination"
          row-key="id"
          flat
          bordered
        >
          <template v-slot:body="props">
            <q-tr :props="props" @click="onPeripheralRowClick(props.row)" style="cursor: pointer;">
              <q-td key="id">
                {{ props.row.id }}
              </q-td>
              <q-td key="name">
                <q-badge color="primary" :label="props.row.name || 'Unnamed'"/>
              </q-td>
              <q-td key="model">
                {{ props.row.model || '-' }}
              </q-td>
              <q-td key="category">
                <q-badge v-if="props.row.category" color="secondary" :label="props.row.category.name"/>
                <span v-else class="text-grey-6">-</span>
              </q-td>
              <q-td key="description">
                {{ props.row.description || '-' }}
              </q-td>
            </q-tr>
          </template>
        </q-table>
      </div>
      <div v-else class="q-pa-md text-center text-grey-6">
        <q-icon name="mdi-power-socket-au" size="md"/>
        <div>No peripherals connected to this port</div>
      </div>

      <q-separator/>

      <!-- Connected Cables Table -->
      <div class="q-pa-md" v-if="viewItem.cables && viewItem.cables.length > 0">
        <q-table
          title="Connected Cables"
          :rows="viewItem.cables"
          :columns="cableColumns"
          @row-click="onCableRowClick"
          :pagination="pagination"
          row-key="id"
          flat
          bordered
        >
          <template v-slot:body="props">
            <q-tr :props="props" @click="onCableRowClick(props.row)" style="cursor: pointer;">
              <q-td key="id">
                {{ props.row.id }}
              </q-td>
              <q-td key="code">
                <q-badge color="primary" :label="props.row.code"/>
              </q-td>
              <q-td key="description">
                {{ props.row.description }}
              </q-td>
            </q-tr>
          </template>
        </q-table>
      </div>
      <div v-else class="q-pa-md text-center text-grey-6">
        <q-icon name="mdi-cable-data" size="md"/>
        <div>No cables connected to this port</div>
      </div>

      <q-separator/>

      <!-- Port Values History Table -->
      <div class="q-pa-md">
        <div class="text-h6 text-grey-8 q-mb-md">
          <q-icon name="mdi-history" class="q-mr-sm"/>
          Recent Value History (Last 20 Records)
        </div>
        <q-table
          :rows="portValues"
          :columns="portValueColumns"
          :loading="portValuesLoading"
          row-key="id"
          flat
          bordered
          virtual-scroll
          :rows-per-page-options="[0]"
          style="max-height: 400px"
          class="sticky-header-table"
        >
          <template v-slot:body-cell-value="props">
            <q-td :props="props">
              <div class="text-mono text-grey-9" style="max-width: 400px; word-break: break-all;">
                {{ props.row.value }}
              </div>
            </q-td>
          </template>

          <template v-slot:body-cell-tsCreated="props">
            <q-td :props="props">
              <div class="text-grey-7">
                {{ formatDateWithSeconds(props.row.tsCreated) }}
              </div>
            </q-td>
          </template>

          <template v-slot:no-data>
            <div class="full-width row flex-center text-grey-6 q-gutter-sm q-pa-md">
              <q-icon size="2em" name="mdi-database-off"/>
              <span>No value history available for this port</span>
            </div>
          </template>
        </q-table>
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
import {useRoute, useRouter} from "vue-router/dist/vue-router";

import {useQuasar} from "quasar";

import {PORT_GET_BY_ID, PORT_VALUES_RECENT} from "@/graphql/queries";

import {format} from 'date-fns';

export default defineComponent({
  name: 'PortView',
  setup() {
    const uri = '/admin/ports';
    const $q = useQuasar();
    const viewItem = ref();
    const loading = ref(false);
    const portValues = ref([]);
    const portValuesLoading = ref(false);
    const {client} = useApolloClient();
    const router = useRouter();
    const route = useRoute();
    const pagination = ref({
      sortBy: 'id',
      descending: true,
      page: 1,
      rowsPerPage: 10
    });
    const cableColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'code', label: 'Code', field: 'code', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
    ];
    
    const peripheralColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'model', label: 'Model', field: 'model', align: 'left', sortable: true},
      {name: 'category', label: 'Category', field: row => row.category?.name || '-', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
    ];

    const portValueColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'value', label: 'Value', field: 'value', align: 'left', sortable: false},
      {name: 'tsCreated', label: 'Timestamp', field: 'tsCreated', align: 'left', sortable: true},
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
     * Format date with seconds for display
     */
    const formatDateWithSeconds = (dateString) => {
      if (!dateString) return '-';
      try {
        const date = new Date(dateString);
        return format(date, 'dd/MM/yyyy HH:mm:ss');
      } catch (error) {
        return '-';
      }
    };

    const getStateColor = (state) => {
      const stateColors = {
        'ACTIVE': 'positive',
        'INACTIVE': 'grey',
        'ERROR': 'negative',
        'WARNING': 'warning',
        'UNKNOW': 'grey-5'
      };
      return stateColors[state] || 'grey';
    };

    const fetchData = () => {
      loading.value = true;
      client.query({
        query: PORT_GET_BY_ID,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        viewItem.value = response.data.devicePort;
        loading.value = false;
        
        // Fetch port values after port data is loaded
        fetchPortValues();
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load port details',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching port:', error);
      });
    };

    /**
     * Fetch recent port values
     */
    const fetchPortValues = () => {
      portValuesLoading.value = true;
      client.query({
        query: PORT_VALUES_RECENT,
        variables: {portId: Number.parseInt(route.params.idPrimary), limit: 20},
        fetchPolicy: 'network-only',
      }).then(response => {
        portValues.value = response.data.recentPortValues || [];
        portValuesLoading.value = false;
      }).catch(error => {
        portValuesLoading.value = false;
        console.error('Error fetching port values:', error);
        // Don't show notification for port values error - it's not critical
      });
    };

    onMounted(() => {
      fetchData();
    });

    return {
      uri,
      fetchData,
      viewItem,
      loading,
      portValues,
      portValuesLoading,
      pagination,
      cableColumns,
      peripheralColumns,
      portValueColumns,
      getStateColor,
      formatDate,
      formatDateWithSeconds,
      onCableRowClick: (row) => {
        router.push({path: `/admin/cables/${row.id}/view`});
      },
      onPeripheralRowClick: (row) => {
        router.push({path: `/admin/peripherals/${row.id}/view`});
      }
    };
  }
});

</script>

<style scoped>
.sticky-header-table {
  /* Make table headers sticky */
  :deep(thead tr th) {
    position: sticky;
    top: 0;
    z-index: 1;
    background-color: #f5f5f5;
  }

  /* Ensure table body is scrollable */
  :deep(.q-table__middle) {
    max-height: 400px;
  }
}

.text-mono {
  font-family: 'Courier New', Courier, monospace;
  font-size: 0.85rem;
}
</style>
