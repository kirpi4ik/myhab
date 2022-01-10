<template>
    <CRow>
        <CCol col="12" xl="8">
            <transition name="slide">
                <CCard>
                    <CCardHeader>
                        Dispozitive
                    </CCardHeader>
                    <CCardBody>
                        <CDataTable
                                hover
                                striped
                                :items="items"
                                :fields="fields"
                                :items-per-page="perPage"
                                :pagination="$options.paginationProps"
                                index-column
                                clickable-rows
                                table-filter
                                columnFilter
                                sorter
                                responsive
                                @row-clicked="navView"
                        >
                            <template #name="data">
                                <td>
                                    <strong>{{data.item.name}}</strong>
                                </td>
                            </template>

                            <template #actions="data">
                                <td>
                                    <a @click="$router.push({path: '/devices/'+data.item.id +'/edit'})" style="padding-right: 1em">
                                        <font-awesome-icon icon="edit" size="1x"/>
                                    </a>
                                    <a @click="modalCheck(data.item)">
                                        <font-awesome-icon icon="ban" size="1x"/>
                                    </a>
                                </td>
                            </template>
                        </CDataTable>
                    </CCardBody>
                    <CCardFooter>
                        <CButton color="success" @click="addNew">{{ $t("actions.create") }}</CButton>
                    </CCardFooter>
                    <CModal :title="$t('modal.delete.title')" color="danger" :show.sync="deleteConfirmShow">
                        {{ $t("modal.delete.confirmation_msg") }} <strong>{{selectedItem.name}}</strong> ?
                        <template #footer>
                            <CButton @click="removeDevice" color="danger">{{ $t("actions.delete") }}</CButton>
                            <CButton @click="deleteConfirmShow = false" color="success">{{ $t("actions.cancel") }}</CButton>
                        </template>
                    </CModal>
                </CCard>
            </transition>
        </CCol>
    </CRow>
</template>

<script>
    import {DEVICE_LIST_ALL, DEVICE_DELETE} from "../../graphql/queries";
    import i18n from './../../i18n'

    export default {
        name: 'DeviceList',
        data: () => {
            return {
                items: [],
                fields: [
                    {key: 'id', label: 'ID'},
                    {key: 'name', label: 'Name'},
                    {key: 'code', label: 'Code'},
                    {key: 'description', label: 'Description'},
                    {
                        key: 'actions', label: i18n.t('table.header.actions'),
                        sorter: false,
                        filter: false
                    }

                ],
                perPage: 10,
                deleteConfirmShow: false,
                selectedItem: {}
            }
        },
        paginationProps: {
            align: 'center',
            doubleArrows: false,
            previousButtonHtml: 'prev',
            nextButtonHtml: 'next'
        },
        mounted() {
            this.loadDevices();
        },
        watch: {
            '$route.fullPath': 'loadDevices'
        },
        methods: {
            modalCheck(item) {
                this.deleteConfirmShow = true;
                this.selectedItem = item
            },
            removeDevice() {
                this.$apollo.mutate({
                    mutation: DEVICE_DELETE, variables: {id: this.selectedItem.id}
                }).then(response => {
                    this.deleteConfirmShow = false;
                    this.loadDevices();
                });
            },
            addNew() {
                this.$router.push({path: "/devices/create"})
            },
            navView(device) {
                this.$router.push({path: '/devices/' + device.id + '/view'})
            },
            loadDevices() {
                this.$apollo.query({
                    query: DEVICE_LIST_ALL,
                    variables: {},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.items = response.data.deviceList;
                });
            },
            deviceLink(id) {
                return `devices/${id}/view`
            },
            viewDeviceDetails(id) {
                const deviceLink = this.deviceLink(id);
                this.$router.push({path: deviceLink})
            }
        }
    }
</script>
