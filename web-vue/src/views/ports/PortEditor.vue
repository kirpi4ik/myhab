<template>
    <div>
        <CRow>
            <CCol sm="6">
                <CCard>
                    <CForm>
                        <CCardHeader>
                            <strong>Port </strong> <small>{{port.name}}</small>
                            <div class="card-header-actions">
                                <a @click="navConfig" style="cursor: pointer" class="card-header-action" rel="noreferrer noopener" v-if="port.id != null">
                                    <small class="text-muted">{{ $t("actions.config") }}</small> |
                                </a>
                                <a @click="navDelete" style="cursor: pointer" class="card-header-action" rel="noreferrer noopener" v-if="port.id != null">
                                    <small class="text-muted">{{ $t("actions.delete") }}</small> |
                                </a>
                                <a style="cursor: pointer" class="card-header-action" rel="noreferrer noopener"
                                   @click="$router.push({path: '/devices/' + $route.params.deviceId + '/view'})">
                                    <small class="text-muted">{{ $t("actions.cancel") }}</small>
                                </a>
                            </div>
                        </CCardHeader>
                        <CCardBody>
                            <CRow v-for="(portDetail, index) in portDetails" :key="`detail-${index}`">
                                <CCol sm="12">
                                    <CInput v-if="!isBoolean(portDetail.key)" :label="portDetail.key.charAt(0).toUpperCase()+portDetail.key.slice(1)"
                                            :value="portDetail.value"
                                            @input="updateFieldValue($event, portDetail.key)"
                                            :ref="portDetail.key"/>
                                    <CInputCheckbox v-if="isBoolean(portDetail.key)"
                                                    :key="portDetail.key"
                                                    :label="portDetail.key.charAt(0).toUpperCase()+portDetail.key.slice(1)"
                                                    :value="portDetail.value"
                                                    :checked="portDetail.value"
                                                    @update:checked="check($event, portDetail.key)"
                                                    :inline="true"
                                                    :ref="portDetail.key"
                                    />
                                </CCol>
                            </CRow>
                            <CRow>
                                {{ $t("port.fields.type") }}
                                <multiselect v-model="portToUpdate['type']" :options="portTypes">
                                </multiselect>
                            </CRow>
                            <CRow>
                                {{ $t("port.fields.state") }}
                                <multiselect v-model="portToUpdate['state']" :options="portStates">
                                </multiselect>
                            </CRow>
                        </CCardBody>
                        <CCardFooter>
                            <CButton type="submit" size="sm" color="primary" @click="save">
                                <CIcon name="cil-check"/>
                                {{ $t("actions.save") }}
                            </CButton>
                        </CCardFooter>
                    </CForm>
                </CCard>
            </CCol>
        </CRow>
    </div>
</template>


<script>
    import {PORT_GET_BY_ID, PORT_UPDATE, PORT_CREATE, DEVICE_GET_BY_ID_MINIMAL} from "../../graphql/queries";

    import Multiselect from 'vue-multiselect'

    export default {
        name: 'PortEditor',
        components: {
            'multiselect': Multiselect
        },
        data: () => {
            return {
                portDetails: [],
                portToUpdate: {},
                port: [],
                readonly: ["id", "__typename", "uid", "device", "type", "state"],
                booleans: ["runAction", "mustSendToServer", "runScenario"],
                deleteConfirmShow: false,
                portTypes: [
                    "UNKNOW",
                    "IN",
                    "OUT",
                    "ADC",
                    "DSEN",
                    "I2C",
                    "NOT_CONFIGURED"
                ],
                portStates: [
                    "UNKNOW",
                    "CONFIGURED",
                    "ACTIVE",
                    "INACTIVE"
                ]
            }
        },
        created() {
            if (this.$route.meta.uiMode === 'CREATE') {
                this.initCreate();

            } else {
                this.init();
            }
        },
        methods: {
            initCreate() {
                this.port = {
                    internalRef: "",
                    name: "",
                    description: "",
                    mode: "",
                    model: "",
                    device: {
                        id: this.$route.params.deviceId
                    }
                };
                let removeReadonly = function (keyMap) {
                    return !this.readonly.includes(keyMap.key)
                }.bind(this);
                this.portDetails = Object.entries(this.port).map(([key, value]) => {
                    return {key, value}
                }).filter(removeReadonly);
                this.portToUpdate['type'] = this.portTypes[0];
                this.portToUpdate['state'] = this.portStates[0];
            },
            init() {
                let port = {};
                this.$apollo.query({
                    query: PORT_GET_BY_ID,
                    variables: {id: this.$route.params.id},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    let cleanup = function (item, index) {
                        delete item["__typename"]
                    };
                    let removeReadonly = function (keyMap) {
                        return !this.readonly.includes(keyMap.key)
                    }.bind(this);

                    this.port = response.data.devicePort;
                    const portDetailsToMap = port ? Object.entries(this.port) : [['id', 'Not found']];
                    this.portDetails = portDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly);

                    this.portToUpdate['type'] = this.port.type;
                    this.portToUpdate['state'] = this.port.state;
                });
            },
            isBoolean(key) {
                return this.booleans.indexOf(key) != -1;
            },
            check(value, key) {
                this.portToUpdate[key] = value

            },
            updateFieldValue(value, key) {
                this.portToUpdate[key] = value
            },
            save() {

                if (this.$route.meta.uiMode === 'EDIT') {
                    this.$apollo.mutate({
                        mutation: PORT_UPDATE, variables: {id: this.port.id, devicePort: this.portToUpdate}
                    }).then(response => {
                        this.$router.push({path: "/devices/" + this.$route.params.deviceId + "/ports/" + response.data.devicePortUpdate.id + "/view"})
                    });
                } else if (this.$route.meta.uiMode === 'CREATE') {
                    this.portToUpdate["device"] = this.port.device;
                    this.$apollo.mutate({
                        mutation: PORT_CREATE, variables: {devicePort: this.portToUpdate}
                    }).then(response => {
                        this.$router.push({path: "/devices/" + this.$route.params.deviceId + "/ports/" + response.data.devicePortCreate.id + "/edit"})
                    });
                }
            },
            navEdit(item, index) {
                this.$router.push({path: "/devices/" + this.$route.params.deviceId + "/ports/" + this.port.id + "/edit"})
            },
            navConfig(item, index) {
                this.$router.push({path: "/devices/" + this.$route.params.deviceId + "/ports/" + this.port.id + "/configurations"})
            },
            navDelete(item, index) {
                this.deleteConfirmShow = true;
            },
        }
    }
</script>
<style src="vue-multiselect/dist/vue-multiselect.min.css"></style>
