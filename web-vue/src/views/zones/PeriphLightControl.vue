<template>
    <div class="card" :class="`card-background text-white`">
        <div class="card-body pb-0">

            <slot></slot>
            <div>
                <font-awesome-icon icon="lightbulb" size="3x" :class="`zone-icon-${peripheral.state}`"/>

                <div style="float: left">
                    <div style="display: inline-block">
                        <h4 class="mb-1">
                            {{peripheral.data.name}} <span style="color: #b1dae8; font-size: 10pt"
                                                           v-if="peripheralTimeout != null && peripheralTimeout.value != null">[ {{peripheralTimeout.value/60}}min ]</span>
                        </h4>
                    </div>
                    <div style="display: inline-block; width: 100%;">
                        <CDropdown color="transparent p-0" placement="bottom-end" :ref="'dropdown-'+peripheral.data.id">
                            <template #toggler-content>
                                <CIcon name="cil-settings"/>
                            </template>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.on.timeout', 600)">Porneste
                                10min
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.on.timeout', 1200)">Porneste
                                20min
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.on.timeout', 3600)">Porneste
                                1h
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.on.timeout', 10800)">Porneste
                                3h
                            </CDropdownItem>
                            <CDropdownItem v-on:click="saveConfig(peripheral.data.id, 'key.on.timeout', null)">Nelimitat

                            </CDropdownItem>
                        </CDropdown>
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
                               :height="40"/>
            </div>
        </slot>
    </div>
</template>

<script>
    import {authenticationService} from '@/_services';
    import {
        CONFIGURATION_GET_VALUE,
        CONFIGURATION_DELETE,
        CONFIGURATION_SET_VALUE,
        PUSH_EVENT
    } from "../../graphql/zones";

    export default {
        name: 'PeriphLightControl',
        props: {
            peripheral: Object
        },
        created() {
            this.loadConfig();
        },
        data() {
            return {
                peripheralTimeout: null
            }
        },
        methods: {
            loadConfig: function () {
                debugger
                this.$apollo.query({
                    query: CONFIGURATION_GET_VALUE,
                    variables: {entityId: this.peripheral.data.id, entityType: 'PERIPHERAL', key: 'key.on.timeout'},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.peripheralTimeout = response.data.configPropertyByKey
                });
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
