<template>
    <div>
        <CRow>
            <CCol sm="6">
                <CCard>
                    <CForm>
                        <CCardHeader>
                            <strong>{{ $t("device.edit.nav_title") }} </strong> <small>{{device.name}}</small>
                            <div class="card-header-actions">
                                <a style="cursor: pointer" class="card-header-action" rel="noreferrer noopener"
                                   @click="$router.go(-1)">
                                    <small class="text-muted">{{ $t("actions.cancel") }}</small>
                                </a>
                            </div>
                        </CCardHeader>
                        <CCardBody>
                            <CRow v-for="(deviceDetail, index) in deviceDetails" :key="`detail-${index}`">
                                <CCol sm="12">
                                    <CInputCheckbox
                                            v-if="deviceDetail.key.endsWith('ed')"
                                            :key="deviceDetail.key"
                                            :label="deviceDetail.key.charAt(0).toUpperCase()+deviceDetail.key.slice(1)"
                                            :value="deviceDetail.value"
                                            :checked="deviceDetail.value"
                                            @update:checked="check($event, deviceDetail.key)"
                                            :inline="true"
                                            :ref="deviceDetail.key"
                                    />
                                    <CInput v-if="!deviceDetail.key.endsWith('ed')"
                                            :label="deviceDetail.key.charAt(0).toUpperCase()+deviceDetail.key.slice(1)"
                                            :placeholder="deviceDetail.key"
                                            :value="deviceDetail.value"
                                            @input="device[deviceDetail.key] =  $event"
                                            :ref="deviceDetail.key"/>
                                </CCol>
                            </CRow>
                            <CRow>
                                <b> {{ $t("device.fields.type.label") }}</b>
                                <multiselect
                                        v-model="device.type"
                                        :options="deviceTypes.options"
                                        track-by="name"
                                        label="name">
                                </multiselect>
                            </CRow>
                            <CRow style="margin-top: 15px">
                                <b> {{ $t("device.fields.rack.label") }}</b>
                                <multiselect
                                        v-model="device.rack"
                                        :options="deviceRacks.options"
                                        track-by="name"
                                        label="name">
                                </multiselect>
                            </CRow>
                            <CRow style="margin-top: 15px">
                                <b>{{ $t("device.fields.networkAddress.label") }}</b>
                                <CCol sm="12">
                                    <CInput :label="$t('device.fields.networkAddress.fields.ip')"
                                            :placeholder="$t('device.fields.networkAddress.fields.ip')"
                                            :value="device.networkAddress.ip"
                                            @input="device.networkAddress.ip = $event"/>
                                    <CInput :label="$t('device.fields.networkAddress.fields.port')"
                                            :placeholder="$t('device.fields.networkAddress.fields.port')"
                                            :value="device.networkAddress.port"
                                            @input="device.networkAddress.port = $event"/>
                                    <CInput :label="$t('device.fields.networkAddress.fields.gateway')"
                                            :placeholder="$t('device.fields.networkAddress.fields.gateway')"
                                            :value="device.networkAddress.gateway"
                                            @input="device.networkAddress.gateway = $event"/>
                                </CCol>
                            </CRow>
                            <CRow style="margin-top: 15px">
                                <b>{{ $t("device.fields.authAccounts.label") }}</b>
                                <CCol sm="12">
                                    <CDataTable
                                            hover
                                            bordered
                                            striped
                                            :items="device.authAccounts"
                                            :fields="accountFields"
                                            :items-per-page="6"
                                            index-column
                                            table-filter
                                            sorter
                                            clickable-rows
                                    >
                                        <template #username="{item}">
                                            <td>
                                                <strong v-if="!item.editable">{{item.username}}</strong>
                                                <CInput v-if="item.editable" :value="item.username" @input="item.username = $event"/>
                                            </td>
                                        </template>
                                        <template #password="{item}">
                                            <td>
                                                <strong v-if="!item.editable">*******</strong>
                                                <CInput v-if="item.editable" :value="item.password" @input="item.password = $event"/>
                                            </td>
                                        </template>
                                        <template #isDefault="{item}">
                                            <td>
                                                <strong v-if="!item.editable">{{item.isDefault}}</strong>
                                                <CInputCheckbox
                                                        v-if="item.editable"
                                                        :key="item.id"
                                                        :value="item.isDefault"
                                                        :checked="item.isDefault"
                                                        @update:checked="item.isDefault = $event"
                                                        :inline="true"
                                                />
                                            </td>
                                        </template>
                                        <template #actions="{item}">
                                            <td>
                                                <CLink @click="accountEdit(item)" v-if="!item.editable">
                                                    <CIcon name="cil-pencil" style="color: #4d80e0"/>
                                                </CLink>
                                                <CLink @click="accountSave(item)" v-if="item.editable">
                                                    <CIcon name="cil-check" style="color: #20cc3a"/>
                                                </CLink>
                                                |
                                                <CLink @click="accountDeleteConfirm(item)">
                                                    <CIcon name="cil-ban" style="color: #cc271b"/>
                                                </CLink>
                                            </td>
                                        </template>
                                    </CDataTable>
                                    <CButton type="submit" size="sm" color="success" @click="accountAdd">
                                        <CIcon name="cil-plus"/>
                                        {{ $t("actions.add") }}
                                    </CButton>
                                </CCol>
                            </CRow>
                        </CCardBody>
                        <CCardFooter>
                            <CButton type="submit" size="sm" color="primary" @click="save">
                                <CIcon name="cil-check"/>
                                {{ $t("actions.save") }}
                            </CButton>
                            <CButton type="reset" size="sm" color="danger" @click="$router.go(-1)">
                                <CIcon name="cil-ban"/>
                                {{ $t("actions.cancel") }}
                            </CButton>
                        </CCardFooter>
                    </CForm>
                </CCard>
            </CCol>
        </CRow>
        <CModal :title="$t('modal.delete.title')" color="danger" :show.sync="deleteConfirmShow">
            {{ $t("modal.delete.confirmation_msg") }} <strong>{{selectedItem.username}}</strong> ?
            <template #footer>
                <CButton @click="removeAccount" color="danger">{{ $t("actions.delete") }}</CButton>
                <CButton @click="deleteConfirmShow = false" color="success">{{ $t("actions.cancel") }}</CButton>
            </template>
        </CModal>
        <CToaster :autohide="3000">
            <template v-for="toast in fixedToasts">
                <CToast :key="'toast-' + toast.message" :show="true" :header="toast.header" :color="toast.color" >
                    {{toast.message}}
                </CToast>
            </template>
        </CToaster>
    </div>
</template>


<script>
    import {
        DEVICE_ACCOUNT_UPDATE,
        DEVICE_ACCOUNT_CREATE,
        DEVICE_ACCOUNT_DELETE,
        DEVICE_GET_DETAILS_FOR_EDIT,
        DEVICE_UPDATE
    } from "../../graphql/queries";
    import Multiselect from 'vue-multiselect'
    import {uid} from 'uid';
    import _ from 'lodash';

    export default {
        name: 'DeviceEdit',
        components: {
            'multiselect': Multiselect
        },
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        data: () => {
            return {
                deviceDetails: [],
                device: {
                    type: null,
                    networkAddress: [],
                    rack: [],
                    authAccounts: []
                },
                deviceModels: {
                    options: ['MEGAD_2561_RTC', 'ESP8266_1', 'TMEZON_INTERCOM', 'NIBE_F1145_8_EM']
                },
                deviceTypes: {
                    options: []
                },
                deviceRacks: {
                    options: []
                },
                deviceToUpdate: {},
                readonly: ["id", "__typename", "uid", "type", "networkAddress", "authAccounts", "rack", "ports"],
                accountFields: [
                    {key: 'id', label: 'ID'},
                    {key: 'username', label: 'Name'},
                    {key: 'password', label: 'Password'},
                    {key: 'isDefault', label: 'Implicit'},
                    {key: 'actions', label: 'Actiuni'}
                ],
                deleteConfirmShow: false,
                selectedItem: [],
                fixedToasts: []

            }
        },
        created() {
            this.init();
        },
        methods: {
            check(value, key) {
                this.deviceToUpdate[key] = value

            },
            updateFieldValue(value, key) {
                this.deviceToUpdate[key] = value
            },
            init() {
                let removeReadonly = function (keyMap) {
                    return !this.readonly.includes(keyMap.key)
                }.bind(this);

                this.$apollo.query({
                    query: DEVICE_GET_DETAILS_FOR_EDIT,
                    variables: {id: this.$route.params.idPrimary},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.device = _.cloneDeep(response.data.device);
                    // this.device = response.data.device;
                    if (this.device.networkAddress == null) {
                        this.device.networkAddress = {}
                    }
                    this.deviceTypes.options = response.data.deviceTypeList;
                    this.deviceRacks.options = response.data.rackList;

                    const deviceDetailToMap = this.device ? Object.entries(this.device) : [['id', 'Not found']];
                    this.deviceDetails = deviceDetailToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly);
                });
            },
            accountEdit(item) {
                this.$set(item, 'editable', true);
            },
            accountSave(item) {
                this.$set(item, 'editable', false);
                if (item.id == null) {
                    delete item.tempId;
                    delete item.editable;
                    this.$apollo.mutate({
                        mutation: DEVICE_ACCOUNT_CREATE, variables: {deviceAccount: item}
                    }).then(response => {
                        this.fixedToasts.push({
                            color: "success",
                            header: "Account",
                            message: response.data.deviceAccountCreate.username + ' added !'
                        });
                        this.init();
                    });
                } else {
                    delete item.editable;
                    let refId = item.id;
                    delete item.id;
                    this.$apollo.mutate({
                        mutation: DEVICE_ACCOUNT_UPDATE, variables: {id: refId, deviceAccount: item}
                    }).then(response => {
                        this.fixedToasts.push({
                            color: "success",
                            header: "Account",
                            message: response.data.deviceAccountUpdate.username + ' updated !'
                        });
                    });
                    this.init();
                }
            },
            accountDeleteConfirm(item) {
                this.selectedItem = item;
                this.deleteConfirmShow = true;
            },
            removeAccount() {
                let tempId = this.selectedItem.tempId;
                if (this.selectedItem.id == null) {
                    let index = this.device.authAccounts.findIndex(k => k.tempId != null && k.tempId == tempId);
                    this.$delete(this.device.authAccounts, index);

                } else {
                    this.$apollo.mutate({
                        mutation: DEVICE_ACCOUNT_DELETE, variables: {id: this.selectedItem.id}
                    }).then(response => {
                        if (response.data.deviceAccountDelete.success) {
                            this.fixedToasts.push({
                                color: "success",
                                header: "Account",
                                message: this.selectedItem.username + ' Account Removed !'
                            });
                        }
                        this.init();
                    });
                }
                this.deleteConfirmShow = false;
            },
            accountAdd() {
                let newItem = {
                    tempId: uid(),
                    editable: true,
                    device: {
                        id: this.device.id
                    }
                };
                if (this.device.authAccounts.length > 0) {
                    this.device.authAccounts.push(newItem);
                } else {
                    this.device.authAccounts = [newItem];
                }
            },
            save() {
                let id = this.device.id;
                delete this.device.id;
                this.$apollo.mutate({
                    mutation: DEVICE_UPDATE, variables: {id: id, device: this.device}
                }).then(response => {
                    this.fixedToasts.push({
                        color: "success",
                        header: "Saved",
                        message: response.data.deviceUpdate.name + ' saved !'
                    });
                    this.init();
                });
            }
        }
    }
</script>
<style src="vue-multiselect/dist/vue-multiselect.min.css"></style>