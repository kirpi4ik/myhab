<template>
    <CRow>
        <CCol col="6" lg="12">
            <CCard>
                <CCardHeader>
                    <strong>{{ $t("device.details.title") }} <span class="field-value">{{ device.name }}</span> </strong>
                    <div class="card-header-actions">
                        <a @click="navEdit" style="cursor: pointer" class="card-header-action" rel="noreferrer noopener">
                            <small class="text-muted">{{ $t("actions.edit") }}</small> |
                        </a>
                        <a @click="navConfig" style="cursor: pointer" class="card-header-action" rel="noreferrer noopener">
                            <small class="text-muted">{{ $t("actions.config") }}</small> |
                        </a>
                        <a @click="navDelete" style="cursor: pointer" class="card-header-action" rel="noreferrer noopener">
                            <small class="text-muted">{{ $t("actions.delete") }}</small>
                        </a>
                    </div>
                </CCardHeader>
                <CCardBody>
                    <CRow>
                        <CCol sm="12">
                            <CDataTable
                                    striped
                                    hover
                                    small
                                    fixed
                                    :items="deviceDetails"
                            />
                        </CCol>
                    </CRow>
                    <CRow style="margin-top: 15px" v-if="device.networkAddress != null">
                        <CCol sm="12">
                            <strong> Network :</strong>
                            <span style="margin-left: 15px">{{device.networkAddress.ip}}:{{device.networkAddress.port}}</span>
                        </CCol>
                    </CRow>
                    <CRow style="margin-top: 15px" v-if="device.rack != null">
                        <CCol sm="12">
                            <strong> Rack :</strong>
                            <span style="margin-left: 15px">{{device.rack.name}}</span> -
                            <span>{{device.rack.description}}</span>
                        </CCol>
                    </CRow>
                    <CRow style="margin-top: 15px">
                        <CCol sm="12">
                            <strong> Ports :</strong>
                            <div style="width: 100%;">
                                <CDataTable
                                        hover
                                        bordered
                                        striped
                                        :items="ports"
                                        :fields="portsTemplate"
                                        :items-per-page="6"
                                        :pagination="$options.paginationProps"
                                        index-column
                                        table-filter
                                        sorter
                                        clickable-rows
                                >
                                    <template #name="data">
                                        <td>
                                            <strong>{{data.item.name}}</strong>
                                        </td>
                                    </template>
                                    <template #actions="data">
                                        <td>
                                            <CButton color="success" @click="viewPortDetails(data.item)">{{ $t("actions.view") }}</CButton>
                                            |
                                            <CButton color="danger" @click="modalCheck(data.item)" class="mr-auto"> {{ $t("actions.delete") }}</CButton>
                                        </td>
                                    </template>
                                </CDataTable>
                                <CButton type="submit" size="sm" color="success" @click="$router.push({path: '/devices/'+device.id +'/ports/create'})">
                                    <CIcon name="cil-plus"/>
                                    {{ $t("actions.add_new") }}
                                </CButton>
                            </div>
                        </CCol>
                    </CRow>
                </CCardBody>
                <CCardFooter>
                    <CButton color="primary" @click="$router.push({path: '/devices'})">{{ $t("actions.nav.back") }}</CButton>
                </CCardFooter>
            </CCard>
        </CCol>
        <CModal :title="$t('modal.delete.title')" color="danger" :show.sync="deleteConfirmShow">
            {{ $t("modal.delete.confirmation_msg") }} <strong>{{device.name}}</strong> ?
            <template #footer>
                <CButton @click="removeDevice" color="danger">{{ $t("actions.delete") }}</CButton>
                <CButton @click="deleteConfirmShow = false" color="success">{{ $t("actions.cancel") }}</CButton>
            </template>
        </CModal>
    </CRow>
</template>

<script>
    import {DEVICE_GET_BY_ID_CHILDS, DEVICE_DELETE} from "../../graphql/queries";
    import i18n from './../../i18n'


    export default {
        name: 'DeviceView',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        data: () => {
            return {
                deleteConfirmShow: false,
                deviceDetails: [],
                portsTemplate: [
                    {key: 'id', label: 'ID'},
                    {key: 'name', label: 'Name'},
                    {key: 'internalRef', label: 'Int. Code'},
                    {key: 'type', label: 'Type'},
                    {key: 'state', label: 'State'},
                    {
                        key: 'actions', label: i18n.t('table.header.actions'),
                        sorter: false,
                        filter: false
                    }

                ],
                ports: [],
                readonly: ["id", "__typename", "uid", "ports", "type", "rack", "networkAddress", "authAccounts"],
                device: {}
            }
        },
        mounted() {
            this.loadDevice();
        },
        paginationProps: {
            align: 'center',
            doubleArrows: false,
            previousButtonHtml: 'prev',
            nextButtonHtml: 'next'
        },
        watch: {
            '$root.componentKey': 'loadDevice'
        },
        methods: {
            loadDevice() {
                this.loading = true;
                let device = {};
                let removeReadonly = function (keyMap) {
                    return !this.readonly.includes(keyMap.key)
                }.bind(this);

                this.$apollo.query({
                    query: DEVICE_GET_BY_ID_CHILDS,
                    variables: {id: this.$route.params.deviceId},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    device = response.data.device;
                    const deviceDetailsToMap = device ? Object.entries(device) : [['id', 'Not found']]
                    this.deviceDetails = deviceDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly);
                    this.device = device;

                    this.ports = response.data.device.ports
                });
                this.loading = false
            },
            removeDevice() {
                this.$apollo.mutate({
                    mutation: DEVICE_DELETE, variables: {id: this.device.id}
                }).then(response => {
                    this.deleteConfirmShow = false;
                    this.$router.push({path: "/devices/"})
                });
            },
            navEdit(item, index) {
                this.$router.push({path: "/devices/" + this.$route.params.deviceId + "/edit"})
            },
            navConfig(item, index) {
                this.$router.push({path: "/devices/" + this.$route.params.deviceId + "/configurations"})
            },
            navDelete(item, index) {
                this.deleteConfirmShow = true;
            },
            goBack() {
                this.$router.go(-1)
            },
            viewPortDetails(item) {
                this.$router.push({path: "/devices/" + this.$route.params.deviceId + "/ports/" + item.id + "/view"})
            }
        }
    }
</script>
<style>
    .field-value {
        color: #abb3c0;
    }
</style>