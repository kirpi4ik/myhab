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
                        <CDropdown color="transparent p-0" placement="bottom-end"
                                   :ref="'dropdown-'+peripheral.data.id" :disabled="peripheral.deviceState != 'ONLINE'">
                            <template #toggler-content>
                                <CIcon name="cil-settings"/>
                            </template>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.temp.allDay.value', 22)">
                                22 &#8451;
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.temp.allDay.value', 23)">
                                23 &#8451;
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.temp.allDay.value', 24)">
                                24 &#8451;
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.temp.allDay.value', 25)">
                                25 &#8451;
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.temp.allDay.value', 26)">
                                26 &#8451;
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.temp.allDay.value', null)">
                                Specifica temp.
                            </CDropdownItem>
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
            <div class="toggle-btn">
                <toggle-button v-model="peripheral.state" :sync="true"
                               :labels="{checked: 'Porneste', unchecked: 'Opreste'}"
                               :switch-color="{checked: 'linear-gradient( #8DFF73, green)', unchecked: 'linear-gradient(#BF0000, #FFBE62)'}"
                               :color="{checked: '#809687', unchecked: '#b90000', disabled: '#CCCCCC'}"
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
    import {
        CONFIGURATION_DELETE,
        CONFIGURATION_GET_VALUE,
        CONFIGURATION_SET_VALUE,
        PUSH_EVENT
    } from "../../graphql/zones";

    export default {
        name: 'PeriphHeatControl',
        props: {
            peripheral: Object
        },
        data() {
            return {
                peripheralTimeout: null
            }
        },
        created() {
            this.loadConfig();
        },
        methods: {
            loadConfig: function () {
                this.$apollo.query({
                    query: CONFIGURATION_GET_VALUE,
                    variables: {
                        entityId: this.peripheral.data.id,
                        entityType: 'PERIPHERAL',
                        key: 'key.temp.allDay.value'
                    },
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.peripheralTimeout = response.data.configPropertyByKey
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
            periphStateChangeHandler: function (peripheral) {
                let event = {
                    "p0": "evt_heat",
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
            hasRole: function (roles) {
                const currentUser = authenticationService.currentUserValue;
                return currentUser.permissions.filter(function (userRole) {
                    return roles.includes(userRole);
                }).length > 0;
            }
        }
    }
</script>
<style scoped>
    .zone-card-background {
        background-image: linear-gradient(#4f6167, #8e949f);
    }

    .card-background-heat-false {
        background-image: linear-gradient(#b8303c, #c25751);
    }

    .card-background-heat-true {
        background-image: linear-gradient(#b88586, #c27973);
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
