<template>
  <q-btn 
    size="md" 
    round 
    icon="mdi-thermometer-lines" 
    class="bg-blue-grey-8 text-blue-grey-1" 
    @click="openScheduler"
  >
    <q-tooltip>Heat Scheduler</q-tooltip>
  </q-btn>
  
  <q-dialog
    v-model="visible"
    transition-show="jump-up"
    transition-hide="jump-down"
    class="heat-scheduler"
    :maximized="$q.platform.is.mobile"
  >
    <q-card class="bg-white" :style="cardStyle">
      <!-- Header -->
      <q-bar class="bg-green-5 text-white">
        <q-icon name="mdi-thermometer"/>
        <div>Thermostat Scheduler</div>
        <q-space/>
        <q-btn dense flat icon="close" v-close-popup>
          <q-tooltip>Close</q-tooltip>
        </q-btn>
      </q-bar>

      <!-- Loading State -->
      <q-inner-loading :showing="loading">
        <q-spinner-gears size="50px" color="primary"/>
      </q-inner-loading>

      <!-- Temperature and Time Input -->
      <q-card-section v-if="!loading" class="q-gutter-md">
        <div class="text-h6 text-grey-8 q-mb-sm">
          <q-icon name="mdi-plus-circle" class="q-mr-xs"/>
          Add Schedule
        </div>

        <!-- Temperature Slider -->
        <div class="q-px-sm">
          <div class="text-caption text-grey-7 q-mb-xs">Target Temperature</div>
          <slider
            v-model="temperature"
            orientation="horizontal"
            :height="16"
            :width="'100%'"
            :min="tempRange.min"
            :max="tempRange.max"
            :step="1"
            tooltip
            color="#4080f7"
            tooltipText="%v ℃"
            tooltipColor="#4080f7"
            tooltipTextColor="#FEFEFE"
            sticky
          />
          <div class="row justify-center q-mt-sm">
            <q-badge rounded color="orange-8" :label="temperature + ' ℃'" class="text-h6 q-pa-sm"/>
          </div>
        </div>

        <!-- Time Input -->
        <q-input 
          v-model="time" 
          filled 
          dense
          label="Time"
          mask="time" 
          :rules="['time']"
          hint="Select time for this temperature"
          color="orange"
        >
          <template v-slot:prepend>
            <q-icon name="mdi-clock-outline"/>
          </template>
          <template v-slot:append>
            <q-icon name="mdi-clock" class="cursor-pointer">
              <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                <q-time v-model="time" color="orange" format24h>
                  <div class="row items-center justify-end">
                    <q-btn v-close-popup label="Set" color="primary" flat/>
                  </div>
                </q-time>
              </q-popup-proxy>
            </q-icon>
          </template>
        </q-input>

        <!-- Add Button -->
        <q-btn
          label="Add Schedule"
          icon="mdi-plus"
          color="positive"
          class="full-width"
          :loading="saving"
          :disable="!isValidSchedule"
          @click="addSchedule"
        />
      </q-card-section>

      <q-separator v-if="!loading"/>

      <!-- Schedule List -->
      <q-card-section v-if="!loading">
        <div class="text-h6 text-grey-8 q-mb-sm">
          <q-icon name="mdi-calendar-clock" class="q-mr-xs"/>
          Schedule ({{ scheduleItems.length }})
        </div>

        <q-table 
          :rows="sortedScheduleItems" 
          :columns="columns" 
          row-key="id" 
          dense
          flat
          :rows-per-page-options="[0]"
          hide-pagination
          class="schedule-table"
        >
          <template v-slot:body-cell-time="props">
            <q-td :props="props">
              <q-badge color="primary" :label="props.row.time"/>
            </q-td>
          </template>

          <template v-slot:body-cell-temp="props">
            <q-td :props="props">
              <q-chip 
                color="orange-8" 
                text-color="white" 
                icon="mdi-thermometer"
              >
                {{ props.row.temp }} ℃
              </q-chip>
            </q-td>
          </template>

          <template v-slot:body-cell-actions="props">
            <q-td :props="props">
              <q-btn 
                dense 
                round 
                flat 
                color="negative" 
                icon="mdi-delete" 
                @click="confirmDelete(props.row)"
              >
                <q-tooltip>Delete</q-tooltip>
              </q-btn>
            </q-td>
          </template>

          <template v-slot:no-data>
            <div class="full-width row flex-center text-grey-6 q-py-lg">
              <q-icon name="mdi-calendar-blank" size="3em" class="q-mr-md"/>
              <span class="text-subtitle1">No schedules configured</span>
            </div>
          </template>
        </q-table>
      </q-card-section>

      <!-- Actions -->
      <q-card-actions align="right" class="q-px-md q-pb-md">
        <q-btn label="Close" color="grey-7" flat v-close-popup/>
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { useQuasar } from 'quasar';
import slider from 'vue3-slider';
import {
  CONFIGURATION_ADDLIST_CONFIG_VALUE,
  CONFIGURATION_GET_LIST_VALUE,
  CONFIGURATION_REMOVE_CONFIG
} from '@/graphql/queries';

// Props
const props = defineProps({
  zone: {
    type: Object,
    required: true
  }
});

// Composables
const { client } = useApolloClient();
const $q = useQuasar();

// Constants
const CONFIG_KEY = 'key.temp.schedule.list.value';
const TEMP_RANGE = {
  min: 0,
  max: 40
};

// State
const visible = ref(false);
const loading = ref(false);
const saving = ref(false);
const scheduleItems = ref([]);
const temperature = ref(20);
const time = ref('00:00');

// Computed
const tempRange = computed(() => TEMP_RANGE);

const cardStyle = computed(() => {
  return $q.platform.is.mobile ? '' : 'width: 600px; max-width: 90vw;';
});

const isValidSchedule = computed(() => {
  return time.value && time.value !== '' && temperature.value >= TEMP_RANGE.min && temperature.value <= TEMP_RANGE.max;
});

const sortedScheduleItems = computed(() => {
  return [...scheduleItems.value].sort((a, b) => {
    const timeA = a.time.split(':').map(Number);
    const timeB = b.time.split(':').map(Number);
    return (timeA[0] * 60 + timeA[1]) - (timeB[0] * 60 + timeB[1]);
  });
});

const columns = [
  {
    name: 'time',
    required: true,
    label: 'Time',
    align: 'left',
    field: 'time',
    sortable: true
  },
  {
    name: 'temp',
    required: true,
    label: 'Temperature',
    align: 'left',
    field: 'temp',
    sortable: true
  },
  {
    name: 'actions',
    align: 'right',
    label: 'Actions'
  }
];

/**
 * Load schedule items from the server
 */
const loadSchedule = async () => {
  loading.value = true;
  
  try {
    const response = await client.query({
      query: CONFIGURATION_GET_LIST_VALUE,
      variables: {
        entityId: props.zone.id,
        entityType: 'ZONE',
        key: CONFIG_KEY
      },
      fetchPolicy: 'network-only'
    });

    scheduleItems.value = response.data.configListByKey.map(config => {
      const configValue = JSON.parse(config.value);
      return {
        id: config.id,
        time: configValue.time,
        temp: configValue.temp
      };
    });
  } catch (error) {
    console.error('Error loading schedule:', error);
    $q.notify({
      color: 'negative',
      message: 'Failed to load schedule',
      icon: 'mdi-alert-circle',
      position: 'top'
    });
  } finally {
    loading.value = false;
  }
};

/**
 * Add a new schedule item
 */
const addSchedule = async () => {
  if (!isValidSchedule.value) {
    return;
  }

  // Check for duplicate time
  const isDuplicate = scheduleItems.value.some(item => item.time === time.value);
  if (isDuplicate) {
    $q.notify({
      color: 'warning',
      message: `A schedule already exists for ${time.value}`,
      icon: 'mdi-alert',
      position: 'top'
    });
    return;
  }

  saving.value = true;

  try {
    const jsonValue = JSON.stringify({
      time: time.value,
      temp: temperature.value
    });

    await client.mutate({
      mutation: CONFIGURATION_ADDLIST_CONFIG_VALUE,
      variables: {
        key: CONFIG_KEY,
        value: jsonValue,
        entityId: props.zone.id,
        entityType: 'ZONE',
        description: `Heat schedule for ${time.value}`
      }
    });

    $q.notify({
      color: 'positive',
      message: `Schedule added for ${time.value}`,
      icon: 'mdi-check-circle',
      position: 'top'
    });

    // Reload schedule
    await loadSchedule();

    // Reset form
    time.value = '00:00';
    temperature.value = 20;
  } catch (error) {
    console.error('Error adding schedule:', error);
    $q.notify({
      color: 'negative',
      message: 'Failed to add schedule',
      icon: 'mdi-alert-circle',
      position: 'top'
    });
  } finally {
    saving.value = false;
  }
};

/**
 * Confirm deletion of a schedule item
 */
const confirmDelete = (row) => {
  $q.dialog({
    title: 'Confirm Delete',
    message: `Are you sure you want to delete the schedule for ${row.time} (${row.temp}℃)?`,
    cancel: true,
    persistent: true,
    ok: {
      label: 'Delete',
      color: 'negative',
      flat: true
    },
    cancel: {
      label: 'Cancel',
      color: 'grey-7',
      flat: true
    }
  }).onOk(() => {
    deleteSchedule(row);
  });
};

/**
 * Delete a schedule item
 */
const deleteSchedule = async (row) => {
  loading.value = true;

  try {
    await client.mutate({
      mutation: CONFIGURATION_REMOVE_CONFIG,
      variables: {
        id: row.id
      }
    });

    $q.notify({
      color: 'positive',
      message: `Schedule for ${row.time} deleted`,
      icon: 'mdi-check-circle',
      position: 'top'
    });

    // Reload schedule
    await loadSchedule();
  } catch (error) {
    console.error('Error deleting schedule:', error);
    $q.notify({
      color: 'negative',
      message: 'Failed to delete schedule',
      icon: 'mdi-alert-circle',
      position: 'top'
    });
  } finally {
    loading.value = false;
  }
};

/**
 * Open the scheduler dialog
 */
const openScheduler = (e) => {
  e?.stopPropagation();
  visible.value = true;
  loadSchedule();
};

// Load schedule on mount
onMounted(() => {
  loadSchedule();
});
</script>


<style scoped>
.heat-scheduler :deep(.q-badge) {
  font-size: 18px;
  font-weight: 500;
  padding: 8px 12px;
}

.schedule-table {
  border: 1px solid #e0e0e0;
  border-radius: 4px;
}

.schedule-table :deep(.q-table__top) {
  padding: 12px;
}

.schedule-table :deep(thead tr th) {
  background-color: #f5f5f5;
  font-weight: 600;
}

.schedule-table :deep(tbody tr:hover) {
  background-color: #f9f9f9;
}
</style>

