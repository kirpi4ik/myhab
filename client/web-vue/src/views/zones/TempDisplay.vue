<template>
    <div style="display: inline">
        <div v-for="port in tempDisplay.portIds" style="display: inline-block" class="temp_display" v-bind:key="port.id"> {{port.value}} &#176;C</div>
    </div>
</template>
<script>
    export default {
        name: 'TempDisplay',
        props: {
            zone: Object
        },
        data() {
            return {
                tempDisplay: {
                    show: false,
                    portIds: []
                },
            }
        },
        created() {
            this.loadInitial();
        },
        methods: {
            loadInitial() {
                let portIds = this.tempDisplay.portIds;
                this.zone.peripherals.forEach(function (peripheral, index) {
                    if (peripheral.category.name == process.env.VUE_APP_CONF_PH_TEMP) {
                        peripheral.connectedTo.forEach(function (port, index) {
                            portIds.push(port);
                        });
                    }
                });
            }
        }
    }
</script>
<style scoped>
    div.temp_display {
        margin-bottom: 2pt;
        margin-left: 2pt;
        padding: 5pt;
        color: #74f0a2;
        text-decoration-color: #afb2c9;
        background-color: #818792;
        border: 1px solid rgba(0, 0, 0, 0.3);;
    }
</style>