<template>
    <CCard>
        <CCardBody>
            <CRow>
                <iframe :src="grafanaUrl+'/d-solo/a588yoxWz/temperatura?orgId=1&theme=light&panelId=4'"
                        width="100%" height="400" frameborder="0"></iframe>
            </CRow>
        </CCardBody>
    </CCard>
</template>
<script>
    import {CChartLine} from '@coreui/vue-chartjs'

    function random(min, max) {
        return Math.floor(Math.random() * (max - min + 1) + min)
    }

    export default {
        name: 'TempChartControl',
        components: {
            CChartLine
        },
        data() {
            return {
                grafanaUrl: process.env.VUE_APP_CONF_GRAFANA_URL
            }
        }
    }
</script>
<style>
    .dashboard-header {
        color: #4d80e0;


    }
</style>