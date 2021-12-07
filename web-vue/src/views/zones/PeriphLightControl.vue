<template>
    <div class="card" :class="`card-background text-white`">
        <div class="card-body pb-0">

            <slot></slot>
            <div>
                <font-awesome-icon icon="lightbulb" size="3x" :class="`zone-icon-${peripheral.value === 'OFF'}`"/>
                <CBadge v-if="peripheral.deviceState !== 'ONLINE'">
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
                        <EventLogger :peripheral="peripheral" :name="peripheral.id"></EventLogger>
                        <CDropdown color="transparent p-0" placement="bottom-end" :ref="'dropdown-'+peripheral.id">
                            <template #toggler-content>
                                <CIcon name="cil-settings"/>
                            </template>
                            <CDropdownItem v-on:click="saveConfig(peripheral.id, 'key.on.timeout', 60)">Opreste dupa
                                1min
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.id, 'key.on.timeout', 300)">Opreste dupa
                                5min
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.id, 'key.on.timeout', 600)">Opreste dupa
                                10min
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.id, 'key.on.timeout', 1200)">Opreste dupa
                                20min
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.id, 'key.on.timeout', 3600)">Opreste dupa
                                1h
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.id, 'key.on.timeout', 7200)">Opreste dupa
                                2h
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.id, 'key.on.timeout', 10800)">Opreste dupa
                                3h
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.id, 'key.on.timeout', null)">Nelimitat

                            </CDropdownItem>
                            <CDropdownItem v-on:click="openPicker()" v-if="hasRGBSupport">Culoare</CDropdownItem>
                            <CDropdownItem v-on:click="$router.push({path: '/peripherals/' + peripheral.id + '/view'})">Details</CDropdownItem>
                        </CDropdown>
                        <CModal :title="'Setari culoare - '+peripheral.name"
                                color="success"
                                :show.sync="showRGB" v-if="hasRGBSupport">
                            <div style="display: inline">
                                <div style="display: inline-block">
                                    <sketch-picker v-model="colors" :name="peripheral.uid" @input="updateValue" :disableAlpha="true"/>
                                </div>
                                <div style="display: inline-block; margin: 10px; vertical-align: top; color: #0b2e13">
                                    <CInputCheckbox
                                            label="Random"
                                            :value.sync="rgbRandom"
                                            :checked.sync="rgbRandom"
                                            :inline="true"
                                            @update:checked="saveConfig(peripheral.id, 'key.light.rgbRandom', rgbRandom)"
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
                <toggle-button v-model="peripheral.state"  :sync="true"
                               :labels="{checked: 'Aprinde', unchecked: 'Stinge'}"
                               @change="toggle()"
                               :disabled="peripheral.deviceState != 'ONLINE'"
                               :switch-color="{checked: 'linear-gradient( #8DFF73, green)', unchecked: 'linear-gradient(#BF0000, #FFBE62)'}"
                               :color="{checked: '#009663', unchecked: '#FF0000', disabled: '#CCCCCC'}"
                               :speed="300"
                               :width="220"
                               :height="40"
                               :font-size="14"/>
            </div>
        </slot>
    </div>
</template>

<script>
    import {authenticationService} from '@/_services';
    import EventLogger from './EventLogger'
    import {Sketch} from 'vue-color'
    import {lightService} from '@/_services/controls';



    import {CACHE_GET_VALUE, CONFIGURATION_DELETE, CONFIGURATION_SET_VALUE, PUSH_EVENT} from "../../graphql/queries";

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
            this.initConfig();
        },
        data() {
            return {
                state: true,
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
            getConfig: function (key) {
                let conf = this.peripheral.data.configurations.filter(function (config) {
                    return config.key === key
                })
                if (conf.length > 0) {
                    return conf[0]
                } else {
                    return null
                }
            },
            initConfig: function () {
                this.peripheralTimeout = this.getConfig('key.on.timeout')
                this.hasRGBSupport = this.getConfig('key.light.hasRGBSupport')
                if (this.hasRGBSupport && this.hasRGBSupport.value == 'true') {
                    let rgbRandom = this.getConfig('key.light.rgbRandom')
                    if (rgbRandom != null) {
                        this.rgbRandom = rgbRandom.value == 'true'
                    }
                }
                this.$apollo.query({
                    query: CACHE_GET_VALUE,
                    variables: {cacheName: 'expiring', cacheKey: this.peripheral.data.connectedTo[0].id},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.peripheralTimeoutOn = response.data.cache.cachedValue
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
            toggle(){
                lightService.toggle(this.peripheral)
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
                    "p2": this.peripheral.data.id,
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
