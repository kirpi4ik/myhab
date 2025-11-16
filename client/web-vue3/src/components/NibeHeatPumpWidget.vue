<template>
  <q-card class="nibe-card">
    <!-- Header -->
    <q-card-section class="header-section q-pa-md">
      <div class="row items-center justify-between">
        <div class="col-auto">
          <div class="text-h5 text-weight-medium text-grey-9">{{ $t('heat_pump.title') }}</div>
          <div class="text-caption text-grey-6">NIBE F1145-8 EM</div>
        </div>
        <div class="col-auto row q-gutter-xs">
          <!-- Device Status Badge -->
          <q-chip 
            :color="deviceStatusColor" 
            text-color="white"
            size="sm"
            :icon="deviceStatusIcon">
            {{ deviceStatusLabel }}
          </q-chip>
          <!-- Heating Status Badge -->
          <q-chip 
            :color="isHeating ? 'deep-orange-6' : 'grey-5'" 
            text-color="white"
            size="sm"
            :icon="isHeating ? 'mdi-fire' : 'mdi-power-standby'">
            {{ isHeating ? $t('heat_pump.heating') : $t('heat_pump.standby') }}
          </q-chip>
        </div>
      </div>
    </q-card-section>

    <q-separator/>

    <!-- Main Status Section -->
    <q-card-section class="status-section q-pa-md">
      <div class="row q-col-gutter-md">
        <!-- Primary Temperatures -->
        <div class="col-6">
          <div class="temp-card outdoor-temp">
            <q-icon name="mdi-sun-thermometer-outline" size="lg" class="temp-icon"/>
            <div class="temp-value">{{ outdoorTemp }}</div>
            <div class="temp-label">{{ $t('heat_pump.outdoor_temp') }}</div>
          </div>
        </div>

        <div class="col-6">
          <div class="temp-card room-temp">
            <q-icon name="mdi-home-thermometer-outline" size="lg" class="temp-icon"/>
            <div class="temp-value">{{ roomTemp }}</div>
            <div class="temp-label">{{ $t('heat_pump.room_temp') }}</div>
          </div>
        </div>

        <div class="col-6">
          <div class="temp-card hot-water-temp">
            <q-icon name="mdi-water-thermometer" size="lg" class="temp-icon"/>
            <div class="temp-value">{{ hotWaterTemp }}</div>
            <div class="temp-label">{{ $t('heat_pump.hot_water') }}</div>
          </div>
        </div>

        <div class="col-6">
          <div class="temp-card supply-temp">
            <q-icon name="mdi-thermometer-chevron-up" size="lg" class="temp-icon"/>
            <div class="temp-value">{{ supplyTemp }}</div>
            <div class="temp-label">{{ $t('heat_pump.supply_line') }}</div>
          </div>
        </div>
      </div>
    </q-card-section>

    <q-separator/>

    <!-- System Flow Visualization -->
    <q-card-section class="flow-section q-pa-md">
      <div class="section-title q-mb-md">
        <q-icon name="mdi-pipe" size="sm" class="text-blue-7 q-mr-xs"/>
        <span class="text-subtitle2 text-weight-medium text-grey-8">{{ $t('heat_pump.heating_circuit') }}</span>
      </div>
      
      <div class="flow-container">
        <div class="row items-center justify-around">
          <!-- Supply -->
          <div class="col-auto flow-metric">
            <q-icon name="mdi-arrow-up-circle" class="text-amber-6" size="md"/>
            <div class="flow-temp supply">{{ supplyTemp }}</div>
            <div class="flow-label">{{ $t('heat_pump.supply') }}</div>
          </div>

          <!-- Arrow -->
          <div class="col-auto">
            <q-icon name="mdi-arrow-right-thick" class="text-blue-5" size="xl"/>
          </div>

          <!-- Return -->
          <div class="col-auto flow-metric">
            <q-icon name="mdi-arrow-down-circle" class="text-deep-orange-6" size="md"/>
            <div class="flow-temp return">{{ returnTemp }}</div>
            <div class="flow-label">{{ $t('heat_pump.return') }}</div>
          </div>
        </div>

        <!-- Temperature Difference Indicator -->
        <div class="temp-diff-indicator q-mt-md text-center">
          <div class="text-caption text-grey-7">{{ $t('heat_pump.delta_t') }}</div>
          <div class="text-h6 text-weight-bold" :class="tempDiffColor">
            {{ tempDiff }}
          </div>
        </div>
      </div>
    </q-card-section>

    <q-separator/>

    <!-- Compressor Status -->
    <q-card-section class="compressor-section q-pa-md">
      <div class="section-title q-mb-md">
        <q-icon name="mdi-engine" size="sm" class="text-green-7 q-mr-xs"/>
        <span class="text-subtitle2 text-weight-medium text-grey-8">{{ $t('heat_pump.compressor') }}</span>
      </div>
      
      <div class="row q-col-gutter-sm">
        <div class="col-6">
          <div class="status-card">
            <div class="status-icon">
              <q-icon :name="compressorRunning ? 'mdi-engine' : 'mdi-engine-off'" size="md" :class="compressorRunning ? 'text-green-6' : 'text-grey-5'"/>
            </div>
            <div class="status-content">
              <div class="status-label">{{ $t('heat_pump.compressor_status') }}</div>
              <div class="status-value" :class="compressorRunning ? 'text-green-7' : 'text-grey-6'">
                {{ compressorRunning ? $t('heat_pump.running') : $t('heat_pump.stopped') }}
              </div>
            </div>
          </div>
        </div>

        <div class="col-6">
          <div class="status-card">
            <div class="status-icon">
              <q-icon name="mdi-counter" size="md" class="text-blue-6"/>
            </div>
            <div class="status-content">
              <div class="status-label">{{ $t('heat_pump.compressor_starts') }}</div>
              <div class="status-value text-blue-7">{{ compressorStarts }}</div>
            </div>
          </div>
        </div>

        <div class="col-12">
          <div class="status-card">
            <div class="status-icon">
              <q-icon name="mdi-clock-outline" size="md" class="text-purple-6"/>
            </div>
            <div class="status-content">
              <div class="status-label">{{ $t('heat_pump.total_operating_time') }}</div>
              <div class="status-value text-purple-7">{{ totalOperatingTime }}</div>
            </div>
          </div>
        </div>
      </div>
    </q-card-section>

    <q-separator/>

    <!-- Brine Circuit (Ground Loop) -->
    <q-expansion-item
      v-model="brineExpanded"
      icon="mdi-water-sync"
      :label="$t('heat_pump.brine_circuit')"
      header-class="bg-grey-1"
      expand-separator
    >
      <q-card-section class="brine-section q-pa-md">
        <div class="row q-col-gutter-md">
          <div class="col-6">
            <div class="brine-metric">
              <q-icon name="mdi-arrow-right-bold" class="text-green-6" size="sm"/>
              <div class="brine-temp">{{ brineInTemp }}</div>
              <div class="brine-label">{{ $t('heat_pump.brine_in') }}</div>
            </div>
          </div>
          <div class="col-6">
            <div class="brine-metric">
              <q-icon name="mdi-arrow-left-bold" class="text-light-blue-6" size="sm"/>
              <div class="brine-temp">{{ brineOutTemp }}</div>
              <div class="brine-label">{{ $t('heat_pump.brine_out') }}</div>
            </div>
          </div>
        </div>
      </q-card-section>
    </q-expansion-item>

    <q-separator/>

    <!-- Energy & Efficiency -->
    <q-expansion-item
      v-model="energyExpanded"
      icon="mdi-lightning-bolt-circle"
      :label="$t('heat_pump.energy_efficiency')"
      header-class="bg-grey-1"
      expand-separator
    >
      <q-card-section class="energy-section q-pa-md">
        <div class="row q-col-gutter-md">
          <div class="col-6">
            <div class="energy-card">
              <q-icon name="mdi-flash" class="text-amber-7" size="md"/>
              <div class="energy-value">{{ currentPower }}</div>
              <div class="energy-label">{{ $t('heat_pump.current_power') }}</div>
            </div>
          </div>
          <div class="col-6">
            <div class="energy-card">
              <q-icon name="mdi-chart-line" class="text-green-7" size="md"/>
              <div class="energy-value">{{ cop }}</div>
              <div class="energy-label">{{ $t('heat_pump.cop') }}</div>
            </div>
          </div>
          <div class="col-6">
            <div class="energy-card">
              <q-icon name="mdi-fire" class="text-deep-orange-7" size="md"/>
              <div class="energy-value">{{ totalHeatingEnergy }}</div>
              <div class="energy-label">{{ $t('heat_pump.total_heating') }}</div>
            </div>
          </div>
          <div class="col-6">
            <div class="energy-card">
              <q-icon name="mdi-water-boiler" class="text-blue-7" size="md"/>
              <div class="energy-value">{{ totalHotWaterEnergy }}</div>
              <div class="energy-label">{{ $t('heat_pump.total_hot_water') }}</div>
            </div>
          </div>
        </div>
      </q-card-section>
    </q-expansion-item>

    <!-- Last Update -->
    <q-card-section class="last-update text-center text-caption text-grey-6 q-pa-sm">
      {{ $t('heat_pump.last_update') }}: {{ lastUpdateTime }}
    </q-card-section>
  </q-card>
</template>

<script>
import { defineComponent, onMounted, ref, computed } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { useWebSocketListener } from '@/composables';
import { DEVICE_GET_BY_ID_WITH_PORT_VALUES } from '@/graphql/queries';
import _ from 'lodash';

export default defineComponent({
  name: 'NibeHeatPumpWidget',
  props: {
    deviceId: {
      type: Number,
      required: true,
      default: 1002 // Nibe heat pump device ID
    }
  },
  setup(props) {
    const { client } = useApolloClient();
    const device = ref({});
    const devicePorts = ref({});
    const portIds = ref([]);
    const lastUpdate = ref(new Date());

    // Expansion state
    const brineExpanded = ref(false);
    const energyExpanded = ref(false);

    /**
     * Load device data and organize ports by internalRef
     */
    const loadDetails = () => {
      client.query({
        query: DEVICE_GET_BY_ID_WITH_PORT_VALUES,
        variables: { id: props.deviceId },
        fetchPolicy: 'network-only',
      }).then(data => {
        device.value = data.data.device;
        
        // Organize ports by internalRef for easy access
        devicePorts.value = _.reduce(
          data.data.device.ports,
          (hash, port) => {
            hash[port.internalRef] = port;
            return hash;
          },
          {}
        );

        // Store port IDs for websocket filtering
        portIds.value = data.data.device.ports.map(port => port.id);
        lastUpdate.value = new Date();
      }).catch(error => {
        console.error('Failed to load heat pump data:', error);
      });
    };

    /**
     * Get port value safely
     * Nibe API already returns scaled values (e.g., 32.5 for 32.5°C)
     */
    const getPortValue = (internalRef, defaultValue = null, decimals = 1) => {
      const port = devicePorts.value[internalRef];
      if (port && port.value !== null && port.value !== undefined) {
        const numValue = Number.parseFloat(port.value);
        if (!Number.isNaN(numValue)) {
          return numValue.toFixed(decimals);
        }
      }
      return defaultValue;
    };

    /**
     * Get raw port value without formatting
     */
    const getRawPortValue = (internalRef, defaultValue = 0) => {
      const port = devicePorts.value[internalRef];
      if (port && port.value !== null && port.value !== undefined) {
        const numValue = Number.parseFloat(port.value);
        return Number.isNaN(numValue) ? defaultValue : numValue;
      }
      return defaultValue;
    };

    // ============================================================================
    // TEMPERATURE READINGS
    // ============================================================================

    const outdoorTemp = computed(() => {
      const temp = getPortValue('40004'); // Current outd temp (BT1)
      return temp !== null ? `${temp}°C` : '--';
    });

    const roomTemp = computed(() => {
      const temp = getPortValue('40033'); // Room temperature (BT50)
      return temp !== null ? `${temp}°C` : '--';
    });

    const hotWaterTemp = computed(() => {
      const temp = getPortValue('40014'); // Hot water charging (BT6)
      return temp !== null ? `${temp}°C` : '--';
    });

    const supplyTemp = computed(() => {
      const temp = getPortValue('40008'); // Supply line (BT2)
      return temp !== null ? `${temp}°C` : '--';
    });

    const returnTemp = computed(() => {
      const temp = getPortValue('40012'); // Return line (BT3)
      return temp !== null ? `${temp}°C` : '--';
    });

    const brineInTemp = computed(() => {
      const temp = getPortValue('40015'); // Brine in (BT10)
      return temp !== null ? `${temp}°C` : '--';
    });

    const brineOutTemp = computed(() => {
      const temp = getPortValue('40016'); // Brine out (BT11)
      return temp !== null ? `${temp}°C` : '--';
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
    // SYSTEM STATUS
    // ============================================================================

    /**
     * Determine if system is actively heating based on supply temperature
     */
    const isHeating = computed(() => {
      const supply = getRawPortValue('40008', 0);
      const returnLine = getRawPortValue('40012', 0);
      // Consider heating if supply is significantly higher than room temp
      return supply > 25 && Math.abs(supply - returnLine) > 0.5;
    });

    /**
     * Temperature difference between supply and return
     */
    const tempDiff = computed(() => {
      const supply = getRawPortValue('40008', 0);
      const returnLine = getRawPortValue('40012', 0);
      const diff = Math.abs(supply - returnLine);
      return diff > 0 ? `${diff.toFixed(1)}°C` : '--';
    });

    const tempDiffColor = computed(() => {
      const supply = getRawPortValue('40008', 0);
      const returnLine = getRawPortValue('40012', 0);
      const diff = Math.abs(supply - returnLine);
      if (diff < 1) return 'text-grey-6';
      if (diff < 3) return 'text-amber-6';
      return 'text-deep-orange-6';
    });

    // ============================================================================
    // COMPRESSOR DATA
    // ============================================================================

    const compressorRunning = computed(() => {
      // TODO: Add proper compressor status parameter when available
      // For now, infer from temperature delta
      return isHeating.value;
    });

    const compressorStarts = computed(() => {
      const starts = getRawPortValue('43416', 0); // Number of starts (EB100-EP14)
      return starts > 0 ? starts.toFixed(0) : '--';
    });

    const totalOperatingTime = computed(() => {
      const hours = getRawPortValue('43420', 0); // Total operating time compressor (EB100-EP14)
      if (hours > 0) {
        if (hours >= 8760) {
          return `${(hours / 8760).toFixed(1)} years`;
        }
        return `${hours.toFixed(0)} h`;
      }
      return '--';
    });

    // ============================================================================
    // ENERGY & EFFICIENCY
    // ============================================================================

    const currentPower = computed(() => {
      const power = getRawPortValue('40072', 0); // Flow sensor (BF1) - L/min
      return power > 0 ? `${power.toFixed(1)} L/min` : '--';
    });

    const cop = computed(() => {
      // COP = Coefficient of Performance = Heat Energy Out / Electrical Energy In
      // 
      // From Nibe data:
      // - "compressor only" = heat delivered by compressor (excludes electric heater)
      // - "including int. add. heat" = total heat (compressor + electric heater)
      // 
      // The difference tells us how much the electric heater contributed directly (COP = 1.0)
      // We can calculate the compressor's COP from this
      
      const heatingCompressorOnly = getRawPortValue('44308', 0); // Heating from compressor (kWh)
      const hotWaterCompressorOnly = getRawPortValue('44306', 0); // Hot water from compressor (kWh)
      
      const heatingTotal = getRawPortValue('44300', 0); // Total heating (kWh)
      const hotWaterTotal = getRawPortValue('44298', 0); // Total hot water (kWh)
      
      // Heat delivered by compressor only
      const compressorHeat = heatingCompressorOnly + hotWaterCompressorOnly;
      
      // Heat from electric heater (backup)
      const electricHeat = (heatingTotal - heatingCompressorOnly) + (hotWaterTotal - hotWaterCompressorOnly);
      
      // Total heat delivered
      const totalHeat = compressorHeat + electricHeat;
      
      if (totalHeat > 100) { // Only calculate if we have meaningful data
        // The electric heater uses electricHeat kWh of electricity (COP = 1.0)
        // The compressor delivered compressorHeat kWh
        // 
        // If we assume typical compressor COP between 3.0-4.5:
        // Total electricity = electricHeat + (compressorHeat / actualCOP)
        // 
        // For ground source: typical seasonal COP = 3.5-4.2
        // We can estimate: compressor used approximately compressorHeat / 3.8 kWh
        
        const estimatedCompressorElectricity = compressorHeat / 3.8; // Conservative estimate
        const totalElectricity = electricHeat + estimatedCompressorElectricity;
        
        if (totalElectricity > 0) {
          const avgCOP = totalHeat / totalElectricity;
          return avgCOP.toFixed(1);
        }
      }
      
      return '--';
    });

    const totalHeatingEnergy = computed(() => {
      const energy = getRawPortValue('44306', 0); // Total heating energy (kWh)
      return energy > 0 ? `${energy.toFixed(0)} kWh` : '--';
    });

    const totalHotWaterEnergy = computed(() => {
      const energy = getRawPortValue('44308', 0); // Total hot water energy (kWh)
      return energy > 0 ? `${energy.toFixed(0)} kWh` : '--';
    });

    /**
     * Last update time formatted
     */
    const lastUpdateTime = computed(() => {
      return lastUpdate.value.toLocaleTimeString('en-US', {
        hour: '2-digit',
        minute: '2-digit'
      });
    });

    // Load data on mount
    onMounted(() => {
      loadDetails();
    });

    // Listen for port value updates via WebSocket
    useWebSocketListener('evt_port_value_persisted', (payload) => {
      if (portIds.value.includes(Number(payload.p2))) {
        const portInternalRef = payload.p3;
        if (devicePorts.value[portInternalRef]) {
          devicePorts.value[portInternalRef].value = payload.p4;
          lastUpdate.value = new Date();
        }
      }
    });

    return {
      device,
      // UI state
      brineExpanded,
      energyExpanded,
      // Temperatures
      outdoorTemp,
      roomTemp,
      hotWaterTemp,
      supplyTemp,
      returnTemp,
      brineInTemp,
      brineOutTemp,
      // Device Status
      deviceStatus,
      deviceStatusColor,
      deviceStatusIcon,
      deviceStatusLabel,
      // Status
      isHeating,
      tempDiff,
      tempDiffColor,
      // Compressor
      compressorRunning,
      compressorStarts,
      totalOperatingTime,
      // Energy
      currentPower,
      cop,
      totalHeatingEnergy,
      totalHotWaterEnergy,
      // System
      lastUpdateTime,
      loadDetails
    };
  }
});
</script>

<style scoped>
.nibe-card {
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
  font-size: 14px;
  letter-spacing: 0.3px;
}

/* ========== STATUS SECTION ========== */
.status-section {
  background: linear-gradient(to bottom, #fafbfc 0%, #ffffff 100%);
}

.temp-card {
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

.temp-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: currentColor;
  opacity: 0.6;
}

.temp-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.12);
}

.outdoor-temp {
  border-color: rgba(33, 150, 243, 0.3);
  color: #1976d2;
}

.room-temp {
  border-color: rgba(76, 175, 80, 0.3);
  color: #388e3c;
}

.hot-water-temp {
  border-color: rgba(255, 152, 0, 0.3);
  color: #f57c00;
}

.supply-temp {
  border-color: rgba(244, 67, 54, 0.3);
  color: #d32f2f;
}

.temp-icon {
  margin-bottom: 8px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

.temp-value {
  font-size: 24px;
  font-weight: 700;
  margin: 8px 0;
  letter-spacing: -0.5px;
  color: #212121;
}

.temp-label {
  font-size: 11px;
  color: #757575;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  font-weight: 500;
}

/* ========== FLOW SECTION ========== */
.flow-section {
  background: linear-gradient(135deg, rgba(33, 150, 243, 0.03) 0%, rgba(255, 152, 0, 0.03) 100%);
}

.flow-container {
  padding: 20px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  border: 1px solid rgba(33, 150, 243, 0.1);
}

.flow-metric {
  text-align: center;
}

.flow-temp {
  font-size: 20px;
  font-weight: 700;
  margin: 8px 0;
  color: #212121;
}

.flow-temp.supply {
  color: #f57c00;
}

.flow-temp.return {
  color: #d32f2f;
}

.flow-label {
  font-size: 11px;
  color: #757575;
  text-transform: uppercase;
  font-weight: 500;
}

.temp-diff-indicator {
  padding: 12px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 8px;
  border: 1px dashed rgba(0, 0, 0, 0.1);
}

/* ========== COMPRESSOR SECTION ========== */
.compressor-section {
  background: #fafbfc;
}

.status-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.status-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.status-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.03);
}

.status-content {
  flex: 1;
  text-align: left;
}

.status-label {
  font-size: 10px;
  color: #757575;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  font-weight: 500;
  margin-bottom: 2px;
}

.status-value {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.3px;
}

/* ========== BRINE SECTION ========== */
.brine-section {
  background: linear-gradient(to bottom, #ffffff 0%, #f5f7fa 100%);
}

.brine-metric {
  background: #ffffff;
  border-radius: 10px;
  padding: 16px;
  text-align: center;
  border: 1px solid rgba(0, 0, 0, 0.06);
  transition: all 0.2s ease;
}

.brine-metric:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.brine-temp {
  font-size: 18px;
  font-weight: 700;
  margin: 8px 0;
  color: #212121;
}

.brine-label {
  font-size: 11px;
  color: #757575;
  text-transform: uppercase;
  font-weight: 500;
}

/* ========== ENERGY SECTION ========== */
.energy-section {
  background: linear-gradient(to bottom, #fafbfc 0%, #ffffff 100%);
}

.energy-card {
  background: #ffffff;
  border-radius: 10px;
  padding: 16px;
  text-align: center;
  border: 1px solid rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.energy-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.energy-value {
  font-size: 18px;
  font-weight: 700;
  margin: 8px 0;
  color: #212121;
}

.energy-label {
  font-size: 10px;
  color: #757575;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  font-weight: 500;
}

/* ========== LAST UPDATE ========== */
.last-update {
  background: rgba(0, 0, 0, 0.02);
  opacity: 0.8;
}

/* ========== RESPONSIVE DESIGN ========== */
@media (max-width: 599px) {
  .temp-value {
    font-size: 20px;
  }

  .flow-temp {
    font-size: 18px;
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

.temp-card,
.status-card,
.brine-metric,
.energy-card {
  animation: fadeIn 0.4s ease-out;
}

/* Stagger animation */
.temp-card:nth-child(1) { animation-delay: 0.05s; }
.temp-card:nth-child(2) { animation-delay: 0.1s; }
.temp-card:nth-child(3) { animation-delay: 0.15s; }
.temp-card:nth-child(4) { animation-delay: 0.2s; }
</style>

