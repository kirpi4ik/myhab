<template>
    <div class="card" :class="`card-background text-white`">
        <div class="card-body pb-0">

            <slot></slot>
            <div>
                <font-awesome-icon icon="lightbulb" size="3x" :class="`zone-icon-${peripheral.state}`"/>
                <CBadge v-if="peripheral.deviceState != 'ONLINE'">
                    <font-awesome-icon :icon="['fas', 'exclamation-triangle']" size="3x"/>
                    OFFLINE
                </CBadge>
                <div style="float: left">

                    <div style="display: inline-block">
                        <h4 class="mb-1">
                            {{peripheral.data.name}}
                        </h4>
                        <h4 class="mb-1">
                            <span style="color: #b1dae8; font-size: 10pt; color: rgba(177, 218, 232, 0.8); "
                                  v-if="peripheralTimeout != null && peripheralTimeout.value != null">[ timer: {{$moment.utc($moment.duration(peripheralTimeout.value,'s').asMilliseconds()).format("HH:mm")}}    <span
                                    style="color: #b1dae8; font-size: 10pt; color: rgba(177, 218, 232, 0.5);"
                                    v-if="peripheralTimeoutOn != 'null'"> | off at: {{$moment(new Date(Number(peripheralTimeoutOn))).format('HH:mm')}}</span> ]</span>
                        </h4>
                    </div>
                    <div style="display: inline-block; width: 100%;" v-if="hasRole(['ROLE_ADMIN'])">
                        <EventLogger :peripheral="peripheral" :name="peripheral.data.id"></EventLogger>
                        <CDropdown color="transparent p-0" placement="bottom-end" :ref="'dropdown-'+peripheral.data.id">
                            <template #toggler-content>
                                <CIcon name="cil-settings"/>
                            </template>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.on.timeout', 60)">Opreste dupa
                                1min
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.on.timeout', 300)">Opreste dupa
                                5min
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.on.timeout', 600)">Opreste dupa
                                10min
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.on.timeout', 1200)">Opreste dupa
                                20min
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.on.timeout', 3600)">Opreste dupa
                                1h
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.on.timeout', 7200)">Opreste dupa
                                2h
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.on.timeout', 10800)">Opreste dupa
                                3h
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.on.timeout', null)">Nelimitat

                            </CDropdownItem>
                            <CDropdownItem v-on:click="openPicker()" v-if="hasRGBSupport">Culoare</CDropdownItem>
                        </CDropdown>
                        <CModal :title="'Setari culoare - '+peripheral.data.name"
                                color="success"
                                :show.sync="showRGB" v-if="hasRGBSupport">
                            <div style="display: inline">
                                <div style="display: inline-block">
                                    <sketch-picker v-model="colors" :name="peripheral.data.uid" @input="updateValue" :disableAlpha="true"/>
                                </div>
                                <div style="display: inline-block; margin: 10px; vertical-align: top; color: #0b2e13" >
                                    <CInputCheckbox
                                            label="Random"
                                            :value.sync="rgbRandom"
                                            :checked.sync="rgbRandom"
                                            :inline="true"
                                            @update:checked="saveConfig(peripheral.data.id, 'key.light.rgbRandom', rgbRandom)"
                                    />
                                </div>
                            </div>
                            <template #footer>
                                <CButton @click="showRGB = false" color="danger">Inchide</CButton>
                            </template>
                        </CModal>
                    </div>
                </div>
            </div>
        </div>
        <slot name="footer" class="card-footer">
            <div class="toggle-btn">
                <toggle-button v-model="peripheral.state" :sync="true"
                               :labels="{checked: 'Aprinde', unchecked: 'Stinge'}"
                               :switch-color="{checked: 'linear-gradient( #8DFF73, green)', unchecked: 'linear-gradient(#BF0000, #FFBE62)'}"
                               :color="{checked: '#009663', unchecked: '#FF0000', disabled: '#CCCCCC'}"
                               :speed="300"
                               @change="periphStateChangeHandler(peripheral)"
                               :font-size="14"
                               :width="250"
                               :height="40" :disabled="peripheral.deviceState != 'ONLINE'"/>
            </div>
        </slot>
    </div>
</template>

<script>
    import {authenticationService} from '@/_services';
    import EventLogger from './EventLogger'
    import {Sketch} from 'vue-color'


    import {
        CONFIGURATION_GET_VALUE,
        CONFIGURATION_DELETE,
        CONFIGURATION_SET_VALUE,
        CACHE_GET_VALUE,
        PUSH_EVENT
    } from "../../graphql/queries";

    export default {
        name: 'PeriphLightControl',
        props: {
            peripheral: Object
        },
        components: {
            EventLogger,
            'sketch-picker': Sketch
        },
        created() {
            this.loadConfig();
        },
        data() {
            return {
                peripheralTimeout: null,
                peripheralTimeoutOn: null,
                hasRGBSupport: false,
                rgbRandom: false,
                showRGB: false,
                colors: {
                    hex: '#194d33',
                    hex8: '#194D33A8',
                    hsl: {h: 150, s: 0.5, l: 0.2, a: 1},
                    hsv: {h: 150, s: 0.66, v: 0.30, a: 1},
                    rgba: {r: 25, g: 77, b: 51, a: 1},
                    a: 1
                }
            }
        },
        methods: {
            loadConfig: function () {
                this.peripheral.show = false;
                this.$apollo.query({
                    query: CONFIGURATION_GET_VALUE,
                    variables: {entityId: this.peripheral.data.id, entityType: 'PERIPHERAL', key: 'key.on.timeout'},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.peripheralTimeout = response.data.configPropertyByKey
                });
                this.$apollo.query({
                    query: CONFIGURATION_GET_VALUE,
                    variables: {entityId: this.peripheral.data.id, entityType: 'PERIPHERAL', key: 'key.light.hasRGBSupport'},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    if (response.data.configPropertyByKey != null) {
                        this.hasRGBSupport = (response.data.configPropertyByKey.value == 'true')
                        this.$apollo.query({
                            query: CONFIGURATION_GET_VALUE,
                            variables: {entityId: this.peripheral.data.id, entityType: 'PERIPHERAL', key: 'key.light.rgbRandom'},
                            fetchPolicy: 'network-only'
                        }).then(response => {
                            if (response.data.configPropertyByKey != null) {
                                this.rgbRandom = (response.data.configPropertyByKey.value == 'true')
                            }
                        });
                    }
                });
                this.$apollo.query({
                    query: CACHE_GET_VALUE,
                    variables: {cacheName: 'expiring', cacheKey: this.peripheral.portId},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.peripheralTimeoutOn = response.data.cache.cachedValue
                });
            },
            periphStateChangeHandler: function (peripheral) {
                let event = {
                    "p0": "evt_light",
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
            },
            saveConfig: function (peripheralId, key, value) {
                let dropdown = this.$refs['dropdown-' + peripheralId];
                if (value != null) {
                    this.$apollo.mutate({
                        mutation: CONFIGURATION_SET_VALUE,
                        variables: {key: key, value: value, entityId: peripheralId, entityType: 'PERIPHERAL'}
                    }).then(response => {
                        dropdown.hide();
                        this.loadConfig();
                    });
                } else {
                    this.$apollo.mutate({
                        mutation: CONFIGURATION_DELETE,
                        variables: {id: this.peripheralTimeout.id}
                    }).then(response => {
                        dropdown.hide();
                        this.peripheralTimeout = null
                    });
                }
                return true;
            },
            hasRole: function (roles) {
                const currentUser = authenticationService.currentUserValue;
                return currentUser.permissions.filter(function (userRole) {
                    return roles.includes(userRole);
                }).length > 0;
            },
            openPicker: function () {
                this.showRGB = true;
            },
            updateValue: function () {
                let color = {
                    rgb: this.colors.rgba,
                    hex: this.colors.hex
                }
                let event = {
                    "p0": "evt_light_set_color",
                    "p1": "PERIPHERAL",
                    "p2": this.peripheral.data.uid,
                    "p3": "mweb",
                    "p4": JSON.stringify(color),
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
