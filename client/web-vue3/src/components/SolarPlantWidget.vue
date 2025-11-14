<template>
  <q-card class="solar-plant-card">
    <!-- Header -->
    <q-item class="bg-orange-9 text-white">
      <q-item-section avatar>
        <q-avatar>
          <q-icon name="mdi-solar-power" size="lg"/>
        </q-avatar>
      </q-item-section>

      <q-item-section>
        <q-item-label class="text-h6">{{ $t('solar.title') }}</q-item-label>
        <q-item-label caption class="text-orange-2">{{ $t('solar.subtitle') }}</q-item-label>
      </q-item-section>
      
      <q-item-section side>
        <q-badge :color="systemHealth === 3 ? 'green' : systemHealth === 2 ? 'orange' : 'red'" 
                 :label="systemHealth === 3 ? $t('solar.status_ok') : systemHealth === 2 ? $t('solar.status_warning') : $t('solar.status_error')"/>
      </q-item-section>
    </q-item>

    <!-- Real-time Power Flow Section -->
    <q-card-section class="bg-orange-1">
      <div class="text-h6 text-orange-9 q-mb-md">
        <q-icon name="mdi-lightning-bolt" size="sm"/>
        {{ $t('solar.realtime') }}
      </div>
      
      <div class="row q-col-gutter-sm">
        <!-- Solar Production -->
        <div class="col-6 col-sm-3">
          <q-card flat bordered class="bg-white">
            <q-card-section class="text-center q-pa-sm">
              <q-icon name="mdi-solar-panel-large" size="md" class="text-amber-7"/>
              <div class="text-h5 text-weight-bold text-orange-9">{{ solarProductionKw }}</div>
              <div class="text-caption text-grey-7">{{ $t('solar.solar_production') }}</div>
            </q-card-section>
          </q-card>
        </div>

        <!-- House from Plant -->
        <div class="col-6 col-sm-3">
          <q-card flat bordered class="bg-white">
            <q-card-section class="text-center q-pa-sm">
              <q-icon name="mdi-home-lightning-bolt" size="md" class="text-green-7"/>
              <div class="text-h5 text-weight-bold text-green-9">{{ houseFromPlantKw }}</div>
              <div class="text-caption text-grey-7">{{ $t('solar.house_from_plant') }}</div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Grid Import -->
        <div class="col-6 col-sm-3">
          <q-card flat bordered class="bg-white">
            <q-card-section class="text-center q-pa-sm">
              <q-icon name="mdi-transmission-tower-import" size="md" class="text-red-7"/>
              <div class="text-h5 text-weight-bold text-red-9">{{ gridImportKw }}</div>
              <div class="text-caption text-grey-7">{{ $t('solar.grid_import') }}</div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Grid Export -->
        <div class="col-6 col-sm-3">
          <q-card flat bordered class="bg-white">
            <q-card-section class="text-center q-pa-sm">
              <q-icon name="mdi-transmission-tower-export" size="md" class="text-blue-7"/>
              <div class="text-h5 text-weight-bold text-blue-9">{{ gridExportKw }}</div>
              <div class="text-caption text-grey-7">{{ $t('solar.grid_export') }}</div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Phase Consumption -->
      <div class="row q-col-gutter-sm q-mt-sm">
        <div class="col-12">
          <q-card flat bordered class="bg-grey-1">
            <q-card-section class="q-pa-sm">
              <div class="text-subtitle2 text-grey-8 q-mb-xs">{{ $t('solar.phase_consumption') }}</div>
              <div class="row text-center">
                <!-- Phase A -->
                <div class="col-4">
                  <div class="text-weight-medium text-grey-9">{{ phaseAPower }}</div>
                  <div class="text-caption text-grey-6">{{ $t('solar.phase_a') }}</div>
                  <div class="text-caption text-amber-9">{{ phaseACurrent }} A</div>
                </div>
                <q-separator vertical/>
                <!-- Phase B -->
                <div class="col-4">
                  <div class="text-weight-medium text-grey-9">{{ phaseBPower }}</div>
                  <div class="text-caption text-grey-6">{{ $t('solar.phase_b') }}</div>
                  <div class="text-caption text-amber-9">{{ phaseBCurrent }} A</div>
                </div>
                <q-separator vertical/>
                <!-- Phase C -->
                <div class="col-4">
                  <div class="text-weight-medium text-grey-9">{{ phaseCPower }}</div>
                  <div class="text-caption text-grey-6">{{ $t('solar.phase_c') }}</div>
                  <div class="text-caption text-amber-9">{{ phaseCCurrent }} A</div>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </q-card-section>

    <!-- Daily Totals Section -->
    <q-card-section class="bg-blue-grey-1">
      <div class="text-h6 text-blue-grey-9 q-mb-md">
        <q-icon name="mdi-calendar-today" size="sm"/>
        {{ $t('solar.today') }}
      </div>
      
      <div class="row q-col-gutter-sm">
        <div class="col-6 col-sm-3">
          <q-card flat bordered class="bg-white">
            <q-card-section class="text-center q-pa-sm">
              <div class="text-h6 text-weight-bold text-orange-8">{{ dailyYield }}</div>
              <div class="text-caption text-grey-7">{{ $t('solar.daily_yield') }}</div>
            </q-card-section>
          </q-card>
        </div>

        <div class="col-6 col-sm-3">
          <q-card flat bordered class="bg-white">
            <q-card-section class="text-center q-pa-sm">
              <div class="text-h6 text-weight-bold text-grey-8">{{ dailyTotalConsumption }}</div>
              <div class="text-caption text-grey-7">{{ $t('solar.daily_total_consumption') }}</div>
            </q-card-section>
          </q-card>
        </div>

        <div class="col-6 col-sm-3">
          <q-card flat bordered class="bg-white">
            <q-card-section class="text-center q-pa-sm">
              <div class="text-h6 text-weight-bold text-green-8">{{ dailySolarUsage }}</div>
              <div class="text-caption text-grey-7">{{ $t('solar.daily_solar_usage') }}</div>
            </q-card-section>
          </q-card>
        </div>

        <div class="col-6 col-sm-3">
          <q-card flat bordered class="bg-white">
            <q-card-section class="text-center q-pa-sm">
              <div class="text-h6 text-weight-bold text-blue-8">{{ dailyGridExport }}</div>
              <div class="text-caption text-grey-7">{{ $t('solar.daily_grid_export') }}</div>
            </q-card-section>
          </q-card>
        </div>

        <div class="col-6 col-sm-3">
          <q-card flat bordered class="bg-white">
            <q-card-section class="text-center q-pa-sm">
              <div class="text-h6 text-weight-bold text-red-8">{{ dailyGridImport }}</div>
              <div class="text-caption text-grey-7">{{ $t('solar.daily_grid_import') }}</div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </q-card-section>

    <!-- Long-term Totals Section -->
    <q-card-section class="bg-green-1">
      <div class="text-h6 text-green-9 q-mb-md">
        <q-icon name="mdi-chart-line" size="sm"/>
        {{ $t('solar.totals') }}
      </div>
      
      <div class="row q-col-gutter-sm">
        <div class="col-6 col-sm-4">
          <q-card flat bordered class="bg-white">
            <q-card-section class="text-center q-pa-sm">
              <div class="text-subtitle2 text-weight-bold text-orange-7">{{ monthlyYield }}</div>
              <div class="text-caption text-grey-7">{{ $t('solar.monthly_yield') }}</div>
            </q-card-section>
          </q-card>
        </div>

        <div class="col-6 col-sm-4">
          <q-card flat bordered class="bg-white">
            <q-card-section class="text-center q-pa-sm">
              <div class="text-subtitle2 text-weight-bold text-orange-7">{{ yearlyYield }}</div>
              <div class="text-caption text-grey-7">{{ $t('solar.yearly_yield') }}</div>
            </q-card-section>
          </q-card>
        </div>

        <div class="col-6 col-sm-4">
          <q-card flat bordered class="bg-white">
            <q-card-section class="text-center q-pa-sm">
              <div class="text-subtitle2 text-weight-bold text-green-7">{{ totalYield }}</div>
              <div class="text-caption text-grey-7">{{ $t('solar.total_yield') }}</div>
            </q-card-section>
          </q-card>
        </div>

        <div class="col-6 col-sm-6">
          <q-card flat bordered class="bg-white">
            <q-card-section class="text-center q-pa-sm">
              <div class="text-subtitle2 text-weight-bold text-blue-7">{{ totalGridExport }}</div>
              <div class="text-caption text-grey-7">{{ $t('solar.total_grid_export') }}</div>
            </q-card-section>
          </q-card>
        </div>

        <div class="col-6 col-sm-6">
          <q-card flat bordered class="bg-white">
            <q-card-section class="text-center q-pa-sm">
              <div class="text-subtitle2 text-weight-bold text-red-7">{{ totalGridImport }}</div>
              <div class="text-caption text-grey-7">{{ $t('solar.total_grid_import') }}</div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </q-card-section>

    <!-- Additional Info (optional) -->
    <q-card-section class="bg-grey-2 q-pa-sm">
      <div class="row text-center text-caption text-grey-7">
        <div class="col-4" v-if="inverterTemp">
          <q-icon name="mdi-thermometer" size="xs"/>
          {{ inverterTemp }}°C
        </div>
        <div class="col-4" v-if="gridFrequency">
          <q-icon name="mdi-sine-wave" size="xs"/>
          {{ gridFrequency }} Hz
        </div>
        <div class="col-4" v-if="efficiency">
          <q-icon name="mdi-speedometer" size="xs"/>
          {{ efficiency }}%
        </div>
      </div>
    </q-card-section>
  </q-card>
</template>

<script>
import { computed, defineComponent, onMounted, ref, toRefs } from 'vue';
import { useApolloClient } from "@vue/apollo-composable";
import { useWebSocketListener } from "@/composables";
import { DEVICE_GET_BY_ID_WITH_PORT_VALUES } from '@/graphql/queries';
import _ from "lodash";

export default defineComponent({
  name: 'SolarPlantWidget',
  props: {
    deviceId: {
      type: Number,
      required: true,
      default: 1000 // Huawei inverter device ID
    },
    meterDeviceId: {
      type: Number,
      required: false,
      default: 1001 // Huawei meter device ID
    },
  },
  setup(props) {
    const { client } = useApolloClient();
    const { deviceId, meterDeviceId } = toRefs(props);
    
    const device = ref({});
    const deviceDetails = ref({});
    const portIds = ref([]);

    /**
     * Get port value by internal reference
     */
    const getPortValue = (internalRef) => {
      const port = deviceDetails.value[internalRef];
      return port?.value || null;
    };

    /**
     * Parse float value from port
     */
    const getFloatValue = (internalRef, decimals = 2) => {
      const value = getPortValue(internalRef);
      if (value === null || value === undefined) return null;
      const parsed = parseFloat(value);
      return isNaN(parsed) ? null : parsed.toFixed(decimals);
    };

    /**
     * Format energy value - converts to MWh if >= 1000 kWh
     */
    const formatEnergy = (kwh) => {
      if (!kwh || kwh === '--') return '--';
      const value = parseFloat(kwh);
      if (isNaN(value)) return '--';
      
      if (value >= 1000) {
        const mwh = value / 1000;
        return `${mwh.toFixed(2)} MWh`;
      }
      return `${value.toFixed(2)} kWh`;
    };

    // ============================================================================
    // REAL-TIME COMPUTED PROPERTIES
    // ============================================================================

    /**
     * Solar production in kW (from inverter)
     */
    const solarProductionKw = computed(() => {
      const watts = getFloatValue('inverter.active_power', 0);
      if (watts === null) return '--';
      return (parseFloat(watts) / 1000).toFixed(2) + ' kW';
    });

    /**
     * Grid power in W (from meter)
     * Negative = importing from grid
     * Positive = exporting to grid
     */
    const meterActivePowerWatts = computed(() => {
      const watts = getFloatValue('meter.active_power', 0);
      return watts ? parseFloat(watts) : 0;
    });

    /**
     * Grid import in kW (when meter is negative)
     */
    const gridImportKw = computed(() => {
      const watts = meterActivePowerWatts.value;
      if (watts >= 0) return '0.00 kW';
      return (Math.abs(watts) / 1000).toFixed(2) + ' kW';
    });

    /**
     * Grid export in kW (when meter is positive)
     */
    const gridExportKw = computed(() => {
      const watts = meterActivePowerWatts.value;
      if (watts <= 0) return '0.00 kW';
      return (watts / 1000).toFixed(2) + ' kW';
    });

    /**
     * House consumption from solar plant
     * = Solar production - Grid export (or + Grid import)
     * 
     * Note: API docs say inverter.active_power is in kW, but widget treats it as W
     * TODO: Verify actual API response unit during daytime production
     */
    const houseFromPlantKw = computed(() => {
      const solarValue = parseFloat(getFloatValue('inverter.active_power', 0) || 0);
      const meterWatts = meterActivePowerWatts.value;
      
      // Assuming both are in same units (likely W based on current widget behavior)
      const houseUsage = solarValue - meterWatts;
      
      if (houseUsage < 0) return '0.00 kW';
      return (houseUsage / 1000).toFixed(2) + ' kW';
    });

    /**
     * Phase power and current
     */
    const phaseAPower = computed(() => {
      const watts = getFloatValue('meter.active_power_a', 0);
      return watts ? `${Math.abs(parseFloat(watts))} W` : '--';
    });

    const phaseBPower = computed(() => {
      const watts = getFloatValue('meter.active_power_b', 0);
      return watts ? `${Math.abs(parseFloat(watts))} W` : '--';
    });

    const phaseCPower = computed(() => {
      const watts = getFloatValue('meter.active_power_c', 0);
      return watts ? `${Math.abs(parseFloat(watts))} W` : '--';
    });

    /**
     * Phase currents
     * Uses direct meter current readings for accuracy
     * Fallback to calculated values if meter data unavailable
     */
    const phaseACurrent = computed(() => {
      // Use direct meter reading (most accurate)
      const directCurrent = getFloatValue('meter.meter_i', 2);
      if (directCurrent !== null) {
        return Math.abs(parseFloat(directCurrent)).toFixed(2);
      }
      
      // Fallback: calculate from power and voltage (accounting for power factor)
      const powerA = parseFloat(getFloatValue('meter.active_power_a', 0) || 0);
      const voltage = parseFloat(getFloatValue('meter.meter_u', 0) || 230);
      const powerFactor = parseFloat(getFloatValue('meter.power_factor', 2) || 1.0);
      
      if (voltage > 0 && powerFactor > 0) {
        return Math.abs(powerA / (voltage * powerFactor)).toFixed(2);
      }
      return '--';
    });

    const phaseBCurrent = computed(() => {
      // Use direct meter reading (most accurate)
      const directCurrent = getFloatValue('meter.b_i', 2);
      if (directCurrent !== null) {
        return Math.abs(parseFloat(directCurrent)).toFixed(2);
      }
      
      // Fallback: calculate from power and voltage (accounting for power factor)
      const powerB = parseFloat(getFloatValue('meter.active_power_b', 0) || 0);
      const voltage = parseFloat(getFloatValue('meter.b_u', 0) || 230);
      const powerFactor = parseFloat(getFloatValue('meter.power_factor', 2) || 1.0);
      
      if (voltage > 0 && powerFactor > 0) {
        return Math.abs(powerB / (voltage * powerFactor)).toFixed(2);
      }
      return '--';
    });

    const phaseCCurrent = computed(() => {
      // Use direct meter reading (most accurate)
      const directCurrent = getFloatValue('meter.c_i', 2);
      if (directCurrent !== null) {
        return Math.abs(parseFloat(directCurrent)).toFixed(2);
      }
      
      // Fallback: calculate from power and voltage (accounting for power factor)
      const powerC = parseFloat(getFloatValue('meter.active_power_c', 0) || 0);
      const voltage = parseFloat(getFloatValue('meter.c_u', 0) || 230);
      const powerFactor = parseFloat(getFloatValue('meter.power_factor', 2) || 1.0);
      
      if (voltage > 0 && powerFactor > 0) {
        return Math.abs(powerC / (voltage * powerFactor)).toFixed(2);
      }
      return '--';
    });

    // ============================================================================
    // DAILY TOTALS
    // ============================================================================

    const dailyYield = computed(() => {
      const kwh = getFloatValue('station.day_power', 2);
      return kwh ? `${kwh} kWh` : '--';
    });

    /**
     * Total daily consumption (from PV + Grid)
     * This is the actual house consumption from all sources
     */
    const dailyTotalConsumption = computed(() => {
      const kwh = getFloatValue('station.day_use_energy', 2);
      return kwh ? `${kwh} kWh` : '--';
    });

    /**
     * Daily solar usage (energy consumed directly from solar)
     * = Daily production - Daily grid export
     */
    const dailySolarUsage = computed(() => {
      const production = parseFloat(getFloatValue('station.day_power', 2) || 0);
      const export_ = parseFloat(getFloatValue('station.day_on_grid_energy', 2) || 0);
      
      const solarUsage = production - export_;
      return solarUsage >= 0 ? `${solarUsage.toFixed(2)} kWh` : '0.00 kWh';
    });

    const dailyGridExport = computed(() => {
      const kwh = getFloatValue('station.day_on_grid_energy', 2);
      return kwh ? `${kwh} kWh` : '--';
    });

    /**
     * Daily grid import calculation:
     * = Total consumption - Solar usage
     */
    const dailyGridImport = computed(() => {
      const totalConsumption = parseFloat(getFloatValue('station.day_use_energy', 2) || 0);
      const production = parseFloat(getFloatValue('station.day_power', 2) || 0);
      const export_ = parseFloat(getFloatValue('station.day_on_grid_energy', 2) || 0);
      
      const solarUsage = production - export_;
      const import_ = totalConsumption - solarUsage;
      
      return import_ > 0 ? `${import_.toFixed(2)} kWh` : '0.00 kWh';
    });

    // ============================================================================
    // LONG-TERM TOTALS
    // ============================================================================

    const monthlyYield = computed(() => {
      const kwh = getFloatValue('station.month_power', 2);
      return kwh ? formatEnergy(kwh) : '--';
    });

    const yearlyYield = computed(() => {
      // TODO: Implement yearly tracking
      return 'N/A';
    });

    const totalYield = computed(() => {
      const kwh = getFloatValue('station.total_power', 2);
      return kwh ? formatEnergy(kwh) : '--';
    });

    const totalGridExport = computed(() => {
      const kwh = getFloatValue('meter.reverse_active_cap', 2);
      return kwh ? formatEnergy(kwh) : '--';
    });

    const totalGridImport = computed(() => {
      const kwh = getFloatValue('meter.active_cap', 2);
      return kwh ? formatEnergy(kwh) : '--';
    });

    // ============================================================================
    // ADDITIONAL INFO
    // ============================================================================

    const systemHealth = computed(() => {
      const health = getPortValue('station.real_health_state');
      return health ? parseInt(health) : 3; // Default to OK
    });

    const inverterTemp = computed(() => {
      return getFloatValue('inverter.temperature', 1);
    });

    const gridFrequency = computed(() => {
      return getFloatValue('meter.grid_frequency', 2);
    });

    const efficiency = computed(() => {
      return getFloatValue('inverter.efficiency', 1);
    });

    // ============================================================================
    // DATA LOADING
    // ============================================================================

    const loadDetails = () => {
      // Load both inverter and meter devices
      Promise.all([
        client.query({
          query: DEVICE_GET_BY_ID_WITH_PORT_VALUES,
          variables: { id: deviceId.value },
          fetchPolicy: 'network-only',
        }),
        client.query({
          query: DEVICE_GET_BY_ID_WITH_PORT_VALUES,
          variables: { id: meterDeviceId.value },
          fetchPolicy: 'network-only',
        })
      ]).then(([inverterData, meterData]) => {
        device.value = inverterData.data.device;
        
        // Merge ports from both devices
        const allPorts = [
          ...inverterData.data.device.ports,
          ...meterData.data.device.ports
        ];
        
        // Map ports by internal reference
        deviceDetails.value = _.reduce(
          allPorts,
          (hash, port) => {
            hash[port.internalRef] = ref(structuredClone(port));
            return hash;
          },
          {}
        );
        
        // Collect port IDs for WebSocket filtering (both devices)
        portIds.value = allPorts.map(port => port.id);
      });
    };

    onMounted(() => {
      loadDetails();
    });

    // Listen for real-time port value updates via WebSocket
    useWebSocketListener('evt_port_value_persisted', (payload) => {
      if (portIds.value.includes(Number(payload.p2))) {
        const portInternalRef = payload.p3;
        if (deviceDetails.value[portInternalRef]) {
          deviceDetails.value[portInternalRef].value = payload.p4;
        }
      }
    });

    return {
      device,
      deviceDetails,
      // Real-time
      solarProductionKw,
      houseFromPlantKw,
      gridImportKw,
      gridExportKw,
      phaseAPower,
      phaseBPower,
      phaseCPower,
      phaseACurrent,
      phaseBCurrent,
      phaseCCurrent,
      // Daily
      dailyYield,
      dailyTotalConsumption,
      dailySolarUsage,
      dailyGridExport,
      dailyGridImport,
      // Long-term
      monthlyYield,
      yearlyYield,
      totalYield,
      totalGridExport,
      totalGridImport,
      // Additional
      systemHealth,
      inverterTemp,
      gridFrequency,
      efficiency,
    };
  }
});
</script>

<style scoped>
.solar-plant-card {
  max-width: 100%;
}

@media (max-width: 599px) {
  .solar-plant-card .text-h5 {
    font-size: 1.2rem;
  }
  
  .solar-plant-card .text-h6 {
    font-size: 1rem;
  }
}
</style>

