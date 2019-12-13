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
                        :zoneUidLeft="zoneIntUid"
                        :zoneUidRight="zoneExtUid"
                        :categoryUid="peripheralLightUid"
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
                        icon="thermometer-half"
                        :zoneUidLeft="zoneIntUid"
                        :zoneUidRight="zoneExtUid"
                        :categoryUid="peripheralTempUid"
                >
                </ZoneActionControl>
            </CCol>
            <CCol md="3" sm="6">
                <ZoneActionControl
                        color="heat"
                        left-header="Etaj"
                        left-footer="Incalzire"
                        right-header="Parter"
                        right-footer="Incalzire"
                        icon="fire"
                        :zoneUidLeft="zoneEtajUid"
                        :zoneUidRight="zoneParterUid"
                        :categoryUid="peripheralHeatUid"
                >
                </ZoneActionControl>
            </CCol>
        </template>
    </CRow>
</template>

<script>
    import ZoneActionControl from './ZoneActionControl'

    export default {
        name: 'ZoneActionListControl',
        components: {
            ZoneActionControl
        },
        data() {
            return {
                peripheralLightUid: process.env.VUE_APP_CONF_PH_LIGHT_UID,
                peripheralTempUid: process.env.VUE_APP_CONF_PH_TEMP_UID,
                peripheralHeatUid: process.env.VUE_APP_CONF_PH_HEAT_UID,

                zoneIntUid: process.env.VUE_APP_CONF_ZONE_INT_UID,
                zoneExtUid: process.env.VUE_APP_CONF_ZONE_EXT_UID,

                zoneEtajUid: process.env.VUE_APP_CONF_ZONE_ETAJ_UID,
                zoneParterUid: process.env.VUE_APP_CONF_ZONE_PARTER_UID

            }
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
