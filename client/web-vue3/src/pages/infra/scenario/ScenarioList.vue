<template>
  <q-page padding>
    <q-btn icon="mdi-plus-circle" color="positive" :disable="loading" label="Add Scenario" @click="addRow" class="q-mb-md"/>
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
        <q-input dense outlined debounce="300" color="primary" v-model="filter" placeholder="Search...">
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
          <q-td key="body">
            {{ props.row.body ? (props.row.body.length > 100 ? props.row.body.substring(0, 100) + '...' : props.row.body) : '-' }}
          </q-td>
          <q-td key="portsCount">
            <q-badge color="info" :label="props.row.portsCount"/>
          </q-td>
          <q-td key="tsCreated">
            {{ formatDate(props.row.tsCreated) }}
          </q-td>
          <q-td key="tsUpdated">
            {{ formatDate(props.row.tsUpdated) }}
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
import {useRouter} from "vue-router/dist/vue-router";

import {useQuasar} from "quasar";

import {SCENARIO_DELETE_BY_ID, SCENARIO_LIST_ALL} from "@/graphql/queries";

import _ from "lodash";

export default defineComponent({
  name: 'ScenarioList',
  setup() {
    const uri = '/admin/scenarios';
    const $q = useQuasar();
    const {client} = useApolloClient();
    const loading = ref(false);
    const router = useRouter();
    const rows = ref([]);
    const filter = ref('');
    const columns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'body', label: 'Body', field: 'body', align: 'left', sortable: true},
      {name: 'portsCount', label: 'Ports', field: 'portsCount', align: 'left', sortable: true},
      {name: 'tsCreated', label: 'Created', field: 'tsCreated', align: 'left', sortable: true},
      {name: 'tsUpdated', label: 'Updated', field: 'tsUpdated', align: 'left', sortable: true},
      {name: 'actions', label: 'Actions', field: row => '', align: 'right', sortable: false}
    ];
    const pagination = ref({
      sortBy: 'id',
      descending: true,
      page: 1,
      rowsPerPage: 10
    });

    const formatDate = (dateString) => {
      if (!dateString) return '-';
      const date = new Date(dateString);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    };

    const fetchData = () => {
      loading.value = true;
      client.query({
        query: SCENARIO_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        rows.value = _.transform(response.data.scenarioList,
          function (result, value) {
            let scenario = {
              id: value.id,
              name: value.name || 'Unnamed',
              body: value.body || '',
              portsCount: 0, // Will be populated if available
              tsCreated: value.tsCreated,
              tsUpdated: value.tsUpdated
            };
            result.push(scenario);
          }, []);
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load scenarios',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching scenarios:', error);
      });
    };

    const removeItem = (toDelete) => {
      $q.dialog({
        title: 'Confirm Deletion',
        message: `Are you sure you want to delete scenario "${toDelete.name}"?`,
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
        }
      }).onOk(() => {
        client.mutate({
          mutation: SCENARIO_DELETE_BY_ID,
          variables: {id: toDelete.id},
          fetchPolicy: 'no-cache',
          update: () => {
            // Prevent Apollo from processing the mutation result
          }
        }).then(response => {
          if (response.data.scenarioDelete.success) {
            $q.notify({
              color: 'positive',
              message: 'Scenario deleted successfully',
              icon: 'mdi-check-circle',
              position: 'top'
            });
            fetchData();
          } else {
            $q.notify({
              color: 'negative',
              message: response.data.scenarioDelete.error || 'Failed to delete scenario',
              icon: 'mdi-alert-circle',
              position: 'top'
            });
          }
        }).catch(error => {
          $q.notify({
            color: 'negative',
            message: 'Failed to delete scenario',
            icon: 'mdi-alert-circle',
            position: 'top'
          });
          console.error('Error deleting scenario:', error);
        });
      });
    };

    onMounted(() => {
      fetchData();
    });

    return {
      rows,
      columns,
      pagination,
      loading,
      fetchData,
      filter,
      removeItem,
      uri,
      formatDate,
      onRowClick: (row) => {
        router.push({path: `${uri}/${row.id}/view`});
      },
      onEdit: (row) => {
        router.push({path: `${uri}/${row.id}/edit`});
      },
      addRow: () => {
        router.push({path: `${uri}/new`});
      },
    };
  }
});

</script>

<style scoped>
.myhab-list-qgrid input:first-child {
  max-width: 40px;
}
</style>

