<template>
  <q-card class="peripheral-temp-card text-white">
    <q-item>
      <!-- Temperature Icon Avatar -->
      <q-item-section avatar>
        <q-avatar 
          size="60px" 
          class="shadow-10 bg-cyan-3"
        >
          <q-icon 
            name="mdi-thermometer" 
            color="cyan-10" 
            size="40px" 
          />
        </q-avatar>
      </q-item-section>

      <!-- Temperature Info -->
      <q-item-section>
        <q-item-label class="text-weight-medium text-h5">
          {{ asset.data?.name || 'Unknown Sensor' }}
        </q-item-label>
        <q-item-label class="text-weight-light text-blue-grey-3">
          {{ asset.data?.description || '' }}
        </q-item-label>
      </q-item-section>

      <!-- Actions Menu -->
      <q-item-section side>
        <q-item-label>
          <q-btn-dropdown size="sm" flat round icon="settings" class="text-white">
            <q-list>
              <!-- View Details -->
              <q-item 
                clickable 
                v-close-popup 
                @click="viewDetails"
              >
                <q-item-section avatar>
                  <q-icon name="mdi-information" color="info"/>
                </q-item-section>
                <q-item-section>
                  <q-item-label>Details</q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </q-btn-dropdown>
        </q-item-label>
        <q-item-label>
          <event-logger :peripheral="peripheral" />
        </q-item-label>
      </q-item-section>
    </q-item>

    <q-separator/>
    
    <!-- Temperature Display -->
    <q-card-section class="q-pa-md">
      <div class="row items-center justify-center q-gutter-md">
        <!-- Current Temperature -->
        <div class="col text-center">
          <div class="text-h2 text-weight-bold">
            {{ currentTemperature }}
          </div>
          <div class="text-subtitle2 text-blue-grey-3">
            {{ temperatureUnit }}
          </div>
        </div>
      </div>

      <!-- Last Update -->
      <div class="row justify-center q-mt-md">
        <div class="text-caption text-blue-grey-3">
          <q-icon name="mdi-clock-outline" size="xs" class="q-mr-xs"/>
          Last update: {{ lastUpdateTime }}
        </div>
      </div>

      <!-- Port Info (if available) -->
      <div v-if="portInfo" class="row justify-center q-mt-sm">
        <div class="text-caption text-blue-grey-4">
          <q-icon name="mdi-connection" size="xs" class="q-mr-xs"/>
          {{ portInfo }}
        </div>
      </div>
    </q-card-section>
  </q-card>
</template>

<script>
import {computed, defineComponent, toRefs} from 'vue';
import {useRouter} from 'vue-router';

import {useApolloClient, useGlobalQueryLoading} from '@vue/apollo-composable';
import {useWebSocketListeners} from '@/composables';

import {PERIPHERAL_GET_BY_ID} from '@/graphql/queries';

import _ from 'lodash';
import {format} from 'date-fns';
import EventLogger from 'components/EventLogger.vue';

export default defineComponent({
  name: 'PeripheralTempCard',
  components: {
    EventLogger,
  },
  props: {
    peripheral: {
      type: Object,
      required: true
    }
  },
  emits: ['onUpdate'],
  setup(props, { emit }) {
    const router = useRouter();
    const { client } = useApolloClient();
    const { peripheral: asset } = toRefs(props);

    /**
     * Get the port ID from connected ports
     */
    const portId = computed(() => {
      if (asset.value?.data?.connectedTo?.length > 0) {
        return asset.value.data.connectedTo[0].id;
      }
      return -1;
    });

    /**
     * Get port information string
     */
    const portInfo = computed(() => {
      if (asset.value?.data?.connectedTo?.length > 0) {
        const port = asset.value.data.connectedTo[0];
        return `Port: ${port.name || port.internalRef || port.id}`;
      }
      return null;
    });

    /**
     * Get current temperature value
     */
    const currentTemperature = computed(() => {
      if (asset.value?.value !== null && asset.value?.value !== undefined) {
        const temp = Number(asset.value.value);
        return isNaN(temp) ? '--' : temp.toFixed(1);
      }
      return '--';
    });

    /**
     * Get temperature unit
     */
    const temperatureUnit = computed(() => {
      // You can make this configurable based on peripheral configuration
      return '°C';
    });

    /**
     * Get last update time
     */
    const lastUpdateTime = computed(() => {
      if (asset.value?.data?.tsUpdated) {
        try {
          return format(new Date(asset.value.data.tsUpdated), 'MMM dd, yyyy HH:mm:ss');
        } catch (error) {
          return 'Unknown';
        }
      }
      return 'Unknown';
    });

    /**
     * Computed peripheral for emit
     */
    const compPeripheral = computed({
      get: () => props.peripheral,
      set: value => emit('onUpdate', value),
    });

    /**
     * Load peripheral details
     */
    const loadDetails = async () => {
      try {
        const { data } = await client.query({
          query: PERIPHERAL_GET_BY_ID,
          variables: { id: asset.value.id },
          fetchPolicy: 'network-only',
        });

        let assetRW = _.cloneDeep(data.devicePeripheral);
        compPeripheral.value = assetRW;
      } catch (error) {
        console.error('Failed to load peripheral details:', error);
      }
    };

    // WebSocket event listeners
    useWebSocketListeners([
      {
        eventName: 'evt_port_value_persisted',
        callback: (payload) => {
          asset.value.value = payload.p4;
          if (asset.value.data) {
            asset.value.data.tsUpdated = new Date().toISOString();
          }
          loadDetails();
        },
        filter: (payload) => portId.value === Number(payload.p2)
      }
    ]);

    /**
     * Navigate to peripheral details
     */
    const viewDetails = () => {
      router.push({ path: `/admin/peripherals/${props.peripheral.id}/view` });
    };

    return {
      asset,
      currentTemperature,
      temperatureUnit,
      lastUpdateTime,
      portInfo,
      portId,
      viewDetails,
      loading: useGlobalQueryLoading(),
    };
  },
});

</script>

<style scoped>
.peripheral-temp-card {
  background: linear-gradient(135deg, #0277bd, #4fc3f7);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.peripheral-temp-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
}
</style>

