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
    import _ from "lodash";

    function random(min, max) {
        return Math.floor(Math.random() * (max - min + 1) + min)
    }

    import {CONFIG_GLOBAL_GET_STRING_VAL} from "../../graphql/queries";

    export default {
        name: 'TempChartControl',
        data() {
            return {
                grafanaUrl: ''
            }
        },
        mounted() {
            this.init()
        },
        methods: {
            init: function() {
                this.$apollo.query({
                    query: CONFIG_GLOBAL_GET_STRING_VAL,
                    variables: {key: 'grafana.url'},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.grafanaUrl = response.data.config
                });
            }
        }
    }
</script>
<style>
    .dashboard-header {
        color: #4d80e0;


    }
</style>