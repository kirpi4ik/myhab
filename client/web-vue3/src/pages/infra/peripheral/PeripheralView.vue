<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-card-section>
        <q-avatar size="103px" class="absolute-center shadow-10" :color="getCategoryColor(viewItem.category?.name)">
          <q-icon :name="getCategoryIcon(viewItem.category?.name)" size="xl" color="white"/>
        </q-avatar>
        <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      </q-card-section>

      <q-card-section class="row items-center">
        <div class="column">
          <div class="text-h4 text-secondary">{{ viewItem.name || 'Unnamed Peripheral' }}</div>
          <div class="text-subtitle2 text-grey-7">{{ viewItem.model || 'No model specified' }}</div>
        </div>
        <q-space/>
        <q-btn outline round color="amber-8" icon="mdi-pencil" :to="uri +'/'+ $route.params.idPrimary+'/edit'">
          <q-tooltip>Edit Peripheral</q-tooltip>
        </q-btn>
        <q-btn outline round color="amber-8" icon="mdi-view-list" 
               :to="'/admin/configurations/'+ $route.params.idPrimary+'?type=PERIPHERAL'" class="q-ml-sm">
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
              <q-icon name="mdi-label" class="q-mr-sm"/>
              Name
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge color="primary" :label="viewItem.name || 'Unnamed'"/>
            </q-item-label>
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
                class="q-mr-sm"
              />
              <q-btn 
                icon="mdi-eye" 
                :to="'/admin/pcategories/'+viewItem.category.id+'/view'" 
                size="sm" 
                flat 
                dense 
                color="blue-6"
              >
                <q-tooltip>View Category</q-tooltip>
              </q-btn>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-separator class="q-my-md"/>

        <!-- Connected Ports Section -->
        <q-item-label header class="text-h6 text-grey-8">Connected Ports</q-item-label>

      </q-list>

      <!-- Connected Ports Table -->
      <div class="q-pa-md" v-if="viewItem.connectedTo && viewItem.connectedTo.length > 0">
        <q-table
          :rows="viewItem.connectedTo"
          :columns="portColumns"
          @row-click="onPortRowClick"
          v-model:pagination="pagination"
          row-key="id"
          flat
          bordered
        >
          <template v-slot:body="props">
            <q-tr :props="props" @click="onPortRowClick(props.row)" style="cursor: pointer;">
              <q-td key="id">
                {{ props.row.id }}
              </q-td>
              <q-td key="internalRef">
                <q-badge color="info" :label="props.row.internalRef || '-'"/>
              </q-td>
              <q-td key="name">
                <q-badge color="primary" :label="props.row.name || 'Unnamed'"/>
              </q-td>
              <q-td key="value">
                {{ props.row.value || '-' }}
              </q-td>
              <q-td key="device">
                {{ props.row.device?.code || '-' }}
              </q-td>
            </q-tr>
          </template>
        </q-table>
      </div>
      
      <div v-else class="q-pa-md text-center text-grey-6">
        <q-icon name="mdi-ethernet-off" size="md"/>
        <div>No ports connected</div>
      </div>

      <q-list>
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

import {PERIPHERAL_GET_BY_ID} from "@/graphql/queries";

import {format} from 'date-fns';

export default defineComponent({
  name: 'PeripheralView',
  setup() {
    const uri = '/admin/peripherals';
    const $q = useQuasar();
    const viewItem = ref();
    const loading = ref(false);
    const {client} = useApolloClient();
    const router = useRouter();
    const route = useRoute();

    const pagination = ref({
      sortBy: 'id',
      descending: true,
      page: 1,
      rowsPerPage: 10
    });

    const zoneColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
    ];

    const portColumns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'internalRef', label: 'Internal Ref', field: 'internalRef', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'value', label: 'Value', field: 'value', align: 'left', sortable: true},
      {name: 'device', label: 'Device', field: row => row.device?.code || '-', align: 'left', sortable: true},
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
        'LIGHT': 'orange-7',
        'HEAT': 'red-7',
        'TEMP': 'blue-7',
        'LOCK': 'purple-7',
        'SENSOR': 'teal-7',
        'ACTUATOR': 'green-7',
        'CAMERA': 'indigo-7',
        'ALARM': 'deep-orange-7'
      };
      return colors[category] || 'grey-7';
    };

    /**
     * Get icon for category
     */
    const getCategoryIcon = (category) => {
      if (!category) return 'mdi-power-socket-au';
      const icons = {
        'LIGHT': 'mdi-lightbulb',
        'HEAT': 'mdi-fire',
        'TEMP': 'mdi-thermometer',
        'LOCK': 'mdi-lock',
        'SENSOR': 'mdi-radar',
        'ACTUATOR': 'mdi-cog',
        'CAMERA': 'mdi-camera',
        'ALARM': 'mdi-bell-alert'
      };
      return icons[category] || 'mdi-power-socket-au';
    };

    /**
     * Fetch peripheral data from server
     */
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: PERIPHERAL_GET_BY_ID,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        viewItem.value = response.data.devicePeripheral;
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load peripheral details',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching peripheral:', error);
      });
    };

    /**
     * Navigate to zone view
     */
    const viewZone = (row) => {
      router.push({path: `/admin/zones/${row.id}/view`});
    };

    /**
     * Navigate to port view
     */
    const onPortRowClick = (row) => {
      router.push({path: `/admin/ports/${row.id}/view`});
    };

    onMounted(() => {
      fetchData();
    });

    return {
      uri,
      fetchData,
      viewItem,
      loading,
      pagination,
      zoneColumns,
      portColumns,
      viewZone,
      onPortRowClick,
      formatDate,
      getCategoryColor,
      getCategoryIcon
    };
  }
});

</script>
