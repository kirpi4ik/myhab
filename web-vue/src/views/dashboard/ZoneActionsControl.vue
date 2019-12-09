<template>
    <CRow>
        <template>
            <CCol md="3" sm="6">
                <ZoneActionControl
                        color="light"
                        left-header="Interior"
                        left-footer="Iluminat"
                        right-header="Exterior"
                        right-footer="Iluminat"
                        icon="lightbulb"
                >
                </ZoneActionControl>
            </CCol>
            <CCol md="3" sm="6">
                <ZoneActionControl
                        color="temp"
                        left-header="Interior"
                        left-footer="Temperatura"
                        right-header="Exterior"
                        right-footer="Temperatura"
                        icon="thermometer-half">
                </ZoneActionControl>
            </CCol>
            <CCol md="3" sm="6">
                <ZoneActionControl
                        color="heat"
                        left-header="Etaj"
                        left-footer="Incalzire"
                        right-header="Parter"
                        right-footer="Incalzire"
                        icon="fire">
                </ZoneActionControl>
            </CCol>
        </template>
    </CRow>
</template>

<script>
    import ZoneActionControl from '../widgets/zoneaction/ZoneActionControl'

    export default {
        name: 'ZoneActionsControl',
        components: {
            ZoneActionControl
        }
    }

</script>

<style scoped>
    .c-chart-brand {
        position: absolute;
        width: 100%;
        height: 100px;
    }
</style>
