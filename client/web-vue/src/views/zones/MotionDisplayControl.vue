<template>
    <div style="display: inline">
        <div v-for="port in motionDisplay.portIds" style="display: inline-block" class="temp_display" v-bind:key="port.uid"> {{port.value}} </div>
    </div>
</template>
<script>
    export default {
        name: 'MotionDisplayControl',
        props: {
            zone: Object
        },
        data() {
            return {
                motionDisplay: {
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
                let portIds = this.motionDisplay.portIds;
                this.zone.peripherals.forEach(function (peripheral, index) {
                    if (peripheral.category.name == 'MOTION') {
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