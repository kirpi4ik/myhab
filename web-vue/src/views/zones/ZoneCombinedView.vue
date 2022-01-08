<template>
    <div class="dashboard">
        <CRow>
            <div class="bottom-space">
                <CLink v-on:click="navRoot">Acasa</CLink>
                >
            </div>
            <div v-for="link in breadcrumb" v-bind:key="link.zoneId" class="bottom-space">
                <CLink v-text="link.name" v-on:click="navZone(link.zoneId)"></CLink>
                >
            </div>
        </CRow>
        <CRow>
            <CCol md="3" sm="6" v-for="zone in childZones" v-bind:key="zone.id" class="dashboard">
                <div class="card" :class="`zone-card-background text-white`">
                    <div class="card-body">
                        <div style="display: inline">
                            <HeatScheduler :zone="zone" :name="zone.name" v-if="category === categoryHeat"/>
                            <TempDisplay :zone="zone" :name="zone.name" v-if="category === categoryTemp"/>
                        </div>
                        <div v-on:click="navZone(zone.id)" style="cursor:pointer; height: 100%">
                            <h2 class="mb-0">{{zone.name}}</h2>
                        </div>
                    </div>
                    <slot name="footer" class="card-footer">
                        <div class="zone-card-footer" v-on:click="navZone(zone.id)" style="cursor:pointer">
                            <slot></slot>
                        </div>
                    </slot>
                </div>
            </CCol>
        </CRow>
        <CRow>
            <CCol md="3" sm="6" v-for="peripheral in peripheralList" :key="peripheral.id">
                <PeriphLightControl :peripheral="peripheral" v-if="category === categoryLight"></PeriphLightControl>
                <PeriphTempControl :peripheral="peripheral" v-if="category === categoryTemp"></PeriphTempControl>
                <PeriphHeatControl :peripheral="peripheral" v-if="category === categoryHeat"></PeriphHeatControl>
            </CCol>
        </CRow>
        <CRow>
            <CCol>
                <TempChartControl v-if="category === categoryTemp"/>

            </CCol>
        </CRow>

    </div>

</template>
<script>
    import {router} from '@/_helpers';

    import {NAV_BREADCRUMB, ZONE_GET_BY_ID, ZONES_GET_ROOT} from "../../graphql/queries";
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
                currentZone: [],
                childZones: [],
                peripheralList: [],
                breadcrumb: [],
                category: this.$route.query.category,
                categoryLight: process.env.VUE_APP_CONF_PH_LIGHT,
                categoryTemp: process.env.VUE_APP_CONF_PH_TEMP,
                categoryHeat: process.env.VUE_APP_CONF_PH_HEAT
            }
        },
        created() {
            this.init();
        },
        computed: {
            stompMessage() {
                return this.$store.state.stomp.message
            }
        },
        watch: {
            '$route.path': 'init',
            stompMessage: function (newVal) {
                if (newVal.eventName == 'evt_port_value_persisted') {
                    this.updatePeripheral(newVal.jsonPayload);
                }
            }
        },
        methods: {
            updatePeripheral: function (jsonPayload) {
                let payload = JSON.parse(jsonPayload);
                this.peripheralList.forEach(function (peripheralComp, index) {
                    if (peripheralComp.data.connectedTo[0].id == payload.p2) {
                        peripheralComp['value'] = payload.p4;
                        peripheralComp['state'] = payload.p4 === 'OFF';
                    }
                }.bind(this))

            },
            init() {
                let peripheralInitCallback = function (peripheral) {
                    if (peripheral != null) {
                        if (peripheral.connectedTo && peripheral.connectedTo.length > 0) {
                            let port = peripheral.connectedTo[0];
                            if (port != null && port.device != null) {
                                peripheral['value'] = peripheral.connectedTo[0].value;
                                peripheral['state'] = peripheral.connectedTo[0].value === 'OFF';
                                peripheral['deviceState'] = peripheral.connectedTo[0].device.status;
                            } else {
                                peripheral['deviceState'] = 'OFFLINE';
                            }
                        }
                        this.peripheralList.push(
                            {
                                data: peripheral,
                                id: peripheral['id'],
                                value: peripheral['value'],
                                state: peripheral['state'],
                                deviceState: peripheral['deviceState']
                            }
                        )
                    }
                }.bind(this);
                let peripheralFilter = function (peripheral) {
                    //TODO: Add filter in query
                    //filter out peripheral which has categories different from the one requested in url
                    return peripheral.category.name === this.$route.query.category
                }.bind(this);

                let localPList
                //query for root zones , where zones has parent currentLocalZone null
                if (this.$route.params.zoneId != null && this.$route.params.zoneId !== "") {
                    this.$apollo.query({
                        query: ZONE_GET_BY_ID,
                        variables: {id: this.$route.params.zoneId},
                        fetchPolicy: 'network-only'
                    }).then(response => {
                        let data = _.cloneDeep(response.data)
                        this.currentZone = data.zoneById;
                        this.childZones = this.currentZone.zones;
                        localPList = this.currentZone.peripherals.filter(peripheralFilter)

                        this.childZones.sort((a, b) => (a.name > b.name) ? 1 : -1)
                        localPList.sort((a, b) => (a.name > b.name) ? 1 : -1)
                        localPList.forEach(peripheralInitCallback);

                    });
                } else {
                    this.$apollo.query({
                        query: ZONES_GET_ROOT,
                        variables: {},
                        fetchPolicy: 'network-only'
                    }).then(response => {
                        let data = _.cloneDeep(response.data)

                        this.currentZone = null;
                        this.childZones = data.zonesRoot.zones;
                        localPList = data.zonesRoot.peripherals.filter(peripheralFilter)

                        this.childZones.sort((a, b) => (a.name > b.name) ? 1 : -1)
                        localPList.sort((a, b) => (a.name > b.name) ? 1 : -1)
                        localPList.forEach(peripheralInitCallback);
                    });
                }
                let zoneId = null;
                if (this.$route.params.zoneId !== "") {
                    zoneId = this.$route.params.zoneId
                }
                this.$apollo.query({
                    query: NAV_BREADCRUMB,
                    variables: {zoneId: zoneId}
                }).then(response => {
                    if (response.data.navigation != null) {
                        this.breadcrumb = response.data.navigation.breadcrumb;
                    }
                });
            },
            navZone: function (zoneId) {
                router.push({path: `${zoneId}`, query: {category: this.$route.query.category}})
            },
            navRoot: function () {
                router.push({path: '', query: {category: this.$route.query.category}})
            }
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