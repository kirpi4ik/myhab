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
                                   @click="$router.push({path: '/devices/' + $route.params.idPrimary + '/view'})">
                                    <small class="text-muted">{{ $t("actions.cancel") }}</small>
                                </a>
                            </div>
                        </CCardHeader>
                        <CCardBody>
                            <CRow v-for="(cableDetail, index) in cableDetails" :key="`detail-${index}`">
                                <CCol sm="12">
                                    <CInput v-if="!isBoolean(cableDetail.key) && !isArray(cableDetail)&& !isObject(cableDetail)" :label="cableDetail.key.charAt(0).toUpperCase()+cableDetail.key.slice(1)"
                                            :value="cableDetail.value"
                                            @input="updateFieldValue($event, cableDetail.key)"
                                            :ref="cableDetail.key"/>
                                    <CInputCheckbox v-if="isBoolean(cableDetail.key)"
                                                    :key="cableDetail.key"
                                                    :label="cableDetail.key.charAt(0).toUpperCase()+cableDetail.key.slice(1)"
                                                    :value="cableDetail.value"
                                                    :checked="cableDetail.value"
                                                    @update:checked="check($event, cableDetail.key)"
                                                    :inline="true"
                                                    :ref="cableDetail.key"
                                    />
                                    <CSelect v-if="!isArray(cableDetail)"
                                                    :label="cableDetail.key.charAt(0).toUpperCase()+cableDetail.key.slice(1)"
                                                    :options="cableDetail.value"
                                                    @update:checked="check($event, cableDetail.key)"
                                    />
                                    <multiselect v-if="isArray(cableDetail)"
                                                 :options="cableDetail.value"
                                                 track-by="name"
                                                 label="name">
                                    </multiselect>
                                </CCol>
                            </CRow>

                            <CRow>
                                {{ $t("cable.fields.type") }}
                                <multiselect v-model="cableToUpdateType" :options="cableTypes">
                                </multiselect>
                            </CRow>
                            <CRow>
                                {{ $t("cable.fields.state") }}
                                <multiselect v-model="cableToUpdateState" :options="cableStates">
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
    import {CABLE_BY_ID, CABLE_VALUE_UPDATE, CABLE_CREATE, DEVICE_GET_BY_ID_MINIMAL} from "../../graphql/queries";

    import Multiselect from 'vue-multiselect'

    export default {
        name: 'CableEditor',
        components: {
            'multiselect': Multiselect
        },
        data: () => {
            return {
                cableDetails: [],
                cableToUpdate: {},
                cable: [],
                readonly: ["id", "__typename", "uid", "device", "type", "state"],
                booleans: ["runAction", "mustSendToServer", "runScenario"],
                deleteConfirmShow: false,
                cableToUpdateType: null,
                cableToUpdateState: null,
                cableTypes: [
                    "UNKNOW",
                    "IN",
                    "OUT",
                    "ADC",
                    "DSEN",
                    "I2C",
                    "NOT_CONFIGURED"
                ],
                cableStates: [
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
                this.cable = {
                    internalRef: "",
                    name: "",
                    description: "",
                    mode: "",
                    model: "",
                    device: {
                        id: this.$route.params.idPrimary
                    }
                };
                let removeReadonly = function (keyMap) {
                    return !this.readonly.includes(keyMap.key)
                }.bind(this);

                this.cableDetails = Object.entries(this.cable).map(([key, value]) => {
                    return {key, value}
                }).filter(removeReadonly);
                this.cableToUpdateType = this.cableTypes[0];
                this.cableToUpdateState = this.cableStates[0];
            },
            init() {
                let cable = {};
                this.$apollo.query({
                    query: CABLE_BY_ID,
                    variables: {id: this.$route.params.idPrimary},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    let removeReadonly = function (keyMap) {
                        return !this.readonly.includes(keyMap.key)
                    }.bind(this);
                    this.cable = response.data.cable;
                    const cableDetailsToMap = cable ? Object.entries(this.cable) : [['id', 'Not found']];
                    this.cableDetails = cableDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly);

                    this.cableToUpdateType = this.cable.type;
                    this.cableToUpdateState = this.cable.state;
                });
            },
            isBoolean(key) {
                return this.booleans.indexOf(key) != -1;
            },
            isArray(item) {
                debugger
                return typeof item.value === 'array';
            },
            isObject(item) {
                return typeof item.value === 'object' && item.value !== null
            },
            check(value, key) {
                this.cableToUpdate[key] = value

            },
            updateFieldValue(value, key) {
                this.cableToUpdate[key] = value
            },
            save() {
                if (this.$route.meta.uiMode === 'EDIT') {
                    this.$apollo.mutate({
                        mutation: CABLE_VALUE_UPDATE, variables: {id: this.cable.id, deviceCable: this.cableToUpdate}
                    }).then(response => {
                        this.$router.push({path: "/devices/" + this.$route.params.idPrimary + "/cables/" + response.data.deviceCableUpdate.id + "/view"})
                    });
                } else if (this.$route.meta.uiMode === 'CREATE') {
                    this.cableToUpdate["device"] = this.cable.device;
                    this.$apollo.mutate({
                        mutation: CABLE_CREATE, variables: {deviceCable: this.cableToUpdate}
                    }).then(response => {
                        this.$router.push({path: "/devices/" + this.$route.params.idPrimary + "/cables/" + response.data.deviceCableCreate.id + "/edit"})
                    });
                }
            },
            navEdit(item, index) {
                this.$router.push({path: "/devices/" + this.$route.params.idPrimary + "/cables/" + this.cable.id + "/edit"})
            },
            navConfig(item, index) {
                this.$router.push({path: "/devices/" + this.$route.params.idPrimary + "/cables/" + this.cable.id + "/configurations"})
            },
            navDelete(item, index) {
                this.deleteConfirmShow = true;
            },
        }
    }
</script>
<style src="vue-multiselect/dist/vue-multiselect.min.css"></style>
