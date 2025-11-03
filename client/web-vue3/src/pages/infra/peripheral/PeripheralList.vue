<template>
  <q-page padding>
    <q-btn icon="mdi-plus-circle" color="positive" :disable="loading" label="Add Peripheral" @click="addRow" class="q-mb-md"/>
    <q-table
      :rows="rows"
      :columns="columns"
      :loading="loading"
      :filter="filter"
      v-model:pagination="pagination"
      row-key="id"
      flat
      bordered
      class="myhab-list-qgrid"
    >
      <template v-slot:top-right>
        <q-input dense outlined debounce="300" color="primary" v-model="filter" placeholder="Search peripherals...">
          <template v-slot:prepend>
            <q-icon name="mdi-magnify"/>
          </template>
          <template v-slot:append v-if="filter">
            <q-icon name="mdi-close-circle" @click="filter = ''" class="cursor-pointer"/>
          </template>
        </q-input>
      </template>
      <template v-slot:body="props">
        <q-tr :props="props" @click="onRowClick(props.row)" style="cursor: pointer;">
          <q-td key="id" style="max-width: 50px">
            {{ props.row.id }}
          </q-td>
          <q-td key="name">
            <q-badge color="primary" :label="props.row.name || 'Unnamed'"/>
          </q-td>
          <q-td key="model">
            {{ props.row.model || '-' }}
          </q-td>
          <q-td key="category">
            <q-badge 
              v-if="props.row.category" 
              :color="getCategoryColor(props.row.category)" 
              :label="props.row.category"
            />
            <span v-else class="text-grey-6">-</span>
          </q-td>
          <q-td key="description">
            {{ props.row.description ? (props.row.description.length > 50 ? props.row.description.substring(0, 50) + '...' : props.row.description) : '-' }}
          </q-td>
          <q-td key="portsCount">
            <q-badge color="info" :label="props.row.portsCount || 0">
              <q-tooltip v-if="props.row.portsCount">Connected to {{ props.row.portsCount }} port(s)</q-tooltip>
            </q-badge>
          </q-td>
          <q-td key="tsCreated">
            {{ formatDate(props.row.tsCreated) }}
          </q-td>
          <q-td key="actions">
            <q-btn-group>
              <q-btn icon="mdi-open-in-new" @click.stop="" :href="'/'+uri+'/'+props.row.id+'/view'" target="_blank"
                     color="blue-6" flat>
                <q-tooltip>View</q-tooltip>
              </q-btn>
              <q-btn icon="mdi-note-edit-outline" color="amber-7" @click.stop="onEdit(props.row)" flat>
                <q-tooltip>Edit</q-tooltip>
              </q-btn>
              <q-btn icon="mdi-delete" color="red-7" @click.stop="removeItem(props.row)" flat>
                <q-tooltip>Delete</q-tooltip>
              </q-btn>
            </q-btn-group>
          </q-td>
        </q-tr>
      </template>
    </q-table>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from "vue";

import {useApolloClient} from "@vue/apollo-composable";
import {useRouter} from "vue-router";

import {useQuasar} from "quasar";

import {PERIPHERAL_DELETE, PERIPHERAL_LIST_ALL} from "@/graphql/queries";

import {format} from 'date-fns';

export default defineComponent({
  name: 'PeripheralList',
  setup() {
    const uri = '/admin/peripherals';
    const $q = useQuasar();
    const {client} = useApolloClient();
    const loading = ref(false);
    const router = useRouter();
    const rows = ref([]);
    const filter = ref('');
    const columns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'model', label: 'Model', field: 'model', align: 'left', sortable: true},
      {name: 'category', label: 'Category', field: 'category', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
      {name: 'portsCount', label: 'Ports', field: 'portsCount', align: 'left', sortable: true},
      {name: 'tsCreated', label: 'Created', field: 'tsCreated', align: 'left', sortable: true},
      {name: 'actions', label: 'Actions', field: row => '', align: 'right', sortable: false}
    ];
    const pagination = ref({
      sortBy: 'id',
      descending: true,
      page: 1,
      rowsPerPage: 10
    });

    /**
     * Format date for display
     */
    const formatDate = (dateString) => {
      if (!dateString) return '-';
      try {
        return format(new Date(dateString), 'dd/MM/yyyy HH:mm');
      } catch (error) {
        return '-';
      }
    };

    /**
     * Get color for category badge
     */
    const getCategoryColor = (category) => {
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
     * Fetch peripheral list from server
     */
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: PERIPHERAL_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        rows.value = (response.data.devicePeripheralList || []).map(peripheral => ({
          id: peripheral.id,
          name: peripheral.name,
          model: peripheral.model,
          description: peripheral.description,
          category: peripheral.category?.name || null,
          portsCount: peripheral.connectedTo?.length || 0,
          tsCreated: peripheral.tsCreated,
          tsUpdated: peripheral.tsUpdated
        }));
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load peripherals',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching peripherals:', error);
      });
    };

    /**
     * Remove peripheral with confirmation
     */
    const removeItem = (toDelete) => {
      $q.dialog({
        title: 'Confirm Deletion',
        message: `Are you sure you want to delete peripheral "${toDelete.name}"?`,
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
          mutation: PERIPHERAL_DELETE,
          variables: {id: toDelete.id},
          fetchPolicy: 'no-cache',
        }).then(response => {
          if (response.data.devicePeripheralDelete.success) {
            $q.notify({
              color: 'positive',
              message: 'Peripheral deleted successfully',
              icon: 'mdi-check-circle',
              position: 'top'
            });
            fetchData();
          } else {
            $q.notify({
              color: 'negative',
              message: response.data.devicePeripheralDelete.error || 'Failed to delete peripheral',
              icon: 'mdi-alert-circle',
              position: 'top'
            });
          }
        }).catch(error => {
          $q.notify({
            color: 'negative',
            message: 'Failed to delete peripheral',
            icon: 'mdi-alert-circle',
            position: 'top'
          });
          console.error('Error deleting peripheral:', error);
        });
      });
    };

    /**
     * Handle row click to view details
     */
    const onRowClick = (row) => {
      router.push({path: `${uri}/${row.id}/view`});
    };

    /**
     * Navigate to edit page
     */
    const onEdit = (row) => {
      router.push({path: `${uri}/${row.id}/edit`});
    };

    /**
     * Navigate to new peripheral page
     */
    const addRow = () => {
      router.push({path: `${uri}/new`});
    };

    onMounted(() => {
      fetchData();
    });

    return {
      rows,
      columns,
      filter,
      pagination,
      loading,
      uri,
      fetchData,
      removeItem,
      onRowClick,
      onEdit,
      addRow,
      formatDate,
      getCategoryColor
    };
  }
});

</script>
