<template>
  <q-btn 
    size="sm" 
    flat 
    round 
    class="text-white" 
    icon="mdi-history" 
    @click="openLog"
  >
    <q-tooltip>Event History</q-tooltip>
  </q-btn>
  
  <q-dialog 
    v-model="visible" 
    transition-show="jump-up" 
    transition-hide="jump-down"
    :maximized="$q.platform.is.mobile"
  >
    <q-card class="bg-white" :style="cardStyle">
      <!-- Header -->
      <q-bar class="bg-green-5 text-white">
        <q-icon name="mdi-history"/>
        <div>Event History - {{ peripheral?.name || 'Peripheral' }}</div>
        <q-space/>
        <q-btn 
          dense 
          flat 
          icon="mdi-refresh" 
          @click="loadEvents"
          :loading="loading"
        >
          <q-tooltip>Refresh</q-tooltip>
        </q-btn>
        <q-btn dense flat icon="close" v-close-popup>
          <q-tooltip>Close</q-tooltip>
        </q-btn>
      </q-bar>

      <!-- Loading State -->
      <q-inner-loading :showing="loading && events.length === 0">
        <q-spinner-gears size="50px" color="primary"/>
      </q-inner-loading>

      <!-- Content -->
      <q-card-section v-if="!loading || events.length > 0">
        <!-- Filters and Info -->
        <div class="row items-center q-mb-md">
          <div class="col">
            <div class="text-h6 text-grey-8">
              <q-icon name="mdi-format-list-bulleted" class="q-mr-xs"/>
              Events ({{ events.length }})
            </div>
            <div class="text-caption text-grey-6">
              Showing last {{ pageSize }} events
            </div>
          </div>
          <div class="col-auto">
            <q-select
              v-model="pageSize"
              :options="pageSizeOptions"
              label="Show"
              dense
              outlined
              style="min-width: 100px"
              @update:model-value="loadEvents"
            />
          </div>
        </div>

        <!-- Events Table -->
        <q-table 
          :rows="events" 
          :columns="columns" 
          row-key="id"
          flat
          bordered
          dense
          :rows-per-page-options="[0]"
          hide-pagination
          :loading="loading"
          class="event-table"
        >
          <template v-slot:body-cell-date="props">
            <q-td :props="props">
              <div class="text-body2">{{ props.row.strDate }}</div>
              <div class="text-caption text-grey-6">{{ props.row.relativeTime }}</div>
            </q-td>
          </template>

          <template v-slot:body-cell-value="props">
            <q-td :props="props">
              <q-badge 
                :color="getValueColor(props.row.p4)" 
                :label="props.row.p4"
              />
            </q-td>
          </template>

          <template v-slot:body-cell-source="props">
            <q-td :props="props">
              <q-chip 
                size="sm" 
                :icon="getSourceIcon(props.row.p3)"
                :color="getSourceColor(props.row.p3)"
                text-color="white"
              >
                {{ props.row.p3 }}
              </q-chip>
            </q-td>
          </template>

          <template v-slot:body-cell-context="props">
            <q-td :props="props">
              <div class="text-caption text-grey-8">
                {{ props.row.p6 || '-' }}
              </div>
            </q-td>
          </template>

          <template v-slot:no-data>
            <div class="full-width row flex-center text-grey-6 q-py-lg">
              <q-icon name="mdi-history" size="3em" class="q-mr-md"/>
              <span class="text-subtitle1">No events found</span>
            </div>
          </template>

          <template v-slot:loading>
            <q-inner-loading showing color="primary"/>
          </template>
        </q-table>
      </q-card-section>

      <!-- Actions -->
      <q-card-actions align="right" class="q-px-md q-pb-md">
        <q-btn 
          label="Export CSV" 
          icon="mdi-download" 
          color="primary" 
          flat 
          @click="exportToCSV"
          :disable="events.length === 0"
        />
        <q-btn label="Close" color="grey-7" flat v-close-popup/>
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { useQuasar } from 'quasar';
import { format, formatDistanceToNow } from 'date-fns';
import { PERIPHERAl_EVENT_LOGS } from '@/graphql/queries';

// Props
const props = defineProps({
  peripheral: {
    type: Object,
    required: true
  }
});

// Composables
const { client } = useApolloClient();
const $q = useQuasar();

// Constants
const DATE_FORMAT = 'dd/MM/yyyy HH:mm:ss';
const PAGE_SIZE_OPTIONS = [10, 25, 50, 100];

// State
const visible = ref(false);
const loading = ref(false);
const events = ref([]);
const pageSize = ref(10);

// Computed
const pageSizeOptions = computed(() => PAGE_SIZE_OPTIONS);

const cardStyle = computed(() => {
  return $q.platform.is.mobile ? '' : 'width: 800px; max-width: 90vw;';
});

const columns = [
  {
    name: 'date',
    required: true,
    label: 'Date & Time',
    align: 'left',
    field: 'strDate',
    sortable: true,
    style: 'width: 200px'
  },
  {
    name: 'value',
    required: true,
    label: 'Value',
    align: 'center',
    field: 'p4',
    sortable: true,
    style: 'width: 100px'
  },
  {
    name: 'source',
    required: true,
    label: 'Source',
    align: 'center',
    field: 'p3',
    sortable: true,
    style: 'width: 150px'
  },
  {
    name: 'context',
    required: true,
    label: 'Context',
    align: 'left',
    field: 'p6',
    sortable: true
  }
];

/**
 * Get color for event value badge
 */
const getValueColor = (value) => {
  if (!value) return 'grey';
  
  const valueLower = String(value).toLowerCase();
  
  if (valueLower === 'on' || valueLower === 'true' || valueLower === '1') {
    return 'positive';
  }
  if (valueLower === 'off' || valueLower === 'false' || valueLower === '0') {
    return 'negative';
  }
  if (valueLower === 'error' || valueLower === 'fail') {
    return 'negative';
  }
  if (valueLower === 'warning' || valueLower === 'warn') {
    return 'warning';
  }
  
  return 'primary';
};

/**
 * Get color for event source chip
 */
const getSourceColor = (source) => {
  if (!source) return 'grey';
  
  const sourceLower = String(source).toLowerCase();
  
  if (sourceLower.includes('user') || sourceLower.includes('manual')) {
    return 'blue-7';
  }
  if (sourceLower.includes('system') || sourceLower.includes('auto')) {
    return 'purple-7';
  }
  if (sourceLower.includes('mqtt') || sourceLower.includes('device')) {
    return 'green-7';
  }
  if (sourceLower.includes('telegram') || sourceLower.includes('bot')) {
    return 'cyan-7';
  }
  if (sourceLower.includes('schedule') || sourceLower.includes('timer')) {
    return 'orange-7';
  }
  
  return 'grey-7';
};

/**
 * Get icon for event source
 */
const getSourceIcon = (source) => {
  if (!source) return 'mdi-help-circle';
  
  const sourceLower = String(source).toLowerCase();
  
  if (sourceLower.includes('user') || sourceLower.includes('manual')) {
    return 'mdi-account';
  }
  if (sourceLower.includes('system') || sourceLower.includes('auto')) {
    return 'mdi-cog';
  }
  if (sourceLower.includes('mqtt') || sourceLower.includes('device')) {
    return 'mdi-router-wireless';
  }
  if (sourceLower.includes('telegram') || sourceLower.includes('bot')) {
    return 'mdi-send';
  }
  if (sourceLower.includes('schedule') || sourceLower.includes('timer')) {
    return 'mdi-calendar-clock';
  }
  
  return 'mdi-information';
};

/**
 * Load events from the server
 */
const loadEvents = async () => {
  if (!props.peripheral?.id) {
    $q.notify({
      color: 'warning',
      message: 'Peripheral ID is required',
      icon: 'mdi-alert',
      position: 'top'
    });
    return;
  }

  loading.value = true;

  try {
    const response = await client.query({
      query: PERIPHERAl_EVENT_LOGS,
      variables: {
        p2: String(props.peripheral.id),
        count: pageSize.value,
        offset: 0
      },
      fetchPolicy: 'network-only'
    });

    // Process events
    events.value = response.data.eventsByP2.map(event => {
      const eventDate = new Date(event.tsCreated);
      
      return {
        ...event,
        strDate: format(eventDate, DATE_FORMAT),
        relativeTime: formatDistanceToNow(eventDate, { addSuffix: true })
      };
    });
  } catch (error) {
    console.error('Error loading events:', error);
    $q.notify({
      color: 'negative',
      message: 'Failed to load event history',
      icon: 'mdi-alert-circle',
      position: 'top'
    });
  } finally {
    loading.value = false;
  }
};

/**
 * Export events to CSV
 */
const exportToCSV = () => {
  if (events.value.length === 0) {
    return;
  }

  try {
    // CSV headers
    const headers = ['Date', 'Value', 'Source', 'Context'];
    
    // CSV rows
    const rows = events.value.map(event => [
      event.strDate,
      event.p4 || '',
      event.p3 || '',
      event.p6 || ''
    ]);

    // Combine headers and rows
    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
    ].join('\n');

    // Create blob and download
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    
    link.setAttribute('href', url);
    link.setAttribute('download', `events_${props.peripheral.name || props.peripheral.id}_${format(new Date(), 'yyyyMMdd_HHmmss')}.csv`);
    link.style.visibility = 'hidden';
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    $q.notify({
      color: 'positive',
      message: 'Events exported successfully',
      icon: 'mdi-check-circle',
      position: 'top'
    });
  } catch (error) {
    console.error('Error exporting events:', error);
    $q.notify({
      color: 'negative',
      message: 'Failed to export events',
      icon: 'mdi-alert-circle',
      position: 'top'
    });
  }
};

/**
 * Open the event log dialog
 */
const openLog = () => {
  visible.value = true;
  loadEvents();
};
</script>

<style scoped>
.event-table {
  border: 1px solid #e0e0e0;
  border-radius: 4px;
}

.event-table :deep(thead tr th) {
  background-color: #f5f5f5;
  font-weight: 600;
}

.event-table :deep(tbody tr:hover) {
  background-color: #f9f9f9;
}

.event-table :deep(.q-table__bottom) {
  border-top: 1px solid #e0e0e0;
}
</style>

