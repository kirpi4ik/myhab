<template>
  <q-card class="text-white">
    <q-item v-if="grafanaUrl">
      <iframe :src="grafanaUrl"
              width="100%" height="400" frameborder="0"></iframe>
    </q-item>
    <q-item v-else>
      <q-item-section class="text-center text-grey-6 q-pa-md">
        <q-icon name="mdi-chart-line" size="md" class="q-mb-sm"/>
        <div class="text-subtitle2">{{ $t('temp_chart.not_configured') }}</div>
        <div class="text-caption">
          {{ $t('temp_chart.hint', { keys: 'grafana.url, grafana.dashboard.temperature.id, grafana.dashboard.temperature.panelId' }) }}
        </div>
      </q-item-section>
    </q-item>
  </q-card>
</template>
<script>
import {computed, defineComponent} from "vue";
import {useAppConfigStore} from 'src/store/app-config.store';

export default defineComponent({
  name: 'TempChartCard',
  setup() {
    const appConfig = useAppConfigStore();

    /*
     * Composed from configuration rather than hardcoded: the Grafana host and the
     * dashboard/panel ids belong to whoever installed myHAB, not to the product.
     * Same key convention as SolarReports (grafana.url + grafana.dashboard.<name>.id).
     */
    const grafanaUrl = computed(() => {
      const base = appConfig.get('grafana.url');
      const dashboardId = appConfig.get('grafana.dashboard.temperature.id');
      const panelId = appConfig.get('grafana.dashboard.temperature.panelId');
      if (!base || !dashboardId || !panelId) {
        return null;
      }
      return `${base.replace(/\/+$/, '')}/d-solo/${dashboardId}/temperatura`
        + `?orgId=1&theme=light&panelId=${panelId}`;
    });

    return {
      grafanaUrl
    }
  }
})

</script>
