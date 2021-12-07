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
                                   @click="$router.push({path: '/devices/' + $route.params.idPrimary + '/view'})">
                                    <small class="text-muted">{{ $t("actions.cancel") }}</small>
                                </a>
                            </div>
                        </CCardHeader>
                        <CCardBody>
                            <CRow>
                                <CCol sm="12">
                                    <CInput label="Internal Ref" :value="port['internalRef']" @input="updateFieldValue($event, 'internalRef')" ref="internalRef" sync/>
                                </CCol>
                            </CRow>
                            <CRow>
                                <CCol sm="12">
                                    <CInput label="Name" :value="port['name']" @input="updateFieldValue($event, 'name')" ref="name" sync/>
                                </CCol>
                            </CRow>
                            <CRow>
                                <CCol sm="12">
                                    <CInput label="Description" :value="port['description']" @input="updateFieldValue($event, 'description')" ref="description" sync/>
                                </CCol>
                            </CRow>
                            <CRow>
                                <CCol sm="12">
                                    <CInput label="Value" :value="port['value']" @input="updateFieldValue($event, 'value')" ref="value" sync/>
                                </CCol>
                            </CRow>
                            <CRow class="form-group">
                                <CCol>
                                    <label>Type</label>
                                    <v-select label="name" v-model="port.type" :options="portTypes"/>
                                </CCol>
                            </CRow>
                            <CRow class="form-group">
                                <CCol>
                                    <label>State</label>
                                    <v-select label="name" v-model="port.state" :options="portStates"/>
                                </CCol>
                            </CRow>
                            <CRow class="form-group">
                                <CCol>
                                    <label>Cables</label>
                                    <v-select label="name" v-model="port.cables" :options="cables" multiple/>
                                </CCol>
                            </CRow>
                            <CRow class="form-group">
                                <CCol>
                                    <label>Peripherals</label>
                                    <v-select label="name" v-model="port.peripherals" :options="peripherals" multiple/>
                                </CCol>
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
    import {PORT_CREATE, PORT_DETAILS_TO_CREATE, PORT_GET_BY_ID, PORT_UPDATE} from "../../graphql/queries";
    import vSelect from "vue-select";
    import _ from "lodash";

    export default {
        name: 'PortEditor',
        components: {
            'v-select': vSelect
        },
        data: () => {
            return {
                portDetails: [],
                port: [],
                portTypes: [],
                portStates: [],
                peripherals: [],
                cables: [],
                deleteConfirmShow: false
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
            updateFieldValue(newVal, field) {
                this.port[field] = newVal
            },
            initCreate() {
                this.port = {
                    internalRef: "",
                    name: "",
                    description: "",
                    mode: "",
                    model: "",
                    device: {
                        id: this.$route.params.idPrimary
                    }
                };

                this.$apollo.query({
                    query: PORT_DETAILS_TO_CREATE,
                    variables: {id: this.$route.params.idPrimary},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.peripherals = _.transform(_.cloneDeep(response.data.devicePeripheralList), function (result, obj) {
                        obj.name = obj.name + ' - ' + obj.description
                        result.push(obj);
                    }, []);
                    this.cables = _.transform(_.cloneDeep(response.data.cableList), function (result, obj) {
                        obj.name = obj.code
                        result.push(obj);
                    }, []);
                    this.portTypes = response.data.portTypes
                    this.portStates = response.data.portStates

                });
            },
            init() {
                this.$apollo.query({
                    query: PORT_GET_BY_ID,
                    variables: {id: this.$route.params.id},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.port = _.cloneDeep(response.data.devicePort);
                    this.peripherals = _.transform(_.cloneDeep(response.data.devicePeripheralList), function (result, obj) {
                        obj.name = obj.name + ' - ' + obj.description
                        result.push(obj);
                    }, []);
                    this.port.cables = _.transform(_.cloneDeep(this.port.cables), function (result, obj) {
                        obj.name = obj.code
                        result.push(obj);
                    }, []);
                    this.cables = _.transform(_.cloneDeep(response.data.cableList), function (result, obj) {
                        obj.name = obj.code
                        result.push(obj);
                    }, []);
                    this.portTypes = response.data.portTypes
                    this.portStates = response.data.portStates

                });
            },
            save() {
                if (this.$route.meta.uiMode === 'EDIT') {
                    let portClone = _.cloneDeep(this.port)
                    delete portClone.id
                    if (portClone.peripherals) {
                        portClone.peripherals.forEach(function (peripheral) {
                            delete peripheral.name
                        })
                        portClone.cables.forEach(function (cable) {
                            delete cable.name
                        })
                    }
                    this.$apollo.mutate({
                        mutation: PORT_UPDATE, variables: {id: this.port.id, port: portClone}
                    }).then(response => {
                        this.$router.push({path: "/devices/" + this.$route.params.idPrimary + "/ports/" + response.data.updatePort.id + "/view"})
                    });
                } else if (this.$route.meta.uiMode === 'CREATE') {
                    this.portToUpdate["device"] = this.port.device;
                    this.$apollo.mutate({
                        mutation: PORT_CREATE, variables: {devicePort: this.portToUpdate}
                    }).then(response => {
                        this.$router.push({path: "/devices/" + this.$route.params.idPrimary + "/ports/" + response.data.devicePortCreate.id + "/edit"})
                    });
                }
            },
            navEdit(item, index) {
                this.$router.push({path: "/devices/" + this.$route.params.idPrimary + "/ports/" + this.port.id + "/edit"})
            },
            navConfig(item, index) {
                this.$router.push({path: "/devices/" + this.$route.params.idPrimary + "/ports/" + this.port.id + "/configurations"})
            },
            navDelete(item, index) {
                this.deleteConfirmShow = true;
            },
        }
    }
</script>
<style src="vue-select/dist/vue-select.css"></style>
