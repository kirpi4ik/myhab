<template>
    <CRow>
        <template>
            <CCol md="3" sm="6">
                <ZoneActionControl
                        header="Iluminat"
                        color="light"
                        left-header="Interior"
                        left-footer="Iluminat"
                        right-header="Exterior"
                        right-footer="Iluminat"
                        icon="lightbulb"
                        :zoneIdLeft="zoneIntId"
                        :zoneIdRight="zoneExtId"
                        category="LIGHT"
                >
                </ZoneActionControl>
            </CCol>
            <CCol md="3" sm="6">
                <ZoneActionControl
                        header="Climatizare"
                        color="heat"
                        left-header="Etaj"
                        left-footer="Incalzire"
                        right-header="Parter"
                        right-footer="Incalzire"
                        icon="fire"
                        :zoneIdLeft="zoneEtajId"
                        :zoneIdRight="zoneParterId"
                        category="HEAT"
                >
                </ZoneActionControl>
            </CCol>
            <CCol md="3" sm="6">
                <ZoneActionControl
                        header="Temperatura"
                        color="temp"
                        left-header="Interior"
                        left-footer="Temperatura"
                        right-header="Exterior"
                        right-footer="Temperatura"
                        icon="thermometer-half"
                        :zoneIdLeft="zoneIntId"
                        :zoneIdRight="zoneExtId"
                        category="TEMP"
                >
                </ZoneActionControl>
            </CCol>
            <CCol md="3" sm="6">
                <PeriphLockControl
                        header="Poarta mica"
                        :peripheralId="peripheralLockId">
                </PeriphLockControl>
            </CCol>
        </template>
    </CRow>
</template>

<script>
    import ZoneActionControl from './ZoneActionControl'
    import PeriphLockControl from '../../zones/PeriphLockControl'

    export default {
        name: 'ZoneActionListControl',
        components: {
            ZoneActionControl,
            PeriphLockControl
        },
        data() {
            return {
                zoneIntId: process.env.VUE_APP_CONF_ZONE_INT_ID,
                zoneExtId: process.env.VUE_APP_CONF_ZONE_EXT_ID,

                zoneEtajId: process.env.VUE_APP_CONF_ZONE_ETAJ_ID,
                zoneParterId: process.env.VUE_APP_CONF_ZONE_PARTER_ID,
                peripheralLockId: Number(process.env.VUE_APP_CONF_PH_LOCK_ID)
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
