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
    @escape-key="visible = false"
  >
    <q-card class="event-logger-card" :style="cardStyle">
      <!-- Header -->
      <q-bar class="event-logger-header">
        <q-icon name="mdi-history" size="20px"/>
        <div class="header-title">Event History - {{ peripheral?.name || 'Peripheral' }}</div>
        <q-space/>
        <q-btn 
          dense 
          flat 
          round
          :icon="includePortEvents ? 'mdi-check-circle' : 'mdi-circle-outline'" 
          @click="togglePortEvents"
          :class="includePortEvents ? 'toggle-active' : 'toggle-inactive'"
        >
          <q-tooltip>{{ includePortEvents ? 'Exclude' : 'Include' }} Port Events</q-tooltip>
        </q-btn>
        <q-btn 
          dense 
          flat 
          round
          icon="mdi-refresh" 
          @click="loadEvents"
          :loading="loading"
        >
          <q-tooltip>Refresh</q-tooltip>
        </q-btn>
        <q-btn dense flat round icon="mdi-close" v-close-popup>
          <q-tooltip>Close</q-tooltip>
        </q-btn>
      </q-bar>

      <!-- Loading State -->
      <q-inner-loading :showing="loading && events.length === 0">
        <q-spinner-gears size="50px" color="primary"/>
      </q-inner-loading>

      <!-- Content -->
      <q-card-section class="event-content-wrapper">
        <!-- Filters and Info -->
        <div class="event-info-header">
          <div class="event-info">
            <div class="event-count">
              <q-icon name="mdi-format-list-bulleted" size="18px" class="q-mr-xs"/>
              <span class="count-number">{{ events.length }}</span>
              <span class="count-label">Events</span>
            </div>
            <div class="event-details">
              Showing last {{ pageSize }} events
              <span v-if="includePortEvents" class="port-info">
                <q-icon name="mdi-information" size="14px"/>
                {{ props.peripheral.connectedTo?.length || 0 }} port{{ (props.peripheral.connectedTo?.length || 0) !== 1 ? 's' : '' }} included
              </span>
            </div>
          </div>
          <div class="page-size-selector">
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

        <!-- Events Table with Scroll -->
        <div class="table-scroll-wrapper">
          <q-table 
            :rows="events" 
            :columns="columns" 
            row-key="id"
            flat
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

          <template v-slot:body-cell-type="props">
            <q-td :props="props">
              <q-chip 
                size="sm" 
                :icon="props.row.p1 === 'PORT' ? 'mdi-electric-switch' : 'mdi-devices'"
                :color="props.row.p1 === 'PORT' ? 'blue-6' : 'green-6'"
                text-color="white"
              >
                {{ props.row.p1 || '-' }}
              </q-chip>
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
            <div class="no-data-wrapper">
              <q-icon name="mdi-history" size="64px" color="grey-4"/>
              <div class="no-data-text">No events found</div>
              <div class="no-data-hint">Try adjusting your filters or check back later</div>
            </div>
          </template>

          <template v-slot:loading>
            <q-inner-loading showing color="primary">
              <q-spinner-dots size="50px" color="primary"/>
              <div class="q-mt-md text-grey-7">Loading events...</div>
            </q-inner-loading>
          </template>
        </q-table>
        </div>
      </q-card-section>

      <!-- Actions -->
      <q-card-actions align="right" class="event-actions">
        <q-btn 
          label="Export CSV" 
          icon="mdi-download" 
          color="primary" 
          unelevated
          @click="exportToCSV"
          :disable="events.length === 0"
          class="export-btn"
        />
        <q-btn 
          label="Close" 
          color="grey-7" 
          flat 
          v-close-popup
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { useQuasar } from 'quasar';
import { format, formatDistanceToNow } from 'date-fns';
import { PERIPHERAl_EVENT_LOGS, PERIPHERAL_EVENT_LOGS_MULTIPLE } from '@/graphql/queries';

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
const includePortEvents = ref(false);

// Computed
const pageSizeOptions = computed(() => PAGE_SIZE_OPTIONS);

const cardStyle = computed(() => {
  return $q.platform.is.mobile ? '' : 'width: 900px; max-width: 95vw; max-height: 85vh;';
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
    name: 'type',
    required: false,
    label: 'Type',
    align: 'center',
    field: 'p1',
    sortable: true,
    style: 'width: 120px'
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
 * Toggle port events inclusion
 */
const togglePortEvents = () => {
  includePortEvents.value = !includePortEvents.value;
  loadEvents();
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
    let response;
    
    if (includePortEvents.value) {
      // Get all connected port IDs
      const portIds = props.peripheral.connectedTo?.map(port => String(port.id)) || [];
      
      // Combine peripheral ID with port IDs
      const p2List = [String(props.peripheral.id), ...portIds];
      
      if (p2List.length === 0) {
        events.value = [];
        return;
      }
      
      // Fetch events for peripheral and all its ports
      response = await client.query({
        query: PERIPHERAL_EVENT_LOGS_MULTIPLE,
        variables: {
          p2List: p2List,
          count: pageSize.value,
          offset: 0
        },
        fetchPolicy: 'network-only'
      });
      
      events.value = response.data.eventsByP2List
        .map(event => {
          const eventDate = new Date(event.tsCreated);
          
          return {
            ...event,
            strDate: format(eventDate, DATE_FORMAT),
            relativeTime: formatDistanceToNow(eventDate, { addSuffix: true }),
            timestamp: eventDate.getTime() // Add numeric timestamp for sorting
          };
        })
        .sort((a, b) => b.timestamp - a.timestamp); // Sort by timestamp descending (newest first)
    } else {
      // Fetch events only for the peripheral
      response = await client.query({
        query: PERIPHERAl_EVENT_LOGS,
        variables: {
          p2: String(props.peripheral.id),
          count: pageSize.value,
          offset: 0
        },
        fetchPolicy: 'network-only'
      });
      
      events.value = response.data.eventsByP2
        .map(event => {
          const eventDate = new Date(event.tsCreated);
          
          return {
            ...event,
            strDate: format(eventDate, DATE_FORMAT),
            relativeTime: formatDistanceToNow(eventDate, { addSuffix: true }),
            timestamp: eventDate.getTime() // Add numeric timestamp for sorting
          };
        })
        .sort((a, b) => b.timestamp - a.timestamp); // Sort by timestamp descending (newest first)
    }
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
    link.remove();

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

<style scoped lang="scss">
/* Card Styling */
.event-logger-card {
  background: #ffffff;
  display: flex;
  flex-direction: column;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
  border-radius: 8px;
  overflow: hidden;
}

/* Header Styling */
.event-logger-header {
  background: linear-gradient(135deg, #15803d 0%, #14532d 100%);
  color: #ffffff;
  padding: 12px 16px;
  min-height: 56px;
  
  .header-title {
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 0.3px;
    color: #ffffff;
  }
  
  .q-btn {
    color: #ffffff;
    transition: all 0.3s ease;
    
    &:hover {
      background: rgba(255, 255, 255, 0.15);
      transform: scale(1.05);
    }
  }
  
  .toggle-active {
    background: rgba(255, 255, 255, 0.2);
    
    &:hover {
      background: rgba(255, 255, 255, 0.3);
    }
  }
  
  .toggle-inactive {
    opacity: 0.7;
  }
}

/* Content Wrapper */
.event-content-wrapper {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  padding: 0 !important;
}

/* Info Header */
.event-info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(to bottom, #f9fafb 0%, #ffffff 100%);
  border-bottom: 2px solid #e5e7eb;
  flex-shrink: 0;
}

.event-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.event-count {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  
  .count-number {
    color: #16a34a;
    font-size: 20px;
    font-weight: 700;
  }
  
  .count-label {
    color: #6b7280;
    font-weight: 500;
  }
}

.event-details {
  font-size: 13px;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 12px;
  
  .port-info {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: #1e3a8a;
    background: #dbeafe;
    padding: 2px 8px;
    border-radius: 12px;
    font-weight: 600;
  }
}

.page-size-selector {
  flex-shrink: 0;
}

/* Table Scroll Wrapper */
.table-scroll-wrapper {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0; /* Important for flexbox scrolling */
}

/* Event Table Styling */
.event-table {
  flex: 1;
  border: none;
  box-shadow: none;
  height: 100%;
  
  :deep(.q-table__middle) {
    flex: 1;
    overflow: auto;
    max-height: 500px; /* Fallback height */
  }
  
  :deep(thead tr th) {
    position: sticky;
    top: 0;
    z-index: 2;
    background: linear-gradient(to bottom, #f3f4f6 0%, #e5e7eb 100%);
    color: #1f2937;
    font-weight: 600;
    font-size: 13px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    padding: 14px 12px;
    border-bottom: 2px solid #d1d5db;
  }
  
  /* Custom Scrollbar */
  :deep(.q-table__middle)::-webkit-scrollbar {
    width: 10px;
    height: 10px;
  }
  
  :deep(.q-table__middle)::-webkit-scrollbar-track {
    background: #f1f5f9;
    border-radius: 10px;
  }
  
  :deep(.q-table__middle)::-webkit-scrollbar-thumb {
    background: linear-gradient(180deg, #94a3b8 0%, #64748b 100%);
    border-radius: 10px;
    
    &:hover {
      background: linear-gradient(180deg, #64748b 0%, #475569 100%);
    }
  }
  
  :deep(tbody) {
    tr {
      transition: all 0.2s ease;
      
      &:nth-child(even) {
        background-color: #f9fafb;
      }
      
      &:hover {
        background-color: #eff6ff !important;
        transform: translateX(2px);
        box-shadow: 0 2px 8px rgba(37, 99, 235, 0.1);
      }
      
      td {
        padding: 12px;
        border-bottom: 1px solid #e5e7eb;
        color: #374151;
        font-size: 13px;
      }
    }
  }
  
  /* Type Column Styling */
  :deep(.q-chip) {
    font-weight: 600;
    font-size: 11px;
    letter-spacing: 0.5px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    
    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
    }
  }
  
  /* Value Badge Styling */
  :deep(.q-badge) {
    font-weight: 600;
    font-size: 12px;
    padding: 6px 12px;
    border-radius: 12px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  }
}

/* No Data State */
.no-data-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
  
  .no-data-text {
    font-size: 18px;
    font-weight: 600;
    color: #6b7280;
  }
  
  .no-data-hint {
    font-size: 14px;
    color: #9ca3af;
  }
}

/* Actions Bar */
.event-actions {
  padding: 12px 20px;
  background: linear-gradient(to top, #f9fafb 0%, #ffffff 100%);
  border-top: 1px solid #e5e7eb;
  gap: 8px;
  flex-shrink: 0;
  
  .export-btn {
    font-weight: 600;
    padding: 8px 20px;
    box-shadow: 0 2px 4px rgba(37, 99, 235, 0.2);
    
    &:hover {
      box-shadow: 0 4px 8px rgba(37, 99, 235, 0.3);
      transform: translateY(-1px);
    }
  }
}

/* Loading Overlay */
:deep(.q-inner-loading) {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(4px);
}

/* Mobile Responsiveness */
@media (max-width: 600px) {
  .event-info-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .page-size-selector {
    width: 100%;
    
    .q-select {
      width: 100%;
    }
  }
  
  .event-table {
    :deep(.q-table__middle) {
      max-height: 400px;
    }
    
    :deep(tbody tr td) {
      padding: 8px;
      font-size: 12px;
    }
    
    :deep(thead tr th) {
      padding: 10px 8px;
      font-size: 11px;
    }
  }
}

/* Animation */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.event-table :deep(tbody tr) {
  animation: fadeIn 0.3s ease;
}
</style>

