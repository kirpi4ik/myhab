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
                <div class="card" :class="`zone-card-background text-white`"
                     v-on:click="navZone(zone.uid)">
                    <div class="card-body">
                        <div>
                            <h2 class="mb-0">{{zone.name}}</h2>
                        </div>
                    </div>
                    <slot name="footer" class="card-footer">
                        <div class="zone-card-footer">
                            <slot></slot>
                        </div>
                    </slot>
                </div>
            </CCol>
        </CRow>
        <CRow>
            <CCol md="3" sm="6" v-for="peripheral in peripherals" v-bind:key="peripheral.uid">
                <div class="card" :class="`card-background text-white`">
                    <div class="card-body pb-2">
                        <slot></slot>
                        <div>
                            <font-awesome-icon icon="lightbulb" size="3x" :class="`zone-icon-${peripheral.state}`"/>
                            <h4 class="mb-1"> {{peripheral.data.name}}</h4>
                        </div>

                    </div>
                    <slot name="footer" class="card-footer">
                        <div class="toggle-btn">
                            <toggle-button v-model="peripheral.state" color="#82C7EB" :sync="true"
                                           :labels="{checked: 'Aprinde', unchecked: 'Stinge'}"
                                           :switch-color="{checked: 'linear-gradient( #8DFF73, green)', unchecked: 'linear-gradient(#BF0000, #FFBE62)'}"
                                           :color="{checked: '#009663', unchecked: '#FF0000', disabled: '#CCCCCC'}"
                                           :speed="300"
                                           @change="periphStateChangeHandler(peripheral)"
                                           :font-size="14"
                                           :width="250"
                                           :height="40"/>
                        </div>
                    </slot>
                </div>
            </CCol>
        </CRow>
    </div>
</template>
<script>
    import {router} from '@/_helpers';
    import {authenticationService} from '@/_services';
    import {GET_ZONE_BY_UID, GET_ZONES_ROOT, NAV_BREADCRUMB, PUSH_EVENT} from "../../graphql/zones";

    export default {
        name: 'ZoneCombinedView',
        data() {
            return {
                zone: [],
                zones: [],
                peripherals: [],
                breadcrumb: []
            }
        },
        created() {
            this.loadInitial();
        },
        watch: {
            '$route.path': 'loadInitial'
        },
        methods: {
            loadInitial() {
                let peripherals = [];
                let peripheralUids = [];
                let zones = [];
                let zone = [];
                let categoryUid = this.$route.query.categoryUid;
                let periphInitCallback = function (peripheral) {
                    let state = false;
                    let portId = null;
                    let portUid = null;
                    if (peripheral.connectedTo && peripheral.connectedTo.length > 0) {
                        let port = peripheral.connectedTo[0];
                        state = port.value === 'OFF'
                        portId = port.id;
                        portUid = port.uid;
                    }

                    if (peripheralUids.indexOf(peripheral.uid) === -1) {
                        peripherals.push({data: peripheral, state: state, portId: portId, portUid: portUid});
                        peripheralUids.push(peripheral.uid)
                    }
                };
                let zoneInitCallback = function (childZone) {
                    childZone.peripherals.sort((a, b) => (a.name > b.name) ? 1 : -1).forEach(periphInitCallback);
                };
                let peripheralFilter = function (peripheral) {
                    return peripheral.data.category.uid === categoryUid
                };
                if (this.$route.query.zoneUid !== "") {
                    this.$apollo.query({
                        query: GET_ZONE_BY_UID,
                        variables: {uid: this.$route.query.zoneUid}
                    }).then(response => {
                        zone = response.data.zoneByUid;
                        zones = zone.zones;
                        zone.peripherals.sort((a, b) => (a.name > b.name) ? 1 : -1).forEach(periphInitCallback);
                        zones.forEach(zoneInitCallback);

                        this.zone = zone;
                        this.zones = zones;
                        this.peripherals = peripherals.filter(peripheralFilter)
                    });
                } else {
                    this.$apollo.query({
                        query: GET_ZONES_ROOT,
                        variables: {}
                    }).then(response => {
                        zones = response.data.zonesRoot;
                        zones.forEach(zoneInitCallback);

                        this.zone = zone;
                        this.zones = zones;
                        this.peripherals = peripherals.filter(peripheralFilter);
                    });
                }
                let zoneUid = null
                if (this.$route.query.zoneUid !== "") {
                    zoneUid = this.$route.query.zoneUid
                }
                this.$apollo.query({
                    query: NAV_BREADCRUMB,
                    variables: {zoneUid: zoneUid}
                }).then(response => {
                    this.breadcrumb = response.data.navigation.breadcrumb;
                });
            },
            navZone: function (zoneUid) {
                router.push({path: 'zones', query: {zoneUid: zoneUid, categoryUid: this.$route.query.categoryUid}})
            },
            navRoot: function () {
                router.push({path: 'zones', query: {zoneUid: "", categoryUid: this.$route.query.categoryUid}})
            },
            periphStateChangeHandler: function (peripheral) {
                let event = {
                    "p0": "light",
                    "p1": "PERIPHERAL",
                    "p2": peripheral.data.uid,
                    "p3": "mweb",
                    "p4": peripheral.state === true ? "off" : "on",
                    "p6": authenticationService.currentUserValue.login
                };
                this.$apollo.mutate({
                    mutation: PUSH_EVENT, variables: {input: event}
                }).then(response => {

                });
            }
        }
    }
</script>
<style scoped>
    .zone-card-background {
        background-image: linear-gradient(#4f6167, #8e949f);
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

    .toggle-btn {
        margin: auto;
    }

    div.v-switch-core {
        border: 1px solid #000000 !important;
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

    .zone-icon-false {
        float: right;
        color: yellow;
    }

    .zone-icon-true {
        float: right;
        fill: #044B9466;
        fill-opacity: 0.5;
    }
</style>