<template>
    <CRow>
        <CCol col="6" lg="12">
            <CCard>
                <CCardHeader>
                    <strong>Device details : <span class="field-value">{{ device.uid }}</span> </strong>
                    <div class="card-header-actions" @click="rowClicked">
                        <a style="cursor: pointer" class="card-header-action" rel="noreferrer noopener">
                            <small class="text-muted">edit</small>
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
                    <CRow>
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
                                            <CButton color="success" @click="viewDeviceDetails(data.item.uid)">View
                                            </CButton>
                                            |
                                            <CButton color="danger" @click="modalCheck(data.item)" class="mr-auto">
                                                Remove
                                            </CButton>
                                        </td>
                                    </template>
                                </CDataTable>
                            </div>
                        </CCol>
                    </CRow>
                </CCardBody>
                <CCardFooter>
                    <CButton color="primary" @click="goBack">Back</CButton>
                </CCardFooter>
            </CCard>
        </CCol>
    </CRow>
</template>

<script>
    import {DEVICE_GET_BY_ID_CHILDS} from "../../graphql/zones";


    export default {
        name: 'Device',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        data: () => {
            return {
                deviceDetails: [],
                portsTemplate: [
                    {key: 'id', label: 'ID'},
                    {key: 'name', label: 'Name'},
                    {key: 'internalRef', label: 'Int. Code'},
                    {key: 'type', label: 'Type'},
                    {key: 'state', label: 'State'},
                    {
                        key: 'actions', label: 'Action',
                        sorter: false,
                        filter: false
                    }

                ],
                ports: [],
                readonly: ["id", "__typename", "uid", "ports", "rack", "networkAddress"],
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
                    variables: {uid: this.$route.params.id},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    device = response.data.deviceByUid;
                    const deviceDetailsToMap = device ? Object.entries(device) : [['id', 'Not found']]
                    this.deviceDetails = deviceDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly);
                    this.device = device;

                    this.ports = response.data.deviceByUid.ports
                });
                this.loading = false
            },
            rowClicked(item, index) {
                this.$router.push({path: "/devices/" + this.$route.params.id + "/edit"})
            },
            goBack() {
                this.$router.go(-1)
            }
        }
    }
</script>
<style>
    .field-value {
        color: #abb3c0;
    }
</style>