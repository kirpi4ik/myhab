<template>
    <div>
        <CRow>
            <CCol sm="6">
                <CCard>
                    <CForm>
                        <CCardHeader>
                            <strong>Cable </strong> <small>{{cable.name}}</small>
                            <div class="card-header-actions">
                                <a @click="navConfig" style="cursor: pointer" class="card-header-action" rel="noreferrer noopener" v-if="cable.id != null">
                                    <small class="text-muted">{{ $t("actions.config") }}</small> |
                                </a>
                                <a @click="navDelete" style="cursor: pointer" class="card-header-action" rel="noreferrer noopener" v-if="cable.id != null">
                                    <small class="text-muted">{{ $t("actions.delete") }}</small> |
                                </a>
                                <a style="cursor: pointer" class="card-header-action" rel="noreferrer noopener"
                                   @click="navCancel">
                                    <small class="text-muted">{{ $t("actions.cancel") }}</small>
                                </a>
                            </div>
                        </CCardHeader>
                        <CCardBody>
                            <CRow>
                                <CCol sm="12">
                                    <CInput label="Code" :value="cable['code']" @input="updateFieldValue($event, 'code')" ref="code" sync/>
                                </CCol>
                            </CRow>
                            <CRow>
                                <CCol sm="12">
                                    <CInput label="Description" :value="cable['description']" @input="updateFieldValue($event, 'description')" ref="description" sync/>
                                </CCol>
                            </CRow>
                            <CRow>
                                <CCol sm="12">
                                    <CInput label="Code new" :value="cable['codeNew']" @input="updateFieldValue($event, 'codeNew')" ref="codeNew" sync/>
                                </CCol>
                            </CRow>
                            <CRow>
                                <CCol sm="12">
                                    <CInput label="Code old" :value="cable['codeOld']" @input="updateFieldValue($event, 'codeOld')" ref="codeOld" sync/>
                                </CCol>
                            </CRow>
                            <CRow>
                                <CCol sm="12">
                                    <CInput label="Max amp" :value="cable['maxAmp']" @input="updateFieldValue($event, 'maxAmp')" ref="maxAmp" sync/>
                                </CCol>
                            </CRow>
                            <CRow>
                                <CCol sm="12">
                                    <CInput label="Nr wires" :value="cable['nrWires']" @input="updateFieldValue($event, 'nrWires')" ref="nrWires" sync/>
                                </CCol>
                            </CRow>
                            <CRow class="form-group">
                                <CCol>
                                    <label>Category</label>
                                    <v-select label="name" v-model="cable.category" :options="categories"/>
                                </CCol>
                            </CRow>
                            <CRow class="form-group">
                                <CCol>
                                    <label>Rack</label>
                                    <v-select label="name" v-model="cable.rack" :options="racks" @input="rackChanged"/>
                                </CCol>
                            </CRow>
                            <CRow class="form-group">
                                <CCol sm="5">
                                    <label>Patch panel</label>
                                    <v-select label="name" v-model="cable.patchPanel" :options="patchPanels"/>
                                </CCol>
                                <CCol>
                                    <label>Port nr</label>
                                    <v-select v-model="cable.patchPanelPort" :options="patchPanelPorts" :reduce="portNr => portNr.value"/>
                                </CCol>
                            </CRow>
                            <CRow class="form-group">
                                <CCol>
                                    <label>Zones</label>
                                    <v-select label="name" v-model="cable.zones" :options="zones" multiple/>
                                </CCol>
                            </CRow>
                            <CRow class="form-group">
                                <CCol>
                                    <label>Peripherals</label>
                                    <v-select label="name" v-model="cable.peripherals" :options="peripherals" multiple/>
                                </CCol>
                            </CRow>
                            <CRow class="form-group">
                                <CCol>
                                    <label>Connected to ports</label>
                                    <v-select label="name" v-model="cable.connectedTo" :options="ports" multiple/>
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
    import {CABLE_CREATE, CABLE_CREATE_GET_DETAILS, CABLE_EDIT_GET_DETAILS, CABLE_VALUE_UPDATE} from "../../graphql/queries";
    import vSelect from "vue-select";
    import _ from "lodash";

    export default {
        name: 'CableEditor',
        components: {
            'v-select': vSelect
        },
        data: () => {
            return {
                cable: {},
                categories: [],
                racks: [],
                zones: [],
                peripherals: [],
                patchPanels: [],
                ports: [],
                patchPanelPorts: [],
                deleteConfirmShow: false,
                viewMode: 'CREATE'
            }
        },
        created() {
            this.viewMode = this.$route.meta.uiMode
            if (this.viewMode === 'CREATE') {
                this.initCreate();

            } else {
                this.init();
            }
        },
        methods: {
            initCreate() {
                this.$apollo.query({
                    query: CABLE_CREATE_GET_DETAILS,
                    variables: {},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.zones = _.transform(response.data.zoneList, function (result, obj) {
                        result.push({label: obj.name, value: obj.id});
                    }, []);
                    this.racks = response.data.rackList
                    this.ports = response.data.devicePortList
                    this.categories = response.data.cableCategoryList;
                    this.peripherals = response.data.devicePeripheralList;
                    this.patchPanels = response.data.patchPanelList
                    this.patchPanelPorts = _.transform(_.range(1, 49), function (result, obj) {
                        result.push({label: obj, value: obj});
                    }, []);
                });
            },
            init() {
                this.$apollo.query({
                    query: CABLE_EDIT_GET_DETAILS,
                    variables: {id: this.$route.params.idPrimary},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.cable = _.cloneDeep(response.data.cable);
                    this.cable.connectedTo = _.transform(this.cable.connectedTo, function (result, obj) {
                        let portName = _.clone(obj.name)
                        obj.name = obj.device.code + ' - ' + portName + ' - [' + obj.internalRef + ']'
                        result.push(obj);
                    }, []);
                    this.zones = response.data.zoneList;
                    this.peripherals = response.data.devicePeripheralList;
                    this.racks = response.data.rackList
                    this.ports = _.transform(_.cloneDeep(response.data.devicePortList), function (result, obj) {
                        let portName = _.clone(obj.name)
                        obj.name = obj.device.code + ' - ' + portName + ' - [' + obj.internalRef + ']'
                        result.push(obj);
                    }, []);
                    this.categories = response.data.cableCategoryList;
                    this.patchPanels = response.data.patchPanelList
                    this.patchPanelPorts = _.transform(_.range(1, 49), function (result, obj) {
                        result.push({label: obj, value: obj});
                    }, []);

                });
            },
            rackChanged(newVal) {
                this.patchPanels = newVal.patchPanels;
            },
            updateFieldValue(newVal, field) {
                this.cable[field] = newVal
            },
            save() {
                if (this.$route.meta.uiMode === 'EDIT') {
                    let cableClone = _.cloneDeep(this.cable)
                    delete cableClone.id
                    if (cableClone.connectedTo) {
                        cableClone.connectedTo.forEach(function (port) {
                            delete port.device
                            delete port.name
                        })
                    }
                    this.$apollo.mutate({
                        mutation: CABLE_VALUE_UPDATE, variables: {id: this.cable.id, cable: cableClone}
                    }).then(response => {
                        this.$router.push({path: "/cables/" + response.data.updateCable.id + "/view"})
                    });
                } else if (this.$route.meta.uiMode === 'CREATE') {
                    this.$apollo.mutate({
                        mutation: CABLE_CREATE, variables: {cable: this.cableToUpdate}
                    }).then(response => {
                        this.$router.push({path: "/cables/" + response.data.cableUpdate.id + "/edit"})
                    });
                }
            },
            navEdit(item, index) {
                this.$router.push({path: "/cables/" + this.cable.id + "/edit"})
            },
            navConfig(item, index) {
                this.$router.push({path: "/cables/" + this.cable.id + "/configurations"})
            },
            navDelete(item, index) {
                this.deleteConfirmShow = true;
            },
            navCancel(item, index) {
                if (this.viewMode === 'CREATE') {
                    this.$router.push({path: '/cables/'})
                } else {
                    this.$router.push({path: '/cables/' + this.cable.id + '/view'})
                }
            },
        }
    }
</script>
<style src="vue-select/dist/vue-select.css"></style>
