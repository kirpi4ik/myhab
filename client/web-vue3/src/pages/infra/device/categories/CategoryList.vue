<template>
  <q-page padding>
    <q-btn icon="mdi-plus-circle" color="positive" :disable="loading" label="Add Category" @click="addRow" class="q-mb-md"/>
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
        <q-input dense outlined debounce="300" color="primary" v-model="filter" placeholder="Search categories...">
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
          <q-td key="id" :props="props" style="max-width: 50px">
            {{ props.row.id }}
          </q-td>
          <q-td key="name" :props="props">
            <q-badge color="primary" :label="props.row.name || 'Unnamed'"/>
          </q-td>
          <q-td key="title" :props="props">
            {{ props.row.title || '-' }}
          </q-td>
          <q-td key="description" :props="props">
            {{ props.row.description ? (props.row.description.length > 50 ? props.row.description.substring(0, 50) + '...' : props.row.description) : '-' }}
          </q-td>
          <q-td key="devicesCount" :props="props">
            <q-badge color="info" :label="props.row.devicesCount || 0">
              <q-tooltip v-if="props.row.devicesCount">{{ props.row.devicesCount }} device(s)</q-tooltip>
            </q-badge>
          </q-td>
          <q-td key="tsCreated" :props="props">
            {{ formatDate(props.row.tsCreated) }}
          </q-td>
          <q-td key="actions" :props="props">
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

import {DEVICE_CATEGORIES_LIST, DEVICE_CATEGORY_DELETE} from "@/graphql/queries";

import {format} from 'date-fns';

export default defineComponent({
  name: 'DCategoryList',
  setup() {
    const uri = '/admin/dcategories';
    const $q = useQuasar();
    const {client} = useApolloClient();
    const loading = ref(false);
    const router = useRouter();
    const rows = ref([]);
    const filter = ref('');
    const pagination = ref({
      sortBy: 'id',
      descending: true,
      page: 1,
      rowsPerPage: 10
    });
    const columns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'title', label: 'Title', field: 'title', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
      {name: 'devicesCount', label: 'Devices', field: 'devicesCount', align: 'left', sortable: true},
      {name: 'tsCreated', label: 'Created', field: 'tsCreated', align: 'left', sortable: true},
      {name: 'actions', label: 'Actions', field: row => '', align: 'right', sortable: false}
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
     * Fetch category list from server
     */
    const fetchData = () => {
      loading.value = true;
      client.query({
        query: DEVICE_CATEGORIES_LIST,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        rows.value = (response.data.deviceCategoryList || []).map(category => ({
          id: category.id,
          name: category.name,
          title: category.title,
          description: category.description,
          devicesCount: category.devices?.length || 0,
          tsCreated: category.tsCreated,
          tsUpdated: category.tsUpdated
        }));
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load device categories',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching device categories:', error);
      });
    };

    /**
     * Remove category with confirmation
     */
    const removeItem = (toDelete) => {
      $q.dialog({
        title: 'Confirm Deletion',
        message: `Are you sure you want to delete category "${toDelete.name}"?`,
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
          mutation: DEVICE_CATEGORY_DELETE,
          variables: {id: toDelete.id},
          fetchPolicy: 'no-cache',
        }).then(response => {
          if (response.data.deviceCategoryDelete.success) {
            $q.notify({
              color: 'positive',
              message: 'Category deleted successfully',
              icon: 'mdi-check-circle',
              position: 'top'
            });
            fetchData();
          } else {
            $q.notify({
              color: 'negative',
              message: response.data.deviceCategoryDelete.error || 'Failed to delete category',
              icon: 'mdi-alert-circle',
              position: 'top'
            });
          }
        }).catch(error => {
          $q.notify({
            color: 'negative',
            message: 'Failed to delete category',
            icon: 'mdi-alert-circle',
            position: 'top'
          });
          console.error('Error deleting category:', error);
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
     * Navigate to new category page
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
      formatDate
    };
  }
});

</script>
