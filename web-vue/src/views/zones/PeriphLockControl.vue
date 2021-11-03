<template>
    <div class="card" :class="`card-background-lock text-white`">
        <slot name="body" class="card-header">
            <div class="card-body pb-3">
                <div>
                    <CBadge v-if="peripheral.deviceState != 'ONLINE'">
                        <font-awesome-icon :icon="['fas', 'exclamation-triangle']" size="3x"/>
                        OFFLINE
                    </CBadge>
                </div>
                <div style="display: inline">
                    <div v-if="hasRole(['ROLE_ADMIN'])" style="display: inline-block">
                        <EventLogger :peripheral="{'data':peripheral}" :name="peripheral.uid"></EventLogger>
                    </div>
                    <div style="display: inline-block; margin-left: 1em">
                        <h4>
                            {{peripheral.name}}
                        </h4>
                    </div>
                    <div style="display: inline-block; float: right; fill-opacity: 0.3">
                        <font-awesome-icon :icon="['fas', `door-open`]" size="2x"/>
                    </div>
                </div>
            </div>

        </slot>
        <slot name="footer" class="card-footer">
            <CButton color="success" @click="openPasswordInput()">
                <img src="../../assets/images/skin/lock-light-mid.png"/><span style="font-size:1.5em; vertical-align: bottom; margin-left: 9px">Deschide</span>
            </CButton>
        </slot>
        <CModal title="Introduceti codul de acces" color="warning" :show.sync="showPassword">
            <CForm validated novalidate>
                <CInput type="number" description="Please enter your password." placeholder="Cod deblocare" :value="unlockCode" @input="unlockCode =  $event" was-validated>
                    <template #append-content>
                        <CIcon name="cil-lock-unlocked"/>
                    </template>
                </CInput>
            </CForm>
            <template #footer>
                <CButton @click="periphStateChangeHandler()" color="danger" type="submit">Deschide</CButton>
            </template>
        </CModal>
        <CModal
                title="Deblocarea a esuat"
                color="danger"
                :show.sync="showErrorModal"
        >
        </CModal>
    </div>

</template>

<script>
    import {authenticationService} from '@/_services';
    import EventLogger from './EventLogger'
    import {PERIPHERAL_GET_BY_ID, PUSH_EVENT} from "../../graphql/queries";
    import _ from "lodash";

    export default {
        name: 'PeriphLockControl',
        components: {
            EventLogger
        },
        props: {
            peripheralId: Number
        },
        data() {
            return {
                showPassword: false,
                showErrorModal: false,
                unlockCode: null,
                peripheral: {state: false},

            }
        },
        created() {
            this.loadInitial();
        },
        methods: {
            loadInitial: function () {
                this.$apollo.query({
                    query: PERIPHERAL_GET_BY_ID,
                    variables: {id: this.peripheralId},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    let data = _.cloneDeep(response.data)
                    this.peripheral = data.devicePeripheral;
                    this.peripheral.deviceState = this.peripheral.connectedTo[0].device.status

                });
            },
            periphStateChangeHandler: function () {
                let event = {
                    "p0": "evt_intercom_door_lock",
                    "p1": "PERIPHERAL",
                    "p2": this.peripheral.id,
                    "p3": "mweb",
                    "p4": "open",
                    "p5": '{"unlockCode": "' + this.unlockCode + '"}',
                    "p6": authenticationService.currentUserValue.login
                };
                this.$apollo.mutate({
                    mutation: PUSH_EVENT, variables: {input: event}
                }).then(response => {
                    this.showPassword = false
                });
            },
            hasRole: function (roles) {
                const currentUser = authenticationService.currentUserValue;
                return currentUser.permissions.filter(function (userRole) {
                    return roles.includes(userRole);
                }).length > 0;
            },
            openPasswordInput: function () {
                this.unlockCode = null;
                this.showPassword = true;
            }
        }
    }
</script>
<style scoped>

    .card-background-lock {
        background-image: linear-gradient(#447cff, #68b8f0);
    }

    .card-footer {
        text-align: center;
    }

    div.v-switch-core {
        border: 1px solid #000000 !important;
    }
</style>
