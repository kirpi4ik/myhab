<template>
    <div class="card" :class="`card-background-heat-${peripheral.state} text-white`">
        <div class="card-body pb-2">
            <slot></slot>
            <div class="card-body pb-0">
                <slot></slot>
                <div style="display: inline; width: 100%;">
                    <div>
                        <CBadge v-if="peripheral.deviceState != 'ONLINE'">
                            <font-awesome-icon :icon="['fas', 'exclamation-triangle']" size="3x"/>
                            OFFLINE
                        </CBadge>
                    </div>
                    <div v-if="hasRole(['ROLE_ADMIN'])" style="margin-top: -20px;">
                        <EventLogger :peripheral="peripheral" :name="peripheral.data.id"></EventLogger>
                        <CDropdown color="transparent p-0" placement="bottom-end" :ref="'dropdown-'+peripheral.id">
                            <template #toggler-content>
                                <CIcon name="cil-settings"/>
                            </template>
                            <CDropdownItem v-on:click="$router.push({path: '/peripherals/' + peripheral.id + '/view'})">Details</CDropdownItem>
                        </CDropdown>
                    </div>
                    <div style="display: inline-block; float: right; margin-top: -20px;">
                        <font-awesome-icon icon="fire-alt" size="3x" :class="`zone-icon-${peripheral.state}`"/>
                    </div>
                </div>
                <div>
                    <h4 class="mb-1">
                        {{peripheral.data.name}} <span style="color: #b1dae8; font-size: 10pt"
                                                  v-if="peripheralTimeout != null && peripheralTimeout.value != null">[ {{peripheralTimeout.value}}&#8451; ]</span>
                    </h4>
                </div>
            </div>
        </div>
        <slot name="footer" class="card-footer">
            <div class="toggle-btn" style="cursor: pointer">
                <toggle-button v-model="peripheral.state" sync
                               :labels="{checked: 'Porneste', unchecked: 'Opreste'}"
                               :switch-color="{checked: 'linear-gradient( #8DFF73, green)', unchecked: 'linear-gradient(#BF0000, #FFBE62)'}"
                               :color="{checked: '#809687', unchecked: '#b90000', disabled: '#CCCCCC'}"
                               :speed="300"
                               @change="periphStateChangeHandler()"
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
    import {
        CONFIGURATION_DELETE,
        CONFIGURATION_SET_VALUE,
        PUSH_EVENT
    } from "../../graphql/queries";

    export default {
        name: 'PeriphHeatControl',
        components: {
            EventLogger
        },
        props: {
            peripheral: Object
        },
        data() {
            return {
                peripheralTimeout: null,

            }
        },
        created() {
            this.peripheralTimeout = this.getConfig('key.on.timeout')
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

            periphStateChangeHandler: function () {
                let event = {
                    "p0": "evt_heat",
                    "p1": "PERIPHERAL",
                    "p2": this.peripheral.data.id,
                    "p3": "mweb",
                    "p4": this.peripheral.state === true ? "off" : "on",
                    "p6": authenticationService.currentUserValue.login
                };
                this.$apollo.mutate({
                    mutation: PUSH_EVENT, variables: {input: event}
                }).then(response => {

                });
            },
            hasRole: function (roles) {
                const currentUser = authenticationService.currentUserValue;
                return currentUser.permissions.filter(function (userRole) {
                    return roles.includes(userRole);
                }).length > 0;
            },
            addNewSchedule() {
                saveConfig(peripheral.data.id, 'key.temp.allDay.value', 24)
            },
        }
    }
</script>
<style scoped>
    .v-switch-core:hover {
        background-color: #8a3333 !important;
    }

    .zone-card-background {
        background-image: linear-gradient(#4f6167, #8e949f);
    }
    .card-background-heat-undefined {
        background-image: linear-gradient(#b6abac, #876c6b);
    }
    .card-background-heat-false {
        background-image: linear-gradient(#b8303c, #c25751);
    }

    .card-background-heat-true {
        background: #0f3854;
        background: radial-gradient(ellipse at center,  #0a2e38  0%, #262626 70%);
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
        color: red;
    }

    .zone-icon-true {
        float: right;
        fill: #044B9466;
        fill-opacity: 0.5;
    }
</style>
