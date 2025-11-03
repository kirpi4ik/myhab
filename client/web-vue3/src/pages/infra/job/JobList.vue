<template>
  <q-page padding>
    <q-btn icon="mdi-plus-circle" color="positive" :disable="loading" label="Add Job" @click="addRow" class="q-mb-md"/>
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
          <q-td key="description">
            {{ props.row.description ? (props.row.description.length > 100 ? props.row.description.substring(0, 100) + '...' : props.row.description) : '-' }}
          </q-td>
          <q-td key="scenario">
            {{ props.row.scenarioName || '-' }}
          </q-td>
          <q-td key="state">
            <q-badge v-if="props.row.state" :color="getStateColor(props.row.state)" :label="props.row.state"/>
            <span v-else class="text-grey-6">-</span>
          </q-td>
          <q-td key="tsCreated">
            {{ formatDate(props.row.tsCreated) }}
          </q-td>
          <q-td key="tsUpdated">
            {{ formatDate(props.row.tsUpdated) }}
          </q-td>
          <q-td key="actions">
            <q-btn-group>
              <!-- Play/Pause Button -->
              <q-btn 
                v-if="props.row.state === 'ACTIVE'" 
                icon="mdi-pause" 
                color="orange-7" 
                @click.stop="toggleJobSchedule(props.row, false)" 
                flat
              >
                <q-tooltip>Pause Job</q-tooltip>
              </q-btn>
              <q-btn 
                v-else 
                icon="mdi-play" 
                color="green-7" 
                @click.stop="toggleJobSchedule(props.row, true)" 
                flat
              >
                <q-tooltip>Start Job</q-tooltip>
              </q-btn>
              
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

import {JOB_DELETE_BY_ID, JOB_LIST_ALL, JOB_SCHEDULE, JOB_UNSCHEDULE} from "@/graphql/queries";

import _ from "lodash";

export default defineComponent({
  name: 'JobList',
  setup() {
    const uri = '/admin/jobs';
    const $q = useQuasar();
    const {client} = useApolloClient();
    const loading = ref(false);
    const router = useRouter();
    const rows = ref([]);
    const filter = ref('');
    const columns = [
      {name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true},
      {name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true},
      {name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true},
      {name: 'scenario', label: 'Scenario', field: 'scenarioName', align: 'left', sortable: true},
      {name: 'state', label: 'State', field: 'state', align: 'left', sortable: true},
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

    const getStateColor = (state) => {
      const stateColors = {
        'ACTIVE': 'positive',
        'INACTIVE': 'grey',
        'PAUSED': 'warning',
        'ERROR': 'negative',
        'DISABLED': 'grey-5'
      };
      return stateColors[state] || 'grey';
    };

    const formatDate = (dateString) => {
      if (!dateString) return '-';
      const date = new Date(dateString);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    };

    const fetchData = () => {
      loading.value = true;
      client.query({
        query: JOB_LIST_ALL,
        variables: {},
        fetchPolicy: 'network-only',
      }).then(response => {
        rows.value = _.transform(response.data.jobList,
          function (result, value) {
            let job = {
              id: value.id,
              name: value.name || 'Unnamed',
              description: value.description || '',
              scenarioName: value.scenario ? value.scenario.name : '',
              state: value.state,
              tsCreated: value.tsCreated,
              tsUpdated: value.tsUpdated
            };
            result.push(job);
          }, []);
        loading.value = false;
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load jobs',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching jobs:', error);
      });
    };

    const removeItem = (toDelete) => {
      $q.dialog({
        title: 'Confirm Deletion',
        message: `Are you sure you want to delete job "${toDelete.name}"?`,
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
          mutation: JOB_DELETE_BY_ID,
          variables: {id: toDelete.id},
          fetchPolicy: 'no-cache',
          update: () => {
            // Prevent Apollo from processing the mutation result
          }
        }).then(response => {
          if (response.data.jobDelete.success) {
            $q.notify({
              color: 'positive',
              message: 'Job deleted successfully',
              icon: 'mdi-check-circle',
              position: 'top'
            });
            fetchData();
          } else {
            $q.notify({
              color: 'negative',
              message: response.data.jobDelete.error || 'Failed to delete job',
              icon: 'mdi-alert-circle',
              position: 'top'
            });
          }
        }).catch(error => {
          $q.notify({
            color: 'negative',
            message: 'Failed to delete job',
            icon: 'mdi-alert-circle',
            position: 'top'
          });
          console.error('Error deleting job:', error);
        });
      });
    };

    const toggleJobSchedule = (job, shouldSchedule) => {
      const action = shouldSchedule ? 'schedule' : 'unschedule';
      const mutation = shouldSchedule ? JOB_SCHEDULE : JOB_UNSCHEDULE;
      const actionLabel = shouldSchedule ? 'Start' : 'Pause';
      
      client.mutate({
        mutation: mutation,
        variables: {jobId: job.id},
        fetchPolicy: 'no-cache',
      }).then(response => {
        const result = response.data[`job${action.charAt(0).toUpperCase()}${action.slice(1)}`];
        if (result.success) {
          $q.notify({
            color: 'positive',
            message: `Job ${actionLabel.toLowerCase()}ed successfully`,
            icon: shouldSchedule ? 'mdi-play-circle' : 'mdi-pause-circle',
            position: 'top'
          });
          fetchData(); // Refresh the list
        } else {
          $q.notify({
            color: 'negative',
            message: result.error || `Failed to ${action} job`,
            icon: 'mdi-alert-circle',
            position: 'top'
          });
        }
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: `Failed to ${action} job`,
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error(`Error ${action}ing job:`, error);
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
      toggleJobSchedule,
      uri,
      getStateColor,
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

