<template>
  
  <q-page class="q-pa-sm">
    <!-- Filter Toggle -->
    <div class="row q-mb-md justify-right">
      <q-btn-toggle
        v-model="stateFilter"
        toggle-color="primary"
        :options="[
          { value: 'ALL', slot: 'all' },
          { value: 'ON', slot: 'on' },
          { value: 'OFF', slot: 'off' }
        ]"
        unelevated
        spread
        class="filter-toggle"
      >
        <template v-slot:all>
          <q-icon name="mdi-filter-variant" size="sm" class="q-mr-xs" />
          <span>ALL</span>
        </template>
        <template v-slot:on>
          <q-icon name="mdi-lightbulb-on" size="sm" class="q-mr-xs" color="yellow-9" />
          <span>ON</span>
        </template>
        <template v-slot:off>
          <q-icon name="mdi-lightbulb-off-outline" size="sm" class="q-mr-xs" />
          <span>OFF</span>
        </template>
      </q-btn-toggle>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="row justify-center q-pa-xl">
      <q-spinner color="primary" size="50px" />
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="row justify-center q-pa-xl">
      <q-banner class="bg-negative text-white" rounded>
        <template v-slot:avatar>
          <q-icon name="mdi-alert-circle" />
        </template>
        Failed to load zone data: {{ error }}
      </q-banner>
    </div>

    <!-- Content -->
    <div v-else>
      <div class="row q-col-gutter-lg">
        <!-- Child Zones -->
        <div 
          v-for="zone in childZones" 
          :key="zone.id"
          class="col-lg-4 col-md-4 col-sm-12 col-xs-12"
        >
          <zone-card :zone="zone" />
        </div>

        <!-- Peripherals -->
        <div 
          v-for="peripheral in filteredPeripheralList"
          :key="peripheral.id"
          class="col-lg-4 col-md-4 col-sm-12 col-xs-12"
        >
          <component 
            :is="getPeripheralComponent(peripheral)"
            :peripheral="peripheral"
            @onUpdate="handlePeripheralUpdate"
          />
        </div>
      </div>

      <!-- Temperature Chart (for TEMP category) -->
      <div v-if="showTempChart" class="row q-mt-lg">
        <div class="col-12">
          <temp-chart-card />
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="isEmpty" class="row justify-center q-pa-xl">
        <q-banner class="bg-info text-white" rounded>
          <template v-slot:avatar>
            <q-icon name="mdi-information" />
          </template>
          No {{ categoryLabel }} found in this zone
        </q-banner>
      </div>
    </div>
  </q-page>
</template>

<script>
import {computed, defineComponent, ref, onMounted} from 'vue';
import {useApolloClient, useQuery} from '@vue/apollo-composable';
import {useRoute} from 'vue-router';

import {CACHE_GET_ALL_VALUES, ZONE_GET_BY_ID_WITH_CATEGORY} from '@/graphql/queries';
import {peripheralService} from '@/_services/controls';
import PeripheralHeatCard from '@/components/cards/PeripheralHeatCard';
import PeripheralLightCard from '@/components/cards/PeripheralLightCard';
import PeripheralSwitchCard from '@/components/cards/PeripheralSwitchCard';
import PeripheralTempCard from '@/components/cards/PeripheralTempCard';
import TempChartCard from '@/components/cards/TempChartCard';
import ZoneCard from '@/components/cards/ZoneCard';

import _ from 'lodash';

/**
 * Category configuration mapping
 */
const CATEGORY_CONFIG = {
  LIGHT: {
    component: 'PeripheralLightCard',
    label: 'lights'
  },
  SWITCH: {
    component: 'PeripheralSwitchCard',
    label: 'switches'
  },
  HEAT: {
    component: 'PeripheralHeatCard',
    label: 'heating devices'
  },
  SPRINKLER: {
    component: 'PeripheralLightCard', // Uses light card for sprinklers
    label: 'sprinklers'
  },
  TEMP: {
    component: 'PeripheralTempCard',
    label: 'temperature sensors'
  }
};

export default defineComponent({
  name: 'ZoneCombinedView',
  components: {
    ZoneCard,
    PeripheralLightCard,
    PeripheralSwitchCard,
    PeripheralHeatCard,
    PeripheralTempCard,
    TempChartCard
  },
  setup() {
    const {client} = useApolloClient();
    const route = useRoute();
    
    // Reactive state
    const category = ref(route.query.category);
    const currentZone = ref(null);
    const childZones = ref([]);
    const peripheralList = ref([]);
    const cacheMap = ref({});
    const loading = ref(true);
    const error = ref(null);
    const stateFilter = ref('ALL');

    /**
     * Get human-readable category label
     */
    const categoryLabel = computed(() => {
      return CATEGORY_CONFIG[category.value]?.label || 'items';
    });

    /**
     * Check if temperature chart should be shown
     */
    const showTempChart = computed(() => {
      return category.value === 'TEMP';
    });

    /**
     * Filter peripherals by state (ALL, ON, OFF)
     */
    const filteredPeripheralList = computed(() => {
      if (stateFilter.value === 'ALL') {
        return peripheralList.value;
      }
      
      return peripheralList.value.filter(peripheral => {
        const isOn = peripheral.state === true || peripheral.data?.state === true;
        
        if (stateFilter.value === 'ON') {
          return isOn;
        } else if (stateFilter.value === 'OFF') {
          return !isOn;
        }
        
        return true;
      });
    });

    /**
     * Check if the view is empty (no zones and no peripherals)
     */
    const isEmpty = computed(() => {
      return !loading.value && 
             childZones.value.length === 0 && 
             filteredPeripheralList.value.length === 0;
    });

    /**
     * Get the appropriate component for a peripheral based on category
     */
    const getPeripheralComponent = (peripheral) => {
      const config = CATEGORY_CONFIG[category.value];
      return config?.component || 'PeripheralLightCard';
    };

    /**
     * Load cache data for peripheral state management
     */
    const {onResult: onCacheResult} = useQuery(
      CACHE_GET_ALL_VALUES, 
      {}, 
      {fetchPolicy: 'no-cache'}
    );

    onCacheResult(queryResult => {
      if (queryResult.data?.cacheAll) {
        cacheMap.value = _.reduce(
          queryResult.data.cacheAll,
          (hash, value) => {
            const key = value.cacheKey;
            const cacheName = value.cacheName;
            hash[key] = {[cacheName]: value};
            return hash;
          },
          {}
        );
      }
    });

    /**
     * Initialize a peripheral with cache data
     */
    const peripheralInitCallback = (peripheral) => {
      try {
        const initialized = peripheralService.peripheralInit(cacheMap, peripheral);
        peripheralList.value.push(initialized);
      } catch (err) {
        console.error('Failed to initialize peripheral:', peripheral.id, err);
      }
    };

    /**
     * Load zone data and peripherals
     * Uses optimized query with server-side category filtering for better performance
     */
    const loadZones = async () => {
      if (!route.params.zoneId) {
        error.value = 'Zone ID is missing';
        loading.value = false;
        return;
      }

      try {
        loading.value = true;
        error.value = null;

        const queryResult = await client.query({
          query: ZONE_GET_BY_ID_WITH_CATEGORY,
          variables: {
            id: route.params.zoneId,
            category: category.value // Pass category for server-side filtering
          },
          fetchPolicy: 'network-only',
        });

        if (queryResult.data?.zoneById) {
          const data = _.cloneDeep(queryResult.data);
          currentZone.value = data.zoneById;
          peripheralList.value = [];

          // Load peripherals - already filtered by server
          if (currentZone.value.peripherals) {
            const sortedPeripherals = currentZone.value.peripherals
              .sort((a, b) => a.name.localeCompare(b.name));
            
            sortedPeripherals.forEach(peripheralInitCallback);
          }

          // Load child zones
          childZones.value = currentZone.value.zones || [];
        } else {
          error.value = 'Zone not found';
        }
      } catch (err) {
        console.error('Failed to load zone:', err);
        error.value = err.message || 'Failed to load zone data';
      } finally {
        loading.value = false;
      }
    };

    /**
     * Handle peripheral update event
     */
    const handlePeripheralUpdate = (updatedPeripheral) => {
      try {
        const index = _.findIndex(peripheralList.value, item => {
          return item.id === updatedPeripheral.id;
        });

        if (index !== -1) {
          peripheralList.value[index] = peripheralService.peripheralInit(
            null, 
            updatedPeripheral
          );
        } else {
          console.warn('Peripheral not found for update:', updatedPeripheral.id);
        }
      } catch (err) {
        console.error('Failed to update peripheral:', err);
      }
    };

    // Load data on mount
    onMounted(() => {
      loadZones();
    });

    return {
      // State
      childZones,
      peripheralList,
      category,
      cacheMap,
      loading,
      error,
      currentZone,
      stateFilter,
      
      // Computed
      categoryLabel,
      showTempChart,
      isEmpty,
      filteredPeripheralList,
      
      // Methods
      getPeripheralComponent,
      handlePeripheralUpdate,
    };
  },
});

</script>

<style scoped>
.filter-toggle {
  min-width: 300px;
  max-width: 400px;
}
</style>
