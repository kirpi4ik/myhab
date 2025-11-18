<template>
  <q-card flat class="solar-plant-card">
    <!-- Header -->
    <q-card-section class="header-section q-pa-md">
      <div class="row items-center justify-between">
        <div class="col-auto">
          <div class="text-h5 text-weight-medium text-grey-9">{{ $t('solar.title') }}</div>
          <div class="text-caption text-grey-6">{{ $t('solar.subtitle') }}</div>
        </div>
        <div class="col-auto row q-gutter-xs items-center">
          <!-- Device Status Badge -->
          <q-chip 
            :color="deviceStatusColor" 
            text-color="white"
            size="sm"
            :icon="deviceStatusIcon">
            {{ deviceStatusLabel }}
          </q-chip>
          <!-- System Health Badge -->
          <q-chip 
            :color="systemHealth === 3 ? 'positive' : systemHealth === 2 ? 'warning' : 'negative'" 
            text-color="white"
            size="sm"
            :icon="systemHealth === 3 ? 'mdi-check-circle' : systemHealth === 2 ? 'mdi-alert' : 'mdi-alert-circle'">
            {{ systemHealth === 3 ? $t('solar.status_ok') : systemHealth === 2 ? $t('solar.status_warning') : $t('solar.status_error') }}
          </q-chip>
          <!-- Reports Button -->
          <q-btn
            round
            flat
            color="primary"
            icon="mdi-chart-box-outline"
            size="sm"
            @click="openReports"
          >
            <q-tooltip>View Reports & History</q-tooltip>
          </q-btn>
        </div>
      </div>
    </q-card-section>

    <q-separator/>

    <!-- Real-time Power Flow Section -->
    <q-card-section class="realtime-section q-pa-md">
      <div class="section-title q-mb-lg">
        <q-icon name="mdi-lightning-bolt-circle" size="sm" class="text-amber-7 q-mr-xs"/>
        <span class="text-subtitle1 text-weight-medium text-grey-8">{{ $t('solar.realtime') }}</span>
      </div>
      
      <!-- Power Flow Diagram -->
      <div class="power-flow-container q-mb-lg">
        <div class="flow-diagram">
          <!-- PV Circle (Top) -->
          <div class="flow-node pv-node">
            <div class="node-circle pv-circle">
              <q-icon name="mdi-white-balance-sunny" size="42px" class="text-amber-6"/>
              <div class="node-value text-weight-bold">{{ solarProductionKw }}</div>
            </div>
            <div class="node-label text-caption text-grey-7">PV</div>
          </div>

          <!-- Consumption Circle (Bottom Left) -->
          <div class="flow-node consumption-node">
            <div class="node-circle consumption-circle">
              <q-icon name="mdi-home-lightning-bolt-outline" size="38px" class="text-blue-6"/>
              <div class="node-value text-weight-bold">{{ totalHouseConsumptionKw }}</div>
            </div>
            <div class="node-label text-caption text-grey-7">{{ $t('solar.house_consumption') || 'Consumption' }}</div>
          </div>

          <!-- Grid Circle (Bottom Right) -->
          <div class="flow-node grid-node">
            <div class="node-circle grid-circle">
              <q-icon name="mdi-transmission-tower" size="38px" class="text-grey-6"/>
              <div class="node-value text-weight-bold">{{ gridExportKw !== '0.00 kW' ? gridExportKw : gridImportKw }}</div>
            </div>
            <div class="node-label text-caption text-grey-7">{{ $t('solar.grid') || 'Grid' }}</div>
          </div>

          <!-- Connection Lines -->
          <svg class="flow-lines" viewBox="0 0 400 300" preserveAspectRatio="xMidYMid meet">
            <!-- PV to Consumption -->
            <line x1="150" y1="100" x2="115" y2="170" class="flow-line pv-to-consumption" stroke-width="3"/>
            <polygon points="115,170 126,162 115,157" class="flow-arrow" :class="{'active': solarProductionWatts > 0 && totalHouseConsumptionWatts > 0}"/>
            
            <!-- PV to Grid -->
            <line x1="250" y1="100" x2="285" y2="170" class="flow-line pv-to-grid" stroke-width="3"/>
            <polygon points="285,170 274,162 285,157" class="flow-arrow" :class="{'active': solarProductionWatts > 0 && meterActivePowerWatts > 0}"/>
            
            <!-- Grid to Consumption (bidirectional base) -->
            <line x1="120" y1="240" x2="280" y2="240" class="flow-line grid-consumption" stroke-width="3"/>
            <polygon points="125,235 115,240 125,245" class="flow-arrow" :class="{'active': meterActivePowerWatts < 0}"/>
          </svg>
        </div>
      </div>

      <!-- Metrics Cards -->
      <div class="row q-col-gutter-md q-mb-md">

        <div class="col-6 col-sm-3">
          <div class="metric-card consumption-metric">
            <q-icon name="mdi-home-lightning-bolt" size="md" class="metric-icon"/>
            <div class="metric-value">{{ totalHouseConsumptionKw }}</div>
            <div class="metric-label">{{ $t('solar.house_consumption') || 'House Consumption' }}</div>
          </div>
        </div>
        
        <div class="col-6 col-sm-3">
          <div class="metric-card import-metric">
            <q-icon name="mdi-transmission-tower-import" size="md" class="metric-icon"/>
            <div class="metric-value">{{ gridImportKw }}</div>
            <div class="metric-label">{{ $t('solar.grid_import') }}</div>
          </div>
        </div>

        <div class="col-6 col-sm-3">
          <div class="metric-card solar-metric">
            <q-icon name="mdi-solar-panel-large" size="md" class="metric-icon"/>
            <div class="metric-value">{{ solarProductionKw }}</div>
            <div class="metric-label">{{ $t('solar.solar_production') }}</div>
          </div>
        </div>

        <div class="col-6 col-sm-3">
          <div class="metric-card export-metric">
            <q-icon name="mdi-transmission-tower-export" size="md" class="metric-icon"/>
            <div class="metric-value">{{ gridExportKw }}</div>
            <div class="metric-label">{{ $t('solar.grid_export') }}</div>
          </div>
        </div>
      </div>

      <!-- Phase Consumption -->
      <div class="phases-container">
        <div class="text-caption text-grey-7 text-uppercase q-mb-sm">{{ $t('solar.phase_consumption') }}</div>
        <div class="row q-col-gutter-sm">
          <div class="col-4">
            <div class="phase-card">
              <div class="phase-header">{{ $t('solar.phase_a') }}</div>
              <div class="phase-power">
                <q-icon 
                  :name="phaseADirection === 'export' ? 'mdi-arrow-up-bold' : 'mdi-arrow-down-bold'" 
                  :class="phaseADirection === 'export' ? 'text-blue-6' : 'text-red-6'"
                  size="xs"
                />
                {{ phaseAPower }}
              </div>
              <div class="phase-current">{{ phaseACurrent }} A</div>
            </div>
          </div>
          <div class="col-4">
            <div class="phase-card">
              <div class="phase-header">{{ $t('solar.phase_b') }}</div>
              <div class="phase-power">
                <q-icon 
                  :name="phaseBDirection === 'export' ? 'mdi-arrow-up-bold' : 'mdi-arrow-down-bold'" 
                  :class="phaseBDirection === 'export' ? 'text-blue-6' : 'text-red-6'"
                  size="xs"
                />
                {{ phaseBPower }}
              </div>
              <div class="phase-current">{{ phaseBCurrent }} A</div>
            </div>
          </div>
          <div class="col-4">
            <div class="phase-card">
              <div class="phase-header">{{ $t('solar.phase_c') }}</div>
              <div class="phase-power">
                <q-icon 
                  :name="phaseCDirection === 'export' ? 'mdi-arrow-up-bold' : 'mdi-arrow-down-bold'" 
                  :class="phaseCDirection === 'export' ? 'text-blue-6' : 'text-red-6'"
                  size="xs"
                />
                {{ phaseCPower }}
              </div>
              <div class="phase-current">{{ phaseCCurrent }} A</div>
            </div>
          </div>
        </div>
      </div>
    </q-card-section>

    <q-separator/>

    <!-- Daily Totals Section -->
    <q-expansion-item
      v-model="dailySectionExpanded"
      icon="mdi-calendar-today"
      :label="$t('solar.today')"
      header-class="bg-grey-1"
      expand-separator
    >
      <q-card-section class="daily-section q-pa-md">
        <div class="row q-col-gutter-md">
          <div class="col-6 col-md-4">
            <div class="stat-card yield-card">
              <div class="stat-icon">
                <q-icon name="mdi-solar-power" size="lg"/>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ dailyYield }}</div>
                <div class="stat-label">{{ $t('solar.daily_yield') }}</div>
              </div>
            </div>
          </div>

          <div class="col-6 col-md-4">
            <div class="stat-card consumption-card">
              <div class="stat-icon">
                <q-icon name="mdi-home-import-outline" size="lg"/>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ dailyTotalConsumption }}</div>
                <div class="stat-label">{{ $t('solar.daily_total_consumption') }}</div>
              </div>
            </div>
          </div>

          <div class="col-6 col-md-4">
            <div class="stat-card solar-usage-card">
              <div class="stat-icon">
                <q-icon name="mdi-home-lightning-bolt-outline" size="lg"/>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ dailySolarUsage }}</div>
                <div class="stat-label">{{ $t('solar.daily_solar_usage') }}</div>
              </div>
            </div>
          </div>

          <div class="col-6 col-md-6">
            <div class="stat-card export-card">
              <div class="stat-icon">
                <q-icon name="mdi-transmission-tower-export" size="lg"/>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ dailyGridExport }}</div>
                <div class="stat-label">{{ $t('solar.daily_grid_export') }}</div>
              </div>
            </div>
          </div>

          <div class="col-6 col-md-6">
            <div class="stat-card import-card">
              <div class="stat-icon">
                <q-icon name="mdi-transmission-tower-import" size="lg"/>
              </div>
              <div class="stat-content">
                <div class="stat-value">{{ dailyGridImport }}</div>
                <div class="stat-label">{{ $t('solar.daily_grid_import') }}</div>
              </div>
            </div>
          </div>
        </div>
      </q-card-section>
    </q-expansion-item>

    <q-separator/>

    <!-- Long-term Totals Section -->
    <q-expansion-item
      v-model="totalsSectionExpanded"
      icon="mdi-chart-line"
      :label="$t('solar.totals')"
      header-class="bg-grey-1"
      expand-separator
    >
      <q-card-section class="totals-section q-pa-md">
        <div class="row q-col-gutter-md">
          <div class="col-6 col-md-4">
            <div class="total-card">
              <q-icon name="mdi-calendar-month" class="total-icon text-amber-7"/>
              <div class="total-value">{{ monthlyYield }}</div>
              <div class="total-label">{{ $t('solar.monthly_yield') }}</div>
            </div>
          </div>

          <div class="col-6 col-md-4">
            <div class="total-card">
              <q-icon name="mdi-calendar-range" class="total-icon text-amber-8"/>
              <div class="total-value">{{ yearlyYield }}</div>
              <div class="total-label">{{ $t('solar.yearly_yield') }}</div>
            </div>
          </div>

          <div class="col-6 col-md-4">
            <div class="total-card">
              <q-icon name="mdi-sigma" class="total-icon text-green-7"/>
              <div class="total-value">{{ totalYield }}</div>
              <div class="total-label">{{ $t('solar.total_yield') }}</div>
            </div>
          </div>

          <div class="col-6">
            <div class="total-card">
              <q-icon name="mdi-arrow-up-bold-circle" class="total-icon text-blue-6"/>
              <div class="total-value">{{ totalGridExport }}</div>
              <div class="total-label">{{ $t('solar.total_grid_export') }}</div>
            </div>
          </div>

          <div class="col-6">
            <div class="total-card">
              <q-icon name="mdi-arrow-down-bold-circle" class="total-icon text-red-6"/>
              <div class="total-value">{{ totalGridImport }}</div>
              <div class="total-label">{{ $t('solar.total_grid_import') }}</div>
            </div>
          </div>
        </div>
      </q-card-section>
    </q-expansion-item>

    <!-- System Info Footer -->
    <q-card-section v-if="inverterTemp || gridFrequency || efficiency" class="system-info-section q-pa-sm bg-grey-2">
      <div class="row items-center justify-around text-caption">
        <div v-if="inverterTemp" class="info-item">
          <q-icon name="mdi-thermometer" size="xs" class="q-mr-xs text-orange-7"/>
          <span class="text-grey-7">{{ inverterTemp }}°C</span>
        </div>
        <div v-if="gridFrequency" class="info-item">
          <q-icon name="mdi-sine-wave" size="xs" class="q-mr-xs text-blue-7"/>
          <span class="text-grey-7">{{ gridFrequency }} Hz</span>
        </div>
        <div v-if="efficiency" class="info-item">
          <q-icon name="mdi-speedometer" size="xs" class="q-mr-xs text-green-7"/>
          <span class="text-grey-7">{{ efficiency }}%</span>
        </div>
      </div>
    </q-card-section>
  </q-card>
</template>

<script>
import { computed, defineComponent, onMounted, ref, toRefs } from 'vue';
import { useApolloClient } from "@vue/apollo-composable";
import { useRouter } from 'vue-router';
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
    const router = useRouter();
    const { deviceId, meterDeviceId } = toRefs(props);
    
    const device = ref({});
    const deviceDetails = ref({});
    const portIds = ref([]);
    
    // Expansion state for collapsible sections
    const dailySectionExpanded = ref(false);
    const totalsSectionExpanded = ref(false);

    /**
     * Open solar reports page
     */
    const openReports = () => {
      router.push('/solar-reports');
    };

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
      const parsed = Number.parseFloat(value);
      return Number.isNaN(parsed) ? null : parsed.toFixed(decimals);
    };

    /**
     * Format energy value - converts to MWh if >= 1000 kWh
     */
    const formatEnergy = (kwh) => {
      if (!kwh || kwh === '--') return '--';
      const value = Number.parseFloat(kwh);
      if (Number.isNaN(value)) return '--';
      
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
     * Note: inverter.active_power is already in kW from API
     */
    const solarProductionKw = computed(() => {
      const kw = getFloatValue('inverter.active_power', 2);
      if (kw === null) return '--';
      return `${kw} kW`;
    });
    
    /**
     * Solar production raw value for calculations (in W)
     */
    const solarProductionWatts = computed(() => {
      const kw = getFloatValue('inverter.active_power', 2);
      if (kw === null) return 0;
      return Number.parseFloat(kw) * 1000; // Convert kW to W
    });

    /**
     * Grid power in W (from meter)
     * Note: meter.active_power is in W (Watts), not kW
     * Negative = importing from grid
     * Positive = exporting to grid
     */
    const meterActivePowerWatts = computed(() => {
      const watts = getFloatValue('meter.active_power', 0);
      return watts ? Number.parseFloat(watts) : 0;
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
     * Total house consumption (from solar + grid)
     * = Solar production - Grid export (or + Grid import)
     * Both values converted to Watts for calculation
     */
    const totalHouseConsumptionKw = computed(() => {
      const solarWatts = solarProductionWatts.value;
      const meterWatts = meterActivePowerWatts.value;
      
      // House usage = Solar production - Grid power
      // If meter is positive (exporting), house uses less than solar produces
      // If meter is negative (importing), house uses more than solar produces
      const houseUsage = solarWatts - meterWatts;
      
      if (houseUsage < 0) return '0.00 kW';
      return (houseUsage / 1000).toFixed(2) + ' kW';
    });
    
    /**
     * Total house consumption raw value for calculations (in W)
     */
    const totalHouseConsumptionWatts = computed(() => {
      const solarWatts = solarProductionWatts.value;
      const meterWatts = meterActivePowerWatts.value;
      const houseUsage = solarWatts - meterWatts;
      return Math.max(0, houseUsage);
    });

    /**
     * Phase power and current
     * Note: meter.active_power_a/b/c are in W (Watts), not kW despite documentation
     * These are REAL power values (accounting for power factor)
     * Negative values indicate export direction, positive = import
     * Display as absolute values for clarity
     */
    const phaseAPower = computed(() => {
      const watts = getFloatValue('meter.active_power_a', 0);
      if (watts === null) return '--';
      const kw = Math.abs(Number.parseFloat(watts)) / 1000;
      return `${kw.toFixed(2)} kW`;
    });

    const phaseBPower = computed(() => {
      const watts = getFloatValue('meter.active_power_b', 0);
      if (watts === null) return '--';
      const kw = Math.abs(Number.parseFloat(watts)) / 1000;
      return `${kw.toFixed(2)} kW`;
    });

    const phaseCPower = computed(() => {
      const watts = getFloatValue('meter.active_power_c', 0);
      if (watts === null) return '--';
      const kw = Math.abs(Number.parseFloat(watts)) / 1000;
      return `${kw.toFixed(2)} kW`;
    });

    /**
     * Phase power direction indicators
     * CORRECTED: Huawei meter convention:
     * Positive value = EXPORT to grid (arrow up, blue)
     * Negative value = IMPORT from grid (arrow down, red)
     */
    const phaseADirection = computed(() => {
      const watts = getFloatValue('meter.active_power_a', 0);
      if (watts === null) return 'neutral';
      return Number.parseFloat(watts) >= 0 ? 'export' : 'import';
    });

    const phaseBDirection = computed(() => {
      const watts = getFloatValue('meter.active_power_b', 0);
      if (watts === null) return 'neutral';
      return Number.parseFloat(watts) >= 0 ? 'export' : 'import';
    });

    const phaseCDirection = computed(() => {
      const watts = getFloatValue('meter.active_power_c', 0);
      if (watts === null) return 'neutral';
      return Number.parseFloat(watts) >= 0 ? 'export' : 'import';
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
        return Math.abs(Number.parseFloat(directCurrent)).toFixed(2);
      }
      
      // Fallback: calculate from power and voltage (accounting for power factor)
      // Note: active_power_a is in W (Watts), already real power
      const powerW = Number.parseFloat(getFloatValue('meter.active_power_a', 0) || 0);
      const voltage = Number.parseFloat(getFloatValue('meter.meter_u', 0) || 240);
      const powerFactor = Number.parseFloat(getFloatValue('meter.power_factor', 2) || 1);
      
      if (voltage > 0 && powerFactor > 0) {
        return Math.abs(powerW / (voltage * powerFactor)).toFixed(2);
      }
      return '--';
    });

    const phaseBCurrent = computed(() => {
      // Use direct meter reading (most accurate)
      const directCurrent = getFloatValue('meter.b_i', 2);
      if (directCurrent !== null) {
        return Math.abs(Number.parseFloat(directCurrent)).toFixed(2);
      }
      
      // Fallback: calculate from power and voltage (accounting for power factor)
      // Note: active_power_b is in W (Watts), already real power
      const powerW = Number.parseFloat(getFloatValue('meter.active_power_b', 0) || 0);
      const voltage = Number.parseFloat(getFloatValue('meter.b_u', 0) || 240);
      const powerFactor = Number.parseFloat(getFloatValue('meter.power_factor', 2) || 1);
      
      if (voltage > 0 && powerFactor > 0) {
        return Math.abs(powerW / (voltage * powerFactor)).toFixed(2);
      }
      return '--';
    });

    const phaseCCurrent = computed(() => {
      // Use direct meter reading (most accurate)
      const directCurrent = getFloatValue('meter.c_i', 2);
      if (directCurrent !== null) {
        return Math.abs(Number.parseFloat(directCurrent)).toFixed(2);
      }
      
      // Fallback: calculate from power and voltage (accounting for power factor)
      // Note: active_power_c is in W (Watts), already real power
      const powerW = Number.parseFloat(getFloatValue('meter.active_power_c', 0) || 0);
      const voltage = Number.parseFloat(getFloatValue('meter.c_u', 0) || 240);
      const powerFactor = Number.parseFloat(getFloatValue('meter.power_factor', 2) || 1);
      
      if (voltage > 0 && powerFactor > 0) {
        return Math.abs(powerW / (voltage * powerFactor)).toFixed(2);
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
      const production = Number.parseFloat(getFloatValue('station.day_power', 2) || 0);
      const export_ = Number.parseFloat(getFloatValue('station.day_on_grid_energy', 2) || 0);
      
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
      const totalConsumption = Number.parseFloat(getFloatValue('station.day_use_energy', 2) || 0);
      const production = Number.parseFloat(getFloatValue('station.day_power', 2) || 0);
      const export_ = Number.parseFloat(getFloatValue('station.day_on_grid_energy', 2) || 0);
      
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
    // DEVICE STATUS
    // ============================================================================

    /**
     * Device connection status from backend
     */
    const deviceStatus = computed(() => {
      return device.value?.status || 'OFFLINE';
    });

    const deviceStatusColor = computed(() => {
      switch (deviceStatus.value) {
        case 'ONLINE':
          return 'positive';
        case 'OFFLINE':
          return 'negative';
        case 'DISABLED':
          return 'grey-6';
        default:
          return 'grey-6';
      }
    });

    const deviceStatusIcon = computed(() => {
      switch (deviceStatus.value) {
        case 'ONLINE':
          return 'mdi-check-circle';
        case 'OFFLINE':
          return 'mdi-alert-circle';
        case 'DISABLED':
          return 'mdi-cancel';
        default:
          return 'mdi-help-circle';
      }
    });

    const deviceStatusLabel = computed(() => {
      switch (deviceStatus.value) {
        case 'ONLINE':
          return 'Online';
        case 'OFFLINE':
          return 'Offline';
        case 'DISABLED':
          return 'Disabled';
        default:
          return 'Unknown';
      }
    });

    // ============================================================================
    // ADDITIONAL INFO
    // ============================================================================

    const systemHealth = computed(() => {
      const health = getPortValue('station.real_health_state');
      return health ? Number.parseInt(health, 10) : 3; // Default to OK
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
          // Update the port object inside the ref, not the ref itself
          deviceDetails.value[portInternalRef].value = {
            ...deviceDetails.value[portInternalRef].value,
            value: payload.p4
          };
        }
      }
    });

    return {
      device,
      deviceDetails,
      // UI state
      dailySectionExpanded,
      totalsSectionExpanded,
      // Actions
      openReports,
      // Real-time
      solarProductionKw,
      solarProductionWatts,
      totalHouseConsumptionKw,
      gridImportKw,
      gridExportKw,
      // Raw values for SVG arrows
      totalHouseConsumptionWatts,
      meterActivePowerWatts,
      // Phase data
      phaseAPower,
      phaseBPower,
      phaseCPower,
      phaseACurrent,
      phaseBCurrent,
      phaseCCurrent,
      // Phase directions
      phaseADirection,
      phaseBDirection,
      phaseCDirection,
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
      // Device Status
      deviceStatus,
      deviceStatusColor,
      deviceStatusIcon,
      deviceStatusLabel,
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
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  background: #ffffff;
}

/* Header Section */
.header-section {
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

/* Section Titles */
.section-title {
  display: flex;
  align-items: center;
  font-size: 16px;
  letter-spacing: 0.3px;
}

/* ========== REAL-TIME SECTION ========== */
.realtime-section {
  background: linear-gradient(to bottom, #fafbfc 0%, #ffffff 100%);
}

/* Power Flow Diagram */
.power-flow-container {
  padding: 20px 0;
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.03) 0%, rgba(33, 150, 243, 0.03) 100%);
  border-radius: 16px;
}

.flow-diagram {
  position: relative;
  width: 100%;
  max-width: 400px;
  height: 300px;
  margin: 0 auto;
}

.flow-node {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 10;
}

.pv-node {
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
}

.consumption-node {
  bottom: 10px;
  left: 15%;
  transform: translateX(-50%);
}

.grid-node {
  bottom: 10px;
  right: 15%;
  transform: translateX(50%);
}

.node-circle {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  padding: 12px;
  border: 3px solid transparent;
}

.node-circle:hover {
  transform: scale(1.05);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18);
}

.pv-circle {
  border-color: #ffc107;
  background: linear-gradient(135deg, #fff9e6 0%, #ffffff 100%);
}

.consumption-circle {
  border-color: #2196f3;
  background: linear-gradient(135deg, #e3f2fd 0%, #ffffff 100%);
}

.grid-circle {
  border-color: #9e9e9e;
  background: linear-gradient(135deg, #f5f5f5 0%, #ffffff 100%);
}

.node-value {
  font-size: 16px;
  margin-top: 4px;
  color: #424242;
  font-weight: 600;
}

.node-label {
  margin-top: 8px;
  text-align: center;
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* Flow Lines */
.flow-lines {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

.flow-line {
  stroke: #e0e0e0;
  stroke-width: 3;
  fill: none;
}

.flow-arrow {
  fill: #e0e0e0;
  transition: fill 0.3s ease;
}

.flow-arrow.active {
  fill: #4caf50;
  animation: pulse-arrow 1.5s ease-in-out infinite;
}

@keyframes pulse-arrow {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

/* Metric Cards */
.metric-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  border: 2px solid transparent;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: hidden;
}

.metric-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, transparent, currentColor, transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.metric-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.12);
}

.metric-card:hover::before {
  opacity: 1;
}

.solar-metric {
  border-color: rgba(255, 193, 7, 0.3);
  color: #f57c00;
}

.consumption-metric {
  border-color: rgba(33, 150, 243, 0.3);
  color: #1976d2;
}

.import-metric {
  border-color: rgba(244, 67, 54, 0.3);
  color: #d32f2f;
}

.export-metric {
  border-color: rgba(76, 175, 80, 0.3);
  color: #388e3c;
}

.metric-icon {
  margin-bottom: 8px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

.metric-value {
  font-size: 20px;
  font-weight: 700;
  margin: 8px 0;
  letter-spacing: -0.5px;
}

.metric-label {
  font-size: 11px;
  color: #757575;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  font-weight: 500;
}

/* Phase Cards */
.phases-container {
  margin-top: 24px;
  padding: 16px;
  background: rgba(255, 193, 7, 0.04);
  border-radius: 12px;
  border: 1px solid rgba(255, 193, 7, 0.1);
}

.phase-card {
  background: #ffffff;
  padding: 12px;
  border-radius: 10px;
  text-align: center;
  border: 1px solid rgba(255, 193, 7, 0.2);
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.phase-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 193, 7, 0.15);
}

.phase-header {
  font-size: 11px;
  font-weight: 600;
  color: #757575;
  text-transform: uppercase;
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.phase-power {
  font-size: 16px;
  font-weight: 700;
  color: #424242;
  margin: 4px 0;
}

.phase-current {
  font-size: 13px;
  color: #f57c00;
  font-weight: 600;
}

/* ========== DAILY SECTION ========== */
.daily-section {
  background: linear-gradient(to bottom, #ffffff 0%, #fafbfc 100%);
}

.stat-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: currentColor;
  opacity: 0.6;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: currentColor;
  color: white;
  opacity: 0.9;
}

.stat-content {
  flex: 1;
  text-align: left;
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
  color: #212121;
  margin-bottom: 2px;
  letter-spacing: -0.3px;
}

.stat-label {
  font-size: 11px;
  color: #757575;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  font-weight: 500;
}

.yield-card {
  color: #f57c00;
}

.consumption-card {
  color: #616161;
}

.solar-usage-card {
  color: #388e3c;
}

.export-card {
  color: #1976d2;
}

.import-card {
  color: #d32f2f;
}

/* ========== TOTALS SECTION ========== */
.totals-section {
  background: linear-gradient(to bottom, #fafbfc 0%, #f5f7fa 100%);
}

.total-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  border: 1px solid rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.total-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

.total-icon {
  font-size: 32px;
  margin-bottom: 12px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

.total-value {
  font-size: 20px;
  font-weight: 700;
  color: #212121;
  margin: 8px 0;
  letter-spacing: -0.5px;
}

.total-label {
  font-size: 11px;
  color: #757575;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  font-weight: 500;
  line-height: 1.4;
}

/* ========== SYSTEM INFO ========== */
.system-info-section {
  background: linear-gradient(135deg, #f5f5f5 0%, #eeeeee 100%);
}

.info-item {
  display: flex;
  align-items: center;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  font-weight: 500;
}

/* ========== RESPONSIVE DESIGN ========== */
@media (max-width: 599px) {
  .node-circle {
    width: 90px;
    height: 90px;
    padding: 8px;
  }

  .node-value {
    font-size: 13px;
  }

  .node-label {
    font-size: 9px;
  }

  .flow-diagram {
    height: 250px;
  }

  .metric-value {
    font-size: 18px;
  }

  .metric-label {
    font-size: 10px;
  }

  .stat-card {
    padding: 12px;
  }

  .stat-icon {
    width: 40px;
    height: 40px;
  }

  .stat-value {
    font-size: 16px;
  }

  .total-value {
    font-size: 16px;
  }

  .phase-power {
    font-size: 14px;
  }
}

@media (min-width: 600px) and (max-width: 1023px) {
  .node-circle {
    width: 100px;
    height: 100px;
  }
}

/* ========== ANIMATIONS ========== */
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

.metric-card,
.stat-card,
.total-card,
.phase-card {
  animation: fadeIn 0.4s ease-out;
}

/* Stagger animation for cards */
.metric-card:nth-child(1) { animation-delay: 0.05s; }
.metric-card:nth-child(2) { animation-delay: 0.1s; }
.metric-card:nth-child(3) { animation-delay: 0.15s; }
.metric-card:nth-child(4) { animation-delay: 0.2s; }

.stat-card:nth-child(1) { animation-delay: 0.05s; }
.stat-card:nth-child(2) { animation-delay: 0.1s; }
.stat-card:nth-child(3) { animation-delay: 0.15s; }
.stat-card:nth-child(4) { animation-delay: 0.2s; }
.stat-card:nth-child(5) { animation-delay: 0.25s; }
</style>

