<template>
  <q-page padding>
    <q-card flat bordered>
      <!-- Header Section -->
      <q-card-section>
        <div class="row items-center">
          <div class="text-h5 text-primary">
            <q-icon name="mdi-calendar-clock" class="q-mr-sm"/>
            Job List
          </div>
          <q-space/>
          <q-input 
            v-model="filter" 
            dense 
            outlined
            debounce="300" 
            placeholder="Search jobs..."
            clearable
            class="q-mr-sm"
            style="min-width: 250px"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-magnify"/>
            </template>
          </q-input>
          <q-btn
            color="primary"
            icon="mdi-plus-circle"
            label="Add Job"
            @click="createItem"
            :disable="loading"
          />
        </div>
      </q-card-section>

      <!-- Table Section -->
      <q-table
        :rows="filteredItems"
        :columns="columns"
        :loading="loading"
        row-key="id"
        flat
        virtual-scroll
        :rows-per-page-options="[0]"
        hide-pagination
        style="max-height: calc(100vh - 250px)"
        class="sticky-header-table"
        @row-click="(evt, row) => viewItem(row)"
      >
        <template v-slot:body-cell-name="props">
          <q-td :props="props">
            <q-badge color="primary" :label="props.row.name || 'Unnamed'"/>
          </q-td>
        </template>

        <template v-slot:body-cell-description="props">
          <q-td :props="props">
            <div class="text-grey-8">
              {{ props.row.description || '-' }}
            </div>
          </q-td>
        </template>

        <template v-slot:body-cell-scenario="props">
          <q-td :props="props">
            <q-badge 
              v-if="props.row.scenarioName" 
              color="secondary" 
              :label="props.row.scenarioName"
            />
            <span v-else class="text-grey-6">-</span>
          </q-td>
        </template>

        <template v-slot:body-cell-state="props">
          <q-td :props="props">
            <q-badge 
              v-if="props.row.state" 
              :color="getStateColor(props.row.state)" 
              :label="props.row.state"
            />
            <span v-else class="text-grey-6">-</span>
          </q-td>
        </template>

        <template v-slot:body-cell-tsCreated="props">
          <q-td :props="props">
            {{ formatDate(props.row.tsCreated) }}
          </q-td>
        </template>

        <template v-slot:body-cell-tsUpdated="props">
          <q-td :props="props">
            {{ formatDate(props.row.tsUpdated) }}
          </q-td>
        </template>

        <template v-slot:body-cell-actions="props">
          <q-td :props="props">
            <q-btn-group>
              <!-- Play/Pause Toggle Button -->
              <q-btn 
                :icon="props.row.state === 'ACTIVE' ? 'mdi-power' : 'mdi-power-off'" 
                :color="props.row.state === 'ACTIVE' ? 'green-7' : 'grey-7'" 
                @click.stop="toggleJobSchedule(props.row, props.row.state !== 'ACTIVE')" 
                flat
                dense
              >
                <q-tooltip>{{ props.row.state === 'ACTIVE' ? 'Pause Job' : 'Start Job' }}</q-tooltip>
              </q-btn>
              
              <!-- Run Button -->
              <q-btn 
                icon="mdi-play-circle" 
                color="blue-7" 
                @click.stop="runJob(props.row)" 
                flat
                dense
                :disable="props.row.state !== 'ACTIVE'"
              >
                <q-tooltip>Run Job Now</q-tooltip>
              </q-btn>
              
              <q-btn 
                icon="mdi-eye" 
                color="blue-6" 
                @click.stop="viewItem(props.row)" 
                flat
                dense
              >
                <q-tooltip>View</q-tooltip>
              </q-btn>
              <q-btn 
                icon="mdi-pencil" 
                color="amber-7" 
                @click.stop="editItem(props.row)" 
                flat
                dense
              >
                <q-tooltip>Edit</q-tooltip>
              </q-btn>
              <q-btn 
                icon="mdi-delete" 
                color="red-7" 
                @click.stop="deleteItem(props.row)" 
                flat
                dense
              >
                <q-tooltip>Delete</q-tooltip>
              </q-btn>
            </q-btn-group>
          </q-td>
        </template>
      </q-table>
    </q-card>
  </q-page>
</template>

<script>
import { defineComponent, onMounted } from 'vue';
import { format } from 'date-fns';
import { useQuasar } from 'quasar';
import { useApolloClient } from '@vue/apollo-composable';
import { useEntityList } from '@/composables';
import { JOB_DELETE_BY_ID, JOB_LIST_ALL, JOB_SCHEDULE, JOB_UNSCHEDULE, JOB_TRIGGER } from '@/graphql/queries';

export default defineComponent({
  name: 'JobList',
  setup() {
    const $q = useQuasar();
    const { client } = useApolloClient();
    
    const columns = [
      { name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true },
      { name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true },
      { name: 'description', label: 'Description', field: 'description', align: 'left', sortable: true },
      { name: 'scenario', label: 'Scenario', field: 'scenarioName', align: 'left', sortable: true },
      { name: 'state', label: 'State', field: 'state', align: 'left', sortable: true },
      { name: 'tsCreated', label: 'Created', field: 'tsCreated', align: 'left', sortable: true },
      { name: 'tsUpdated', label: 'Updated', field: 'tsUpdated', align: 'left', sortable: true },
      { 
        name: 'actions', 
        label: 'Actions', 
        field: () => '', 
        align: 'right', 
        sortable: false,
        headerClasses: 'bg-grey-2',
        classes: 'bg-grey-1',
        headerStyle: 'position: sticky; right: 0; z-index: 1',
        style: 'position: sticky; right: 0'
      }
    ];

    /**
     * Get color for job state
     */
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

    /**
     * Format date for display
     */
    const formatDate = (dateString) => {
      if (!dateString) return '-';
      try {
        return format(new Date(dateString), 'MMM dd, yyyy HH:mm');
      } catch (error) {
        return '-';
      }
    };

    const {
      filteredItems,
      loading,
      filter,
      fetchList,
      viewItem,
      editItem,
      createItem,
      deleteItem
    } = useEntityList({
      entityName: 'Job',
      entityPath: '/admin/jobs',
      listQuery: JOB_LIST_ALL,
      deleteMutation: JOB_DELETE_BY_ID,
      deleteKey: 'jobDelete',
      columns,
      transformAfterLoad: (job) => ({
        id: job.id,
        name: job.name || 'Unnamed',
        description: job.description ? (job.description.length > 100 ? job.description.substring(0, 100) + '...' : job.description) : '-',
        scenarioName: job.scenario?.name || null,
        state: job.state,
        tsCreated: job.tsCreated,
        tsUpdated: job.tsUpdated
      })
    });

    /**
     * Toggle job schedule (play/pause)
     */
    const toggleJobSchedule = (job, shouldSchedule) => {
      const action = shouldSchedule ? 'schedule' : 'unschedule';
      const mutation = shouldSchedule ? JOB_SCHEDULE : JOB_UNSCHEDULE;
      const actionLabel = shouldSchedule ? 'Start' : 'Pause';
      
      client.mutate({
        mutation: mutation,
        variables: { jobId: job.id },
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
          fetchList(); // Refresh the list
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

    /**
     * Run job immediately
     */
    const runJob = (job) => {
      if (job.state !== 'ACTIVE') {
        $q.notify({
          color: 'warning',
          message: 'Job must be ACTIVE to run',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        return;
      }

      client.mutate({
        mutation: JOB_TRIGGER,
        variables: { jobId: job.id },
        fetchPolicy: 'no-cache',
      }).then(response => {
        const result = response.data.jobTrigger;
        if (result.success) {
          $q.notify({
            color: 'positive',
            message: 'Job triggered successfully',
            icon: 'mdi-play-circle',
            position: 'top'
          });
        } else {
          $q.notify({
            color: 'negative',
            message: result.error || 'Failed to trigger job',
            icon: 'mdi-alert-circle',
            position: 'top'
          });
        }
      }).catch(error => {
        $q.notify({
          color: 'negative',
          message: 'Failed to trigger job',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error triggering job:', error);
      });
    };

    onMounted(() => {
      fetchList();
    });

    return {
      filteredItems,
      columns,
      loading,
      filter,
      viewItem,
      editItem,
      createItem,
      deleteItem,
      toggleJobSchedule,
      runJob,
      getStateColor,
      formatDate
    };
  }
});
</script>

