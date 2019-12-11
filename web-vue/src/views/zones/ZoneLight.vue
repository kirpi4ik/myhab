<template>
    <div>
        <CRow>
            <CCol md="3" sm="6" v-for="zone in zones" v-bind:key="zone.uid">
                <div class="card" :class="`zone-card-background text-white`"
                     v-on:click="navZone(zone.uid)">
                    <div class="card-body pb-2">
                        <slot></slot>
                        <h4 class="mb-1"> {{zone.name}}</h4>

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
                        <h4 class="mb-1"> {{peripheral.data.name}}</h4>

                    </div>
                    <slot name="footer" class="card-footer">
                        <div class="toggle-btn">
                            <toggle-button v-model="peripheral.state" color="#82C7EB" :sync="true"
                                           :labels="{checked: 'Aprinde', unchecked: 'Stinge'}"
                                           :switch-color="{checked: 'linear-gradient( #8DFF73, green)', unchecked: 'linear-gradient(#BF0000, #FFBE62)'}"
                                           :color="{checked: '#009663', unchecked: '#FF0000', disabled: '#CCCCCC'}"
                                           :speed="300"
                                           @change="periphStateChangeHandler({value:peripheral.state, id:peripheral.data.id, uid:peripheral.data.uid})"
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
    import {GET_ZONE_BY_UID, GET_ZONES_ROOT, UPDATE_PORT_VALUE} from "../../graphql/zones";

    export default {
        name: 'ZoneLight',
        data() {
            return {
                zone: [],
                zones: [],
                peripherals: []
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
                let peripherals = []
                let peripheralUids = []
                let zones = []
                let zone = []
                let categoryUid = this.$route.query.categoryUid
                let periphInitCallback = function (peripheral) {
                    let state = false;
                    if (peripheral.connectedTo && peripheral.connectedTo.length > 0) {
                        state = peripheral.connectedTo[0].value === 'ON'
                    }

                    if (peripheralUids.indexOf(peripheral.uid) === -1) {
                        peripherals.push({data: peripheral, state: state});
                        peripheralUids.push(peripheral.uid)
                    }
                };
                let zoneInitCallback = function (childZone) {
                    childZone.peripherals.forEach(periphInitCallback);
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
                        zone.peripherals.forEach(periphInitCallback);
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
            },
            navZone: function (zoneUid) {
                router.push({path: 'light', query: {zoneUid: zoneUid, categoryUid: this.$route.query.categoryUid}})
            },
            periphStateChangeHandler: function (value, srcEvent) {
                this.$apollo.mutate({
                    mutation: UPDATE_PORT_VALUE, variables: {id: 6816, portValue: {value: "OFF"}}
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
</style>