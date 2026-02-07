<template>
  <q-card class="text-white">
    <q-item>
      <iframe :src="grafanaUrl"
              width="100%" height="400" frameborder="0"></iframe>
    </q-item>
  </q-card>
</template>
<script>
import {defineComponent} from "vue";



export default defineComponent({
  name: 'TempChartCard',
  setup() {
    const grafanaUrl = 'https://grafana.madhouse.app/d-solo/a588yoxWz/temperatura?orgId=1&theme=light&panelId=4'
    return {
      grafanaUrl
    }
  }
})

</script>
