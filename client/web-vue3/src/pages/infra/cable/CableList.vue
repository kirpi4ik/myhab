<template>
  <q-page padding>
    <q-btn icon="mdi-plus-circle" color="positive" :disable="loading" label="Add Cable" @click="addRow" class="q-mb-md"/>
    <q-table 
      :rows="rows" 
      :columns="columns" 
      :loading="loading"
      :filter="filter"
      v-model:pagination="pagination"
      row-key="id"
      class="myhab-list-qgrid"
      flat
      bordered
    >
      <template v-slot:top-right>
        <q-input dense outlined debounce="300" color="primary" v-model="filter" placeholder="Search cables...">
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
          <q-td key="code">
            <q-badge color="primary" :label="props.row.code || 'No Code'"/>
          </q-td>
          <q-td key="location">
            <q-badge color="secondary" :label="props.row.location || '-'"/>
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
          <q-td key="patchPanel">
            {{ props.row.patchPanel || '-' }}
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
import {useRouter} from "vue-router";

import {useQuasar} from "quasar";

import {useApolloClient} from "@vue/apollo-composable";

import {CABLE_DELETE, CABLE_LIST_ALL} from "@/graphql/queries";

import {format} from 'date-fns';

export default defineComponent({
  name: 'CableList',
  setup() {
    const uri = '/admin/cables';
    const $q = useQuasar();
    const {client} = useApolloClient();
    const loading = ref(false);
    const router = useRouter();
    const rows = ref([]);
    const filter = ref('');
    const categoryList = ref([]);
    const pagination = ref({
      sortBy: 'id',
      descending: true,
      page: 1,
      rowsPerPage: 10
    });
    const columns = [
      {name: 'id', label: 'ID', field: 'id', sortable: true, align: 'left'},
      {name: 'code', label: 'Code', field: 'code', sortable: true, align: 'left'},
      {name: 'location', label: 'Rack', field: 'location', sortable: true, align: 'left'},
      {name: 'category', label: 'Category', field: 'category', sortable: true, align: 'left'},
      {name: 'description', label: 'Description', field: 'description', sortable: true, align: 'left'},
      {name: 'patchPanel', label: 'Panel Port', field: 'patchPanel', sortable: true, align: 'left'},
      {name: 'tsCreated', label: 'Created', field: 'tsCreated', sortable: true, align: 'left'},
      {name: 'actions', label: 'Actions', field: row => '', sortable: false, align: 'right'}
    ];

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
     * Fetch cable list from server
     */
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: CABLE_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        categoryList.value = (response.data.cableCategoryList || []).map(cat => cat.name);
        
        rows.value = (response.data.cableList || []).map(cable => ({
          id: cable.id,
          code: cable.code,
          location: cable.rack?.name || '-',
          category: cable.category?.name || null,
          description: cable.description,
          patchPanel: cable.patchPanel 
            ? `${cable.patchPanel.name} (${cable.patchPanelPort}/${cable.patchPanel.size})` 
            : null,
          tsCreated: cable.tsCreated,
          tsUpdated: cable.tsUpdated
        }));
        
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load cables',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching cables:', error);
      });
    };

    /**
     * Remove cable with confirmation
     */
    const removeItem = (toDelete) => {
      $q.dialog({
        title: 'Confirm Deletion',
        message: `Are you sure you want to delete cable "${toDelete.code}"?`,
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
          mutation: CABLE_DELETE,
          variables: {id: toDelete.id},
          fetchPolicy: 'no-cache',
        }).then(response => {
          if (response.data.cableDelete.success) {
            $q.notify({
              color: 'positive',
              message: 'Cable deleted successfully',
              icon: 'mdi-check-circle',
              position: 'top'
            });
            fetchData();
          } else {
            $q.notify({
              color: 'negative',
              message: response.data.cableDelete.error || 'Failed to delete cable',
              icon: 'mdi-alert-circle',
              position: 'top'
            });
          }
        }).catch(error => {
          $q.notify({
            color: 'negative',
            message: 'Failed to delete cable',
            icon: 'mdi-alert-circle',
            position: 'top'
          });
          console.error('Error deleting cable:', error);
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
     * Navigate to new cable page
     */
    const addRow = () => {
      router.push({path: `${uri}/new`});
    };

    onMounted(() => {
      fetchData();
    });

    return {
      rows,
      loading,
      filter,
      fetchData,
      removeItem,
      categoryList,
      columns,
      pagination,
      uri,
      onRowClick,
      onEdit,
      addRow,
      formatDate,
      getCategoryColor
    };
  }
});

</script>

<style>
.myhab-list-qgrid input:first-child {
  max-width: 40px;
}
</style>
