<template>
  <q-page padding>
    <div class="q-pa-md">
      <!-- Header -->
      <div class="row items-center q-mb-lg">
        <div class="col">
          <div class="text-h4 text-grey-9">
            <q-icon name="mdi-solar-power" size="md" class="q-mr-sm text-amber-7"/>
            Solar Plant Reports
          </div>
          <div class="text-subtitle2 text-grey-6">
            Real-time monitoring and historical data analysis
          </div>
        </div>
        <div class="col-auto">
          <q-btn 
            outline 
            color="grey-7" 
            icon="mdi-arrow-left" 
            label="Back"
            @click="$router.go(-1)"
          />
        </div>
      </div>

      <!-- Time Range Selector -->
      <div class="row q-mb-md">
        <div class="col-12">
          <q-card flat bordered>
            <q-card-section class="q-pa-md">
              <div class="row items-center q-gutter-md">
                <div class="col-auto">
                  <q-icon name="mdi-clock-outline" size="sm" class="text-grey-6"/>
                  <span class="text-subtitle2 text-grey-8 q-ml-sm">Time Range:</span>
                </div>
                <div class="col-auto">
                  <q-btn-toggle
                    v-model="timeRange"
                    spread
                    no-caps
                    toggle-color="primary"
                    :options="timeRangeOptions"
                    @update:model-value="updateDashboards"
                  />
                </div>
                <div class="col-auto" v-if="timeRange === 'custom'">
                  <q-input
                    v-model="customFrom"
                    filled
                    dense
                    label="From"
                    type="datetime-local"
                    @update:model-value="updateDashboards"
                  />
                </div>
                <div class="col-auto" v-if="timeRange === 'custom'">
                  <q-input
                    v-model="customTo"
                    filled
                    dense
                    label="To"
                    type="datetime-local"
                    @update:model-value="updateDashboards"
                  />
                </div>
                <div class="col-auto">
                  <q-btn
                    flat
                    round
                    color="primary"
                    icon="mdi-refresh"
                    @click="refreshDashboards"
                  >
                    <q-tooltip>Refresh Dashboards</q-tooltip>
                  </q-btn>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Grafana Dashboard -->
      <div class="row">
        <div class="col-12">
          <q-card flat bordered class="dashboard-card">
            <q-card-section class="bg-grey-2 q-pa-sm">
              <div class="row items-center">
                <div class="col">
                  <div class="text-subtitle1 text-weight-medium text-grey-8">
                    <q-icon name="mdi-solar-power" class="q-mr-xs"/>
                    Solar Plant Dashboard
                  </div>
                </div>
                <div class="col-auto">
                  <q-btn
                    flat
                    dense
                    round
                    size="sm"
                    icon="mdi-fullscreen"
                    @click="openFullscreen(grafanaDashboardUrl)"
                  >
                    <q-tooltip>Open in Fullscreen</q-tooltip>
                  </q-btn>
                  <q-btn
                    flat
                    dense
                    round
                    size="sm"
                    icon="mdi-open-in-new"
                    @click="openInNewTab(grafanaDashboardUrl)"
                  >
                    <q-tooltip>Open in New Tab</q-tooltip>
                  </q-btn>
                </div>
              </div>
            </q-card-section>
            <q-separator/>
            <q-card-section class="q-pa-none dashboard-container">
              <iframe
                v-if="grafanaDashboardUrl"
                :src="grafanaDashboardUrl"
                title="Solar Plant Dashboard"
                class="grafana-iframe"
                :key="refreshKey + '-dashboard'"
              />
              <div v-else class="row items-center justify-center q-pa-xl text-grey-6">
                <div class="column items-center">
                  <q-icon name="mdi-alert-circle-outline" size="xl" class="q-mb-md"/>
                  <div class="text-subtitle1">Dashboard URL not configured</div>
                  <div class="text-caption">Please configure GRAFANA_DASHBOARD_1_ID in settings</div>
                </div>
              </div>
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Configuration Help Dialog -->
      <q-dialog v-model="showConfigHelp">
        <q-card style="min-width: 500px">
          <q-card-section class="bg-primary text-white">
            <div class="text-h6">
              <q-icon name="mdi-information" class="q-mr-sm"/>
              Dashboard Configuration
            </div>
          </q-card-section>
          <q-separator/>
          <q-card-section>
            <div class="text-body1 q-mb-md">
              To configure Grafana dashboard URLs, add the following environment variables or configuration:
            </div>
            <q-list bordered separator>
              <q-item>
                <q-item-section>
                  <q-item-label class="text-weight-medium">GRAFANA_URL</q-item-label>
                  <q-item-label caption>Grafana server base URL (e.g., http://localhost:3000)</q-item-label>
                </q-item-section>
              </q-item>
              <q-item>
                <q-item-section>
                  <q-item-label class="text-weight-medium">GRAFANA_DASHBOARD_1_ID</q-item-label>
                  <q-item-label caption>Dashboard ID from Grafana URL (/d/dashboard-id/...)</q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
            <div class="text-caption text-grey-7 q-mt-md">
              <strong>Example:</strong> If your dashboard URL is<br>
              <code>http://grafana.madhouse.app/d/abc123/solar-plant</code><br>
              Then set:<br>
              • GRAFANA_URL=http://grafana.madhouse.app<br>
              • GRAFANA_DASHBOARD_1_ID=abc123
            </div>
          </q-card-section>
          <q-card-actions align="right">
            <q-btn flat label="Close" color="primary" v-close-popup/>
          </q-card-actions>
        </q-card>
      </q-dialog>

      <!-- Floating Action Button for Config Help -->
      <q-page-sticky position="bottom-right" :offset="[18, 18]">
        <q-btn
          fab
          icon="mdi-help-circle"
          color="primary"
          @click="showConfigHelp = true"
        >
          <q-tooltip>Configuration Help</q-tooltip>
        </q-btn>
      </q-page-sticky>
    </div>
  </q-page>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue';

export default defineComponent({
  name: 'SolarReports',
  setup() {
    const timeRange = ref('6h');
    const customFrom = ref('');
    const customTo = ref('');
    const refreshKey = ref(0);
    const showConfigHelp = ref(false);

    const timeRangeOptions = [
      { label: '1h', value: '1h' },
      { label: '6h', value: '6h' },
      { label: '12h', value: '12h' },
      { label: '24h', value: '24h' },
      { label: '7d', value: '7d' },
      { label: '30d', value: '30d' },
      { label: 'Custom', value: 'custom' }
    ];

    // Base Grafana URLs - configured via environment variables
    // Add these to your .env file or set them in deployment
    const grafanaBaseUrl = process.env.GRAFANA_URL || 'http://localhost:3000';
    const grafanaDashboardId = process.env.GRAFANA_DASHBOARD_1_ID || '';

    /**
     * Build Grafana dashboard URL with time range parameters
     */
    const buildDashboardUrl = (dashboardId) => {
      if (!dashboardId) return null;

      let fromParam = '';
      let toParam = 'now';

      if (timeRange.value === 'custom' && customFrom.value && customTo.value) {
        fromParam = new Date(customFrom.value).getTime();
        toParam = new Date(customTo.value).getTime();
      } else {
        fromParam = `now-${timeRange.value}`;
      }

      const params = new URLSearchParams({
        orgId: '1',
        theme: 'light',
        from: fromParam,
        to: toParam,
        refresh: '30s',
        kiosk: 'tv'
      });

      return `${grafanaBaseUrl}/d/${dashboardId}?${params.toString()}`;
    };

    const grafanaDashboardUrl = computed(() => buildDashboardUrl(grafanaDashboardId));

    /**
     * Refresh all dashboards by updating the refresh key
     */
    const refreshDashboards = () => {
      refreshKey.value++;
    };

    /**
     * Update dashboards when time range changes
     */
    const updateDashboards = () => {
      refreshKey.value++;
    };

    /**
     * Open dashboard in fullscreen mode
     */
    const openFullscreen = (url) => {
      if (!url) return;
      window.open(url, '_blank');
    };

    /**
     * Open dashboard in new tab
     */
    const openInNewTab = (url) => {
      if (!url) return;
      const urlWithoutKiosk = url.replace('kiosk=tv', 'kiosk=false');
      window.open(urlWithoutKiosk, '_blank');
    };

    onMounted(() => {
      // Check if dashboard URL is configured
      if (!grafanaDashboardId) {
        console.warn('No Grafana dashboard ID configured. Please set GRAFANA_DASHBOARD_1_ID environment variable.');
      }
    });

    return {
      timeRange,
      customFrom,
      customTo,
      timeRangeOptions,
      grafanaDashboardUrl,
      refreshKey,
      showConfigHelp,
      refreshDashboards,
      updateDashboards,
      openFullscreen,
      openInNewTab
    };
  }
});
</script>

<style scoped>
.dashboard-card {
  border-radius: 8px;
  overflow: hidden;
}

.dashboard-container {
  position: relative;
  width: 100%;
  height: 600px;
  background: #f5f5f5;
}

.grafana-iframe {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}

/* Responsive heights */
@media (max-width: 599px) {
  .dashboard-container {
    height: 450px;
  }
}

@media (min-width: 600px) and (max-width: 1023px) {
  .dashboard-container {
    height: 550px;
  }
}

@media (min-width: 1024px) {
  .dashboard-container {
    height: 700px;
  }
}
</style>

