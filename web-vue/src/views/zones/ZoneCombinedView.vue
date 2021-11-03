<template>
    <div>
        <CRow>
            <div class="bottom-space">
                <CLink v-on:click="navRoot">Acasa</CLink>
                >
            </div>
            <div v-for="link in breadcrumb" v-bind:key="link.zoneUid" class="bottom-space">
                <CLink v-text="link.name" v-on:click="navZone(link.zoneUid)"></CLink>
                >
            </div>
        </CRow>
        <CRow>
            <CCol md="3" sm="6" v-for="zone in zones" v-bind:key="zone.uid">
                <div class="card" :class="`zone-card-background text-white`">
                    <div class="card-body">
                        <div style="display: inline">
                            <HeatScheduler :zone="zone" :name="zone.name" v-if="categoryUid === peripheralHeatUid"/>
                            <TempDisplay :zone="zone" :name="zone.name" v-if="categoryUid === peripheralTempUid"/>
                        </div>
                        <div v-on:click="navZone(zone.uid)" style="cursor:pointer; height: 100%">
                            <h2 class="mb-0">{{zone.name}}</h2>
                        </div>
                    </div>
                    <slot name="footer" class="card-footer">
                        <div class="zone-card-footer" v-on:click="navZone(zone.uid)" style="cursor:pointer">
                            <slot></slot>
                        </div>
                    </slot>
                </div>
            </CCol>
        </CRow>
        <CRow>
            <CCol md="3" sm="6" v-for="peripheral in peripherals" v-bind:key="peripheral.uid">
                <PeriphLightControl :peripheral="peripheral" v-if="categoryUid === peripheralLightUid"></PeriphLightControl>
                <PeriphTempControl :peripheral="peripheral" v-if="categoryUid === peripheralTempUid"></PeriphTempControl>
                <PeriphHeatControl :peripheral="peripheral" v-if="categoryUid === peripheralHeatUid"></PeriphHeatControl>
            </CCol>
        </CRow>
        <CRow>
            <CCol>
                <TempChartControl v-if="categoryUid === peripheralTempUid"/>

            </CCol>
        </CRow>

    </div>

</template>
<script>
    import {router} from '@/_helpers';

    import {NAV_BREADCRUMB, ZONE_GET_BY_UID, ZONES_GET_ROOT,} from "../../graphql/queries";
    import PeriphLightControl from './PeriphLightControl'
    import PeriphHeatControl from './PeriphHeatControl'
    import PeriphTempControl from './PeriphTempControl'
    import HeatScheduler from './HeatScheduler'
    import TempDisplay from "./TempDisplay";
    import TempChartControl from "./TempChartControl";
    import _ from "lodash";

    export default {
        name: 'ZoneCombinedView',
        components: {
            TempDisplay,
            PeriphLightControl,
            PeriphTempControl,
            PeriphHeatControl,
            HeatScheduler,
            TempChartControl
        },
        data() {
            return {
                zone: [],
                zones: [],
                peripherals: [],
                breadcrumb: [],
                categoryUid: this.$route.query.categoryUid,
                peripheralLightUid: process.env.VUE_APP_CONF_PH_LIGHT_UID,
                peripheralTempUid: process.env.VUE_APP_CONF_PH_TEMP_UID,
                peripheralHeatUid: process.env.VUE_APP_CONF_PH_HEAT_UID
            }
        },
        created() {
            this.loadInitial();
        },
        computed: {
            stompMessage() {
                return this.$store.state.stomp.message
            }
        },
        watch: {
            '$route.path': 'loadInitial',
            stompMessage: function (newVal) {
                if (newVal.eventName == 'evt_port_value_changed') {
                    this.loadInitial();
                }
            }
        },
        methods: {
            loadInitial() {
                let peripherals = [];
                let peripheralUids = [];
                let zones = [];
                let zone = [];
                let periphInitCallback = function (peripheral) {
                    if (peripheral != null) {
                        let portValue = null;
                        let state = false;
                        let portId = null;
                        let portUid = null;
                        let deviceState = null;
                        if (peripheral.connectedTo && peripheral.connectedTo.length > 0) {
                            let port = peripheral.connectedTo[0];
                            if (port != null && port.device != null) {
                                portValue = port.value;
                                state = portValue === 'OFF';
                                portId = port.id;
                                portUid = port.uid;
                                deviceState = port.device.status
                            } else {
                                port = null;
                                state = false;
                                deviceState = null;
                            }
                        }
                        if (peripheralUids.indexOf(peripheral.uid) === -1) {
                            peripherals.push({
                                data: peripheral,
                                portValue: portValue,
                                state: state,
                                portId: portId,
                                portUid: portUid,
                                deviceState: deviceState
                            });
                            peripheralUids.push(peripheral.uid)
                        }
                    }
                };
                let zoneInitCallback = function (childZone) {
                    childZone.peripherals.sort((a, b) => (a.name > b.name) ? 1 : -1).forEach(periphInitCallback);
                };
                let peripheralFilter = function (peripheral) {
                    //filter out peripheral which has categories different from the one requested in url
                    return peripheral.data.category.uid === this.$route.query.categoryUid
                }.bind(this);
                //query for root zones , where zones has parent zone null


                if (this.$route.query.zoneUid != null && this.$route.query.zoneUid !== "") {
                    this.$apollo.query({
                        query: ZONE_GET_BY_UID,
                        variables: {uid: this.$route.query.zoneUid},
                        fetchPolicy: 'network-only'
                    }).then(response => {
                        let data = _.cloneDeep(response.data)
                        zone = data.zoneByUid;
                        zones = zone.zones;
                        zone.peripherals.sort((a, b) => (a.name > b.name) ? 1 : -1).forEach(periphInitCallback);
                        zones.forEach(zoneInitCallback);
                        this.zone = zone;
                        this.zones = zones;
                        this.peripherals = peripherals.filter(peripheralFilter)
                    });
                } else {
                    this.$apollo.query({
                        query: ZONES_GET_ROOT,
                        variables: {},
                        fetchPolicy: 'network-only'
                    }).then(response => {
                        let data = _.cloneDeep(response.data)
                        zones = data.zonesRoot;
                        zones.forEach(zoneInitCallback);
                        this.zone = zone;
                        this.zones = zones;
                        this.peripherals = peripherals.filter(peripheralFilter);
                    });
                }
                let zoneUid = null;
                if (this.$route.query.zoneUid !== "") {
                    zoneUid = this.$route.query.zoneUid
                }
                this.$apollo.query({
                    query: NAV_BREADCRUMB,
                    variables: {zoneUid: zoneUid}
                }).then(response => {
                    if (response.data.navigation != null) {
                        this.breadcrumb = response.data.navigation.breadcrumb;
                    }
                });
            },
            navZone: function (zoneUid) {
                router.push({path: 'zones', query: {zoneUid: zoneUid, categoryUid: this.$route.query.categoryUid}})
            },
            navRoot: function () {
                router.push({path: 'zones', query: {zoneUid: "", categoryUid: this.$route.query.categoryUid}})
            },


        }
    }
</script>
<style scoped>
    .zone-card-background {
        display: block;
        position: relative;
        background-image: url("../../assets/images/layer-1.png"), linear-gradient(#546e82, #7c919f);
        background-size: 300px;
    }

    .card-background {
        background-image: linear-gradient(#156cb8, #60a3c2);
    }

    .card-footer {
        text-align: center;
    }

    .zone-card-footer {
        height: 50pt;
    }


    div.bottom-space {
        margin-bottom: 15pt;
        margin-left: 5pt;
        color: #8e949f;
        text-decoration-color: #6c757d;
    }

    div.bottom-space > a:link, div.bottom-space > a:visited {
        color: #8e949f;
    }


</style>