<template>
  <q-page padding>
    <q-card class="my-card" v-if="viewItem">
      <q-card-section>
        <q-avatar size="103px" class="absolute-center shadow-10">
          <q-icon name="mdi-briefcase-clock" size="xl"/>
        </q-avatar>
        <q-btn flat color="secondary" @click="$router.go(-1)" align="right" label="Back" icon="mdi-arrow-left"/>
      </q-card-section>

      <q-card-section class="row items-center">
        <div class="text-h4 text-secondary">{{ viewItem.name || 'Unnamed Job' }}</div>
        <q-space/>
        <q-btn outline round color="amber-8" icon="mdi-pencil" :to="uri +'/'+ $route.params.idPrimary+'/edit'">
          <q-tooltip>Edit Job</q-tooltip>
        </q-btn>
        <q-btn outline round color="amber-8" icon="mdi-view-list" 
               :to="'/admin/configurations/'+ $route.params.idPrimary+'?type=JOB'" class="q-ml-sm">
          <q-tooltip>View Configurations</q-tooltip>
        </q-btn>
      </q-card-section>

      <q-separator/>

      <!-- Job Information -->
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
              <q-icon name="mdi-state-machine" class="q-mr-sm"/>
              State
            </q-item-label>
            <q-item-label caption class="text-body2">
              <q-badge v-if="viewItem.state" :color="getStateColor(viewItem.state)" :label="viewItem.state"/>
              <span v-else class="text-grey-6">Not specified</span>
            </q-item-label>
          </q-item-section>
        </q-item>

        <q-item v-if="viewItem.scenario">
          <q-item-section>
            <q-item-label class="text-h6">
              <q-icon name="mdi-script-text" class="q-mr-sm"/>
              Scenario
            </q-item-label>
            <q-item-label caption class="text-body2">
              {{ viewItem.scenario.name }}
              <q-btn icon="mdi-eye" :to="'/admin/scenarios/'+viewItem.scenario.id+'/view'" size="xs" flat color="blue-6" class="q-ml-sm">
                <q-tooltip>View Scenario</q-tooltip>
              </q-btn>
            </q-item-label>
          </q-item-section>
        </q-item>


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

      <!-- Cron Triggers -->
      <div class="q-pa-md" v-if="viewItem.cronTriggers && viewItem.cronTriggers.length > 0">
        <div class="text-h6 q-mb-md">
          <q-icon name="mdi-clock-outline" class="q-mr-sm"/>
          Cron Triggers
          <div class="text-caption text-grey-6 text-weight-normal">Expressions are in UTC timezone</div>
        </div>
        <q-list bordered separator>
          <q-item v-for="trigger in viewItem.cronTriggers" :key="trigger.id">
            <q-item-section>
              <q-item-label v-if="trigger.description" class="text-weight-medium q-mb-xs">
                {{ trigger.description }}
              </q-item-label>
              <q-item-label>
                <q-badge color="info" :label="trigger.expression"/>
                <span class="q-ml-md text-grey-7">
                  Next run: <span class="text-primary text-weight-medium">{{ getNextRunTime(trigger.expression) }}</span>
                  <span class="text-caption">(local time)</span>
                </span>
              </q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </div>

      <!-- Event Triggers -->
      <div class="q-pa-md" v-if="viewItem.eventTriggers && viewItem.eventTriggers.length > 0">
        <div class="text-h6 q-mb-md">
          <q-icon name="mdi-bell-ring" class="q-mr-sm"/>
          Event Triggers
        </div>
        <q-list bordered separator>
          <q-item v-for="trigger in viewItem.eventTriggers" :key="trigger.id">
            <q-item-section>
              <q-item-label>
                <q-badge color="warning" :label="trigger.eventDefinition ? trigger.eventDefinition.name : 'Unknown Event'"/>
              </q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </div>

      <!-- Tags -->
      <div class="q-pa-md" v-if="viewItem.tags && viewItem.tags.length > 0">
        <div class="text-h6 q-mb-md">
          <q-icon name="mdi-tag-multiple" class="q-mr-sm"/>
          Tags
        </div>
        <div class="q-gutter-sm">
          <q-chip v-for="tag in viewItem.tags" :key="tag.id" color="primary" text-color="white" icon="mdi-tag">
            {{ tag.name }}
          </q-chip>
        </div>
      </div>

      <q-separator/>

      <!-- Execution History -->
      <div class="q-pa-md">
        <div class="text-h6 q-mb-md row items-center">
          <q-icon name="mdi-history" class="q-mr-sm"/>
          Execution History
          <q-space/>
          <q-btn 
            flat 
            dense 
            icon="mdi-refresh" 
            color="primary" 
            @click="fetchExecutionHistory"
            :loading="historyLoading"
          >
            <q-tooltip>Refresh history</q-tooltip>
          </q-btn>
        </div>
        
        <q-table
          :rows="executionHistory"
          :columns="historyColumns"
          row-key="id"
          :loading="historyLoading"
          :pagination="historyPagination"
          dense
          flat
          bordered
          :rows-per-page-options="[10, 25, 50]"
        >
          <!-- Status column with colored badge -->
          <template v-slot:body-cell-status="props">
            <q-td :props="props">
              <q-badge 
                :color="getExecutionStatusColor(props.row.status)" 
                :label="props.row.status"
              />
            </q-td>
          </template>
          
          <!-- Start time column formatted -->
          <template v-slot:body-cell-startTime="props">
            <q-td :props="props">
              {{ formatDate(props.row.startTime) }}
            </q-td>
          </template>
          
          <!-- Duration column formatted -->
          <template v-slot:body-cell-durationMs="props">
            <q-td :props="props">
              <span v-if="props.row.durationMs !== null">
                {{ formatDuration(props.row.durationMs) }}
              </span>
              <span v-else class="text-grey-5">-</span>
            </q-td>
          </template>
          
          <!-- Error message column with tooltip for long messages -->
          <template v-slot:body-cell-errorMessage="props">
            <q-td :props="props">
              <span v-if="props.row.errorMessage" class="text-negative cursor-pointer">
                {{ truncateText(props.row.errorMessage, 50) }}
                <q-tooltip v-if="props.row.errorMessage.length > 50" max-width="400px">
                  {{ props.row.errorMessage }}
                </q-tooltip>
              </span>
              <span v-else class="text-grey-5">-</span>
            </q-td>
          </template>
          
          <!-- Trigger column -->
          <template v-slot:body-cell-triggerName="props">
            <q-td :props="props">
              <template v-if="props.row.triggerName">
                <template v-if="getTriggerInfo(props.row.triggerName)">
                  <span v-if="getTriggerInfo(props.row.triggerName).description" class="text-weight-medium">
                    {{ getTriggerInfo(props.row.triggerName).description }}
                  </span>
                  <span v-else>
                    {{ getTriggerInfo(props.row.triggerName).expression }}
                  </span>
                  <q-tooltip max-width="300px">
                    <div><strong>Expression:</strong> {{ getTriggerInfo(props.row.triggerName).expression }}</div>
                    <div v-if="getTriggerInfo(props.row.triggerName).description">
                      <strong>Description:</strong> {{ getTriggerInfo(props.row.triggerName).description }}
                    </div>
                  </q-tooltip>
                </template>
                <span v-else>{{ props.row.triggerName }}</span>
              </template>
              <span v-else class="text-grey-5">Manual/Unknown</span>
            </q-td>
          </template>
          
          <!-- No data message -->
          <template v-slot:no-data>
            <div class="full-width row flex-center text-grey-6 q-pa-md">
              <q-icon name="mdi-information-outline" size="sm" class="q-mr-sm"/>
              No execution history found for this job
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
import { Cron } from 'croner';

import {JOB_GET_BY_ID, JOB_EXECUTION_HISTORY} from "@/graphql/queries";

export default defineComponent({
  name: 'JobView',
  setup() {
    const uri = '/admin/jobs';
    const $q = useQuasar();
    const viewItem = ref();
    const loading = ref(false);
    const {client} = useApolloClient();
    const router = useRouter();
    const route = useRoute();
    
    // Execution history state
    const executionHistory = ref([]);
    const historyLoading = ref(false);
    const historyPagination = ref({
      sortBy: 'startTime',
      descending: true,
      page: 1,
      rowsPerPage: 10
    });
    
    // Execution history table columns
    const historyColumns = [
      {
        name: 'status',
        label: 'Status',
        field: 'status',
        align: 'left',
        sortable: true
      },
      {
        name: 'startTime',
        label: 'Start Time',
        field: 'startTime',
        align: 'left',
        sortable: true
      },
      {
        name: 'durationMs',
        label: 'Duration',
        field: 'durationMs',
        align: 'left',
        sortable: true
      },
      {
        name: 'triggerName',
        label: 'Trigger',
        field: 'triggerName',
        align: 'left',
        sortable: true
      },
      {
        name: 'errorMessage',
        label: 'Error',
        field: 'errorMessage',
        align: 'left',
        sortable: false
      }
    ];

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
    
    const getExecutionStatusColor = (status) => {
      const statusColors = {
        'COMPLETED': 'positive',
        'FAILED': 'negative',
        'STARTED': 'info',
        'VETOED': 'warning'
      };
      return statusColors[status] || 'grey';
    };

    const formatDate = (dateString) => {
      if (!dateString) return '-';
      const date = new Date(dateString);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    };
    
    const formatDuration = (ms) => {
      if (ms === null || ms === undefined) return '-';
      if (ms < 1000) return `${ms}ms`;
      if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`;
      const minutes = Math.floor(ms / 60000);
      const seconds = ((ms % 60000) / 1000).toFixed(0);
      return `${minutes}m ${seconds}s`;
    };
    
    const truncateText = (text, maxLength) => {
      if (!text) return '';
      if (text.length <= maxLength) return text;
      return text.substring(0, maxLength) + '...';
    };
    
    /**
     * Get trigger display info from trigger name
     * Trigger name format is "trigger_{id}" where id is the CronTrigger id
     * Returns object with description and expression if found
     */
    const getTriggerInfo = (triggerName) => {
      if (!triggerName || !viewItem.value?.cronTriggers) {
        return null;
      }
      
      // Extract trigger ID from name format "trigger_{id}"
      const match = triggerName.match(/^trigger_(\d+)$/);
      if (!match) {
        return null;
      }
      
      const triggerId = match[1];
      const cronTrigger = viewItem.value.cronTriggers.find(t => String(t.id) === triggerId);
      
      if (cronTrigger) {
        return {
          description: cronTrigger.description,
          expression: cronTrigger.expression
        };
      }
      
      return null;
    };

    const getNextRunTime = (cronExpression) => {
      try {
        if (!cronExpression) return '-';
        
        // Quartz format has 6-7 fields: second minute hour day month weekday (year)
        // Standard cron has 5 fields: minute hour day month weekday
        // Check field count and convert if needed
        const fields = cronExpression.trim().split(/\s+/);
        
        let cronForCroner = cronExpression;
        
        // If it's a 5-field expression, add seconds field at the beginning for Quartz format
        if (fields.length === 5) {
          cronForCroner = '0 ' + cronExpression;
        }
        // If it's 7 fields (with year), remove the year field as croner doesn't support it
        else if (fields.length === 7) {
          cronForCroner = fields.slice(0, 6).join(' ');
        }
        
        // Parse cron expression in UTC timezone (cron expressions are in UTC)
        // The cron job will be scheduled based on UTC time
        const job = new Cron(cronForCroner, { timezone: 'UTC' });
        const nextRun = job.nextRun();
        
        if (!nextRun) return 'No upcoming runs';
        
        // Convert UTC time to browser local time for display
        // toLocaleString automatically converts from the Date object's UTC representation
        // to the user's browser local timezone
        return nextRun.toLocaleString('en-GB', {
          day: '2-digit',
          month: '2-digit',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
          hour12: false
        });
      } catch (error) {
        console.error('Error parsing cron expression:', cronExpression, error);
        return 'Invalid cron expression';
      }
    };

    const fetchData = () => {
      loading.value = true;
      client.query({
        query: JOB_GET_BY_ID,
        variables: {id: route.params.idPrimary},
        fetchPolicy: 'network-only',
      }).then(response => {
        viewItem.value = response.data.job;
        loading.value = false;
        // Also fetch execution history
        fetchExecutionHistory();
      }).catch(error => {
        loading.value = false;
        $q.notify({
          color: 'negative',
          message: 'Failed to load job details',
          icon: 'mdi-alert-circle',
          position: 'top'
        });
        console.error('Error fetching job:', error);
      });
    };
    
    const fetchExecutionHistory = () => {
      historyLoading.value = true;
      client.query({
        query: JOB_EXECUTION_HISTORY,
        variables: {
          jobId: route.params.idPrimary,
          limit: 50
        },
        fetchPolicy: 'network-only',
      }).then(response => {
        executionHistory.value = response.data.jobExecutionHistoryByJobId || [];
        historyLoading.value = false;
      }).catch(error => {
        historyLoading.value = false;
        console.error('Error fetching execution history:', error);
        // Don't show error notification - history is optional
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
      getStateColor,
      formatDate,
      getNextRunTime,
      // Execution history
      executionHistory,
      historyLoading,
      historyColumns,
      historyPagination,
      fetchExecutionHistory,
      getExecutionStatusColor,
      formatDuration,
      truncateText,
      getTriggerInfo
    };
  }
});

</script>

