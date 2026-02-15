<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-card-section>
        <q-avatar size="103px" class="absolute-center shadow-10" :color="getCategoryColor(viewItem.category?.name)">
          <q-icon name="mdi-cable-data" size="xl" color="white"/>
        </q-avatar>
        <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      </q-card-section>

      <q-card-section class="row items-center">
        <div class="column">
          <div class="text-h4 text-secondary">{{ viewItem.code || 'Unnamed Cable' }}</div>
          <div class="text-subtitle2 text-grey-7">{{ viewItem.rack?.name || 'No rack assigned' }}</div>
        </div>
        <q-space/>
        <q-btn outline round color="amber-8" icon="mdi-pencil" :to="uri +'/'+ $route.params.idPrimary+'/edit'">
          <q-tooltip>Edit Cable</q-tooltip>
        </q-btn>
        <q-btn outline round color="teal-7" icon="mdi-label-outline" 
               @click="downloadLabel" 
               :loading="labelLoading"
               class="q-ml-sm">
          <q-tooltip>Download Label</q-tooltip>
        </q-btn>
        <q-btn outline round color="amber-8" icon="mdi-view-list" 
               :to="'/admin/configurations/'+ $route.params.idPrimary+'?type=CABLE'" class="q-ml-sm">
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

        <q-item v-if="viewItem.codeOld">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-barcode-scan" class="q-mr-sm"/>
              Code Old
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.codeOld }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.codeNew">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-barcode-scan" class="q-mr-sm"/>
              Code New
            </q-item-label>
            <q-item-label caption class="text-body2">{{ viewItem.codeNew }}</q-item-label>
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

        <q-item v-if="viewItem.category">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-shape" class="q-mr-sm"/>
              Category
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge 
                :color="getCategoryColor(viewItem.category.name)" 
                :label="viewItem.category.name"
              />
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-separator class="q-my-md"/>

        <!-- Location Information -->
        <q-item-label header class="text-h6 text-grey-8">Location Information</q-item-label>

        <q-item v-if="viewItem.rack">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-server" class="q-mr-sm"/>
              Rack
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge color="secondary" :label="viewItem.rack.name"/>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.rackRowNr || viewItem.orderInRow">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-grid" class="q-mr-sm"/>
              Position in Rack
            </q-item-label>
            <q-item-label caption class="text-body2">
              <span v-if="viewItem.rackRowNr">Row: <strong>{{ viewItem.rackRowNr }}</strong></span>
              <span v-if="viewItem.rackRowNr && viewItem.orderInRow"> | </span>
              <span v-if="viewItem.orderInRow">Order: <strong>{{ viewItem.orderInRow }}</strong></span>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.patchPanel">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-lan" class="q-mr-sm"/>
              Patch Panel
            </q-item-label>
            <q-item-label caption class="text-body2">
              <div class="q-gutter-sm">
                <div><strong>Name:</strong> {{ viewItem.patchPanel.name }}</div>
                <div><strong>Port:</strong> {{ viewItem.patchPanelPort }}/{{ viewItem.patchPanel.size }}</div>
                <div v-if="viewItem.patchPanel.description" class="text-grey-7">
                  {{ viewItem.patchPanel.description }}
                </div>
              </div>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.device">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-devices" class="q-mr-sm"/>
              Device
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge color="info" :label="viewItem.device.code" class="q-mr-sm"/>
              <span>{{ viewItem.device.name }}</span>
              <q-btn 
                icon="mdi-eye" 
                :to="'/admin/devices/'+viewItem.device.id+'/view'" 
                size="sm" 
                flat 
                dense 
                color="blue-6"
                class="q-ml-sm"
              >
                <q-tooltip>View Device</q-tooltip>
              </q-btn>
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
        <div class="text-h6 text-grey-8 q-mb-md">
          <q-icon name="mdi-ethernet" class="q-mr-sm"/>
          Connected Ports
        </div>
        
        <q-table
          v-if="viewItem.connectedTo && viewItem.connectedTo.length > 0"
          :rows="viewItem.connectedTo"
          :columns="portColumns"
          @row-click="viewPort"
          v-model:pagination="pagination"
          row-key="id"
          flat
          bordered
        >
          <template v-slot:body="props">
            <q-tr :props="props" @click="viewPort(props.row)" style="cursor: pointer;">
              <q-td key="id" style="max-width: 50px">
                {{ props.row.id }}
              </q-td>
              <q-td key="internalRef">
                <q-badge color="info" :label="props.row.internalRef || props.row.id"/>
              </q-td>
              <q-td key="name">
                {{ props.row.name || '-' }}
              </q-td>
            </q-tr>
          </template>
        </q-table>

        <div v-else class="text-center text-grey-6 q-pa-md">
          <q-icon name="mdi-ethernet-off" size="md"/>
          <div>No ports connected</div>
        </div>
      </div>

      <q-separator/>

      <!-- Connected Peripherals Table -->
      <div class="q-pa-md">
        <div class="text-h6 text-grey-8 q-mb-md">
          <q-icon name="mdi-power-socket-au" class="q-mr-sm"/>
          Connected Peripherals
        </div>
        
        <q-table
          v-if="viewItem.peripherals && viewItem.peripherals.length > 0"
          :rows="viewItem.peripherals"
          :columns="peripheralColumns"
          @row-click="viewPeripheral"
          v-model:pagination="pagination"
          row-key="id"
          flat
          bordered
        >
          <template v-slot:body="props">
            <q-tr :props="props" @click="viewPeripheral(props.row)" style="cursor: pointer;">
              <q-td key="id" style="max-width: 50px">
                {{ props.row.id }}
              </q-td>
              <q-td key="name">
                <q-badge color="primary" :label="props.row.name || 'Unnamed'"/>
              </q-td>
              <q-td key="model">
                {{ props.row.model || '-' }}
              </q-td>
            </q-tr>
          </template>
        </q-table>

        <div v-else class="text-center text-grey-6 q-pa-md">
          <q-icon name="mdi-power-plug-off" size="md"/>
          <div>No peripherals connected</div>
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

import {CABLE_BY_ID} from "@/graphql/queries";
import {labelService} from '@/_services';

import {format} from 'date-fns';

export default defineComponent({
  name: 'CableView',
  setup() {
    const uri = '/admin/cables';
    const $q = useQuasar();
    const viewItem = ref();
    const loading = ref(false);
    const labelLoading = ref(false);
    const {client} = useApolloClient();
    const router = useRouter();
    const route = useRoute();

    const pagination = ref({
      sortBy: 'id',
      descending: true,
      page: 1,
      rowsPerPage: 10
    });

    const portColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'internalRef', label: 'Int Ref', field: 'internalRef', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
    ];

    const peripheralColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'model', label: 'Model', field: 'model', align: 'left', sortable: true},
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
     * Get color for category badge
     */
    const getCategoryColor = (category) => {
      if (!category) return 'grey-7';
      const colors = {
        'CAT5': 'blue-6',
        'CAT5E': 'blue-7',
        'CAT6': 'green-7',
        'CAT6A': 'green-8',
        'CAT7': 'purple-7',
        'FIBER': 'orange-7',
        'COAX': 'brown-7',
        'POWER': 'red-7'
      };
      return colors[category] || 'grey-7';
    };

    /**
     * Fetch cable data from server
     */
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: CABLE_BY_ID,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        viewItem.value = response.data.cable;
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load cable details',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching cable:', error);
      });
    };

    /**
     * Navigate to port view
     */
    const viewPort = (row) => {
      router.push({path: `/admin/ports/${row.id}/view`});
    };

    /**
     * Navigate to peripheral view
     */
    const viewPeripheral = (row) => {
      router.push({path: `/admin/peripherals/${row.id}/view`});
    };

    /**
     * Download label for the current cable
     */
    const downloadLabel = async () => {
      labelLoading.value = true;
      try {
        await labelService.downloadCableLabel(route.params.idPrimary, {
          template: 'brother_18mm'
        });
        $q.notify({
          color: 'positive',
          message: 'Label downloaded successfully',
          icon: 'mdi-check-circle',
          position: 'top',
          timeout: 2000
        });
      } catch (error) {
        $q.notify({
          color: 'negative',
          message: 'Failed to download label: ' + error.message,
          icon: 'mdi-alert-circle',
          position: 'top'
        });
      } finally {
        labelLoading.value = false;
      }
    };

    onMounted(() => {
      fetchData();
    });

    return {
      uri,
      fetchData,
      viewItem,
      loading,
      labelLoading,
      pagination,
      portColumns,
      peripheralColumns,
      viewPort,
      viewPeripheral,
      formatDate,
      getCategoryColor,
      downloadLabel
    };
  }
});

</script>
