<template>
  <q-card class="meteo-card">
    <!-- Header -->
    <q-item class="meteo-header">
      <q-item-section avatar>
        <q-avatar>
          <q-icon :name="currentWeatherIcon" size="lg"/>
        </q-avatar>
      </q-item-section>

      <q-item-section>
        <q-item-label class="text-h6">{{ locationName || $t('meteo.weather_station') }}</q-item-label>
        <q-item-label caption class="text-orange-3">{{ currentDate }}</q-item-label>
      </q-item-section>
    </q-item>

    <!-- Current Weather -->
    <q-card-section class="current-weather">
      <div class="row items-center">
        <div class="col-6 text-center">
          <q-icon :name="currentWeatherIcon" size="120px" class="weather-icon"/>
        </div>
        <div class="col-6 text-center">
          <div class="temperature-display">
            <span class="temperature-value">{{ currentTemperature }}</span>
            <span class="temperature-unit">°C</span>
          </div>
          <div class="weather-details">
            <div class="detail-item">
              <q-icon name="mdi-weather-windy" size="sm"/>
              <span>{{ currentWindSpeed }} km/h</span>
            </div>
            <div class="detail-item" v-if="currentPrecipitation !== null && currentPrecipitation > 0">
              <q-icon name="mdi-water" size="sm"/>
              <span>{{ currentPrecipitation }} mm</span>
            </div>
            <div class="detail-item" v-if="currentHumidity !== null">
              <q-icon name="mdi-water-percent" size="sm"/>
              <span>{{ currentHumidity }}%</span>
            </div>
          </div>
        </div>
      </div>
    </q-card-section>

    <!-- Daily Forecast -->
    <q-separator/>
    <q-card-section class="daily-forecast">
      <div class="forecast-title text-subtitle1 text-weight-medium q-mb-sm">
        {{ $t('meteo.forecast_3_days') }}
      </div>
      <div class="row q-col-gutter-sm">
        <div 
          v-for="(day, index) in dailyForecast" 
          :key="index"
          class="col-4"
        >
          <q-card class="forecast-day" flat bordered>
            <q-card-section class="text-center q-pa-sm">
              <div class="day-name text-weight-medium">
                <template v-if="day.dayName === 'today'">{{ $t('meteo.today') }}</template>
                <template v-else-if="day.dayName === 'tomorrow'">{{ $t('meteo.tomorrow') }}</template>
                <template v-else>{{ day.dayName }}</template>
              </div>
              <q-icon :name="day.icon" size="md" class="q-my-xs"/>
              <div class="temp-range">
                <span class="temp-max">{{ day.tempMax }}°</span>
                <span class="temp-separator">/</span>
                <span class="temp-min">{{ day.tempMin }}°</span>
              </div>
              <div class="day-details">
                <div v-if="day.rainSum > 0" class="text-caption">
                  <q-icon name="mdi-water" size="xs"/> {{ day.rainSum }} mm
                </div>
                <div class="text-caption">
                  <q-icon name="mdi-weather-windy" size="xs"/> {{ day.windMax }} km/h
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>
    </q-card-section>

    <!-- Sunrise/Sunset -->
    <q-separator/>
    <q-card-section class="sun-info">
      <div class="row q-col-gutter-md">
        <div class="col-6 text-center">
          <q-icon name="mdi-weather-sunset-up" size="md" color="orange-6"/>
          <div class="text-caption text-orange-3">{{ $t('meteo.sunrise') }}</div>
          <div class="text-weight-medium">{{ todaySunrise }}</div>
        </div>
        <div class="col-6 text-center">
          <q-icon name="mdi-weather-sunset-down" size="md" color="orange-8"/>
          <div class="text-caption text-orange-3">{{ $t('meteo.sunset') }}</div>
          <div class="text-weight-medium">{{ todaySunset }}</div>
        </div>
      </div>
    </q-card-section>

    <!-- Additional Info -->
    <q-separator/>
    <q-card-section class="additional-info">
      <div class="row q-col-gutter-sm text-center">
        <div class="col-6">
          <div class="info-box">
            <q-icon name="mdi-white-balance-sunny" size="sm" color="amber-6"/>
            <div class="text-caption text-orange-7">{{ $t('meteo.sunshine') }}</div>
            <div class="text-weight-medium">{{ todaySunshineDuration }} h</div>
          </div>
        </div>
        <div class="col-6">
          <div class="info-box">
            <q-icon name="mdi-clock-outline" size="sm" color="blue-6"/>
            <div class="text-caption text-orange-7">{{ $t('meteo.daylight') }}</div>
            <div class="text-weight-medium">{{ todayDaylightDuration }} h</div>
          </div>
        </div>
      </div>
    </q-card-section>

    <!-- Last Update -->
    <q-card-section class="last-update text-center text-caption text-grey-6">
      {{ $t('meteo.last_update') }}: {{ lastUpdateTime }}
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
  name: 'MeteoStationCard',
  props: {
    deviceId: {
      type: Number,
      required: true,
      default: 2000
    },
    locationName: {
      type: String,
      default: null
    }
  },
  setup(props) {
    const { client } = useApolloClient();
    const device = ref({});
    const devicePorts = ref({});
    const portIds = ref([]);
    const lastUpdate = ref(new Date());

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
        console.error('Failed to load meteo station data:', error);
      });
    };

    /**
     * Get port value safely
     */
    const getPortValue = (internalRef, defaultValue = null) => {
      const port = devicePorts.value[internalRef];
      if (port && port.value !== null && port.value !== undefined) {
        return port.value;
      }
      return defaultValue;
    };

    /**
     * Parse JSON array from port value
     */
    const getPortArrayValue = (internalRef) => {
      const value = getPortValue(internalRef);
      
      // If value is already an array, return it
      if (Array.isArray(value)) {
        return value;
      }
      
      // If value is a string, try to parse it
      if (typeof value === 'string') {
        try {
          const parsed = JSON.parse(value);
          return Array.isArray(parsed) ? parsed : [];
        } catch (e) {
          console.warn(`Failed to parse ${internalRef}:`, value, e);
        }
      }
      
      return [];
    };

    /**
     * Current date formatted
     */
    const currentDate = computed(() => {
      const now = new Date();
      return now.toLocaleDateString('en-US', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    });

    /**
     * Current temperature from current weather data
     */
    const currentTemperature = computed(() => {
      const temp = getPortValue('current.temperature_2m');
      if (temp !== null) {
        return Math.round(parseFloat(temp));
      }
      return '--';
    });

    /**
     * Current wind speed from current weather data
     */
    const currentWindSpeed = computed(() => {
      const wind = getPortValue('current.wind_speed_10m');
      if (wind !== null) {
        return Math.round(parseFloat(wind));
      }
      return '--';
    });

    /**
     * Current precipitation from current weather data
     */
    const currentPrecipitation = computed(() => {
      const precip = getPortValue('current.precipitation');
      if (precip !== null) {
        return parseFloat(precip);
      }
      return null;
    });

    /**
     * Current humidity from current weather data
     */
    const currentHumidity = computed(() => {
      const humidity = getPortValue('current.relative_humidity_2m');
      if (humidity !== null) {
        return Math.round(parseFloat(humidity));
      }
      return null;
    });

    /**
     * Weather icon based on weather code from current data
     * Using WMO Weather interpretation codes (WW)
     * https://www.nodc.noaa.gov/archive/arc0021/0002199/1.1/data/0-data/HTML/WMO-CODE/WMO4677.HTM
     */
    const currentWeatherIcon = computed(() => {
      const weatherCode = getPortValue('current.weather_code');
      const isDay = getPortValue('current.is_day');
      
      if (weatherCode === null) {
        // Fallback to simple logic
        const precip = currentPrecipitation.value;
        const temp = currentTemperature.value;
        
        if (precip > 0) {
          return 'mdi-weather-rainy';
        } else if (temp < 0) {
          return 'mdi-weather-snowy';
        } else if (temp > 25) {
          return 'mdi-weather-sunny';
        } else {
          return 'mdi-weather-partly-cloudy';
        }
      }
      
      const code = parseInt(weatherCode);
      const dayIcon = isDay === 1 || isDay === '1';
      
      // Clear sky
      if (code === 0) {
        return dayIcon ? 'mdi-weather-sunny' : 'mdi-weather-night';
      }
      // Mainly clear, partly cloudy
      if (code <= 3) {
        return dayIcon ? 'mdi-weather-partly-cloudy' : 'mdi-weather-night-partly-cloudy';
      }
      // Fog
      if (code <= 48) {
        return 'mdi-weather-fog';
      }
      // Drizzle
      if (code <= 57) {
        return 'mdi-weather-partly-rainy';
      }
      // Rain
      if (code <= 67) {
        return 'mdi-weather-rainy';
      }
      // Snow
      if (code <= 77) {
        return 'mdi-weather-snowy';
      }
      // Rain showers
      if (code <= 82) {
        return 'mdi-weather-pouring';
      }
      // Snow showers
      if (code <= 86) {
        return 'mdi-weather-snowy-heavy';
      }
      // Thunderstorm
      if (code <= 99) {
        return 'mdi-weather-lightning-rainy';
      }
      
      return 'mdi-weather-partly-cloudy';
    });

    /**
     * Daily forecast (3 days)
     */
    const dailyForecast = computed(() => {
      const times = getPortArrayValue('daily.time');
      const tempMin = getPortArrayValue('daily.apparent_temperature_min');
      const tempMax = getPortArrayValue('daily.apparent_temperature_max');
      const rainSum = getPortArrayValue('daily.rain_sum');
      const windMax = getPortArrayValue('daily.wind_speed_10m_max');

      const forecast = [];

      for (let i = 0; i < Math.min(3, times.length); i++) {
        const date = new Date(times[i]);
        
        // Use translation keys for first two days, browser locale for others
        let dayName;
        if (i === 0) {
          dayName = 'today';  // Will be translated in template
        } else if (i === 1) {
          dayName = 'tomorrow';  // Will be translated in template
        } else {
          dayName = date.toLocaleDateString(undefined, { weekday: 'short' });
        }
        
        forecast.push({
          dayName,
          date: times[i],
          tempMin: tempMin[i] !== undefined ? Math.round(tempMin[i]) : '--',
          tempMax: tempMax[i] !== undefined ? Math.round(tempMax[i]) : '--',
          rainSum: rainSum[i] || 0,
          windMax: windMax[i] !== undefined ? Math.round(windMax[i]) : '--',
          icon: rainSum[i] > 0 ? 'mdi-weather-rainy' : 'mdi-weather-partly-cloudy'
        });
      }

      return forecast;
    });

    /**
     * Today's sunrise time
     */
    const todaySunrise = computed(() => {
      const sunrises = getPortArrayValue('daily.sunrise');
      if (sunrises.length > 0) {
        const time = new Date(sunrises[0]);
        return time.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
      }
      return '--:--';
    });

    /**
     * Today's sunset time
     */
    const todaySunset = computed(() => {
      const sunsets = getPortArrayValue('daily.sunset');
      if (sunsets.length > 0) {
        const time = new Date(sunsets[0]);
        return time.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
      }
      return '--:--';
    });

    /**
     * Today's sunshine duration in hours
     */
    const todaySunshineDuration = computed(() => {
      const durations = getPortArrayValue('daily.sunshine_duration');
      if (durations.length > 0) {
        return (durations[0] / 3600).toFixed(1);
      }
      return '--';
    });

    /**
     * Today's daylight duration in hours
     */
    const todayDaylightDuration = computed(() => {
      const durations = getPortArrayValue('daily.daylight_duration');
      if (durations.length > 0) {
        return (durations[0] / 3600).toFixed(1);
      }
      return '--';
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
        loadDetails();
      }
    });

    return {
      device,
      currentDate,
      currentTemperature,
      currentWindSpeed,
      currentPrecipitation,
      currentHumidity,
      currentWeatherIcon,
      dailyForecast,
      todaySunrise,
      todaySunset,
      todaySunshineDuration,
      todayDaylightDuration,
      lastUpdateTime,
      loadDetails
    };
  }
});
</script>

<style scoped>
.meteo-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.meteo-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.3);
}

.meteo-header {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  color: white;
}

.current-weather {
  background: rgba(255, 255, 255, 0.05);
  padding: 20px;
}

.weather-icon {
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
}

.temperature-display {
  margin-bottom: 16px;
}

.temperature-value {
  font-size: 64px;
  font-weight: 300;
  line-height: 1;
}

.temperature-unit {
  font-size: 32px;
  font-weight: 300;
  vertical-align: top;
}

.weather-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  opacity: 0.9;
}

.daily-forecast {
  background: rgba(255, 255, 255, 0.08);
  padding: 16px;
}

.forecast-title {
  color: white;
  opacity: 0.95;
}

.forecast-day {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  transition: transform 0.2s ease;
}

.forecast-day:hover {
  transform: scale(1.05);
  background: rgba(255, 255, 255, 0.2);
}

.day-name {
  font-size: 14px;
  color: white;
  margin-bottom: 4px;
}

.temp-range {
  font-size: 18px;
  font-weight: 500;
  margin-top: 4px;
}

.temp-max {
  color: #ffeb3b;
}

.temp-separator {
  opacity: 0.6;
  margin: 0 4px;
}

.temp-min {
  color: #90caf9;
}

.day-details {
  margin-top: 8px;
  opacity: 0.9;
}

.sun-info {
  background: rgba(255, 255, 255, 0.05);
  padding: 16px;
}

.additional-info {
  background: rgba(255, 255, 255, 0.03);
  padding: 16px;
}

.info-box {
  padding: 8px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
}

.last-update {
  background: rgba(0, 0, 0, 0.1);
  padding: 8px;
  opacity: 0.8;
}

/* Responsive adjustments */
@media (max-width: 600px) {
  .temperature-value {
    font-size: 48px;
  }
  
  .temperature-unit {
    font-size: 24px;
  }
}
</style>


