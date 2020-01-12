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
                                table-filter
                                sorter
                                clickable-rows
                                pagination
                        >
                            <template #name="data">
                                <td>
                                    <strong>{{data.item.name}}</strong>
                                </td>
                            </template>

                            <template #actions="data">
                                <td>
                                    <CButton color="success" @click="viewDeviceDetails(data.item.uid)">View</CButton>
                                    |
                                    <CButton color="danger" @click="modalCheck(data.item)" class="mr-auto">
                                        Remove
                                    </CButton>
                                </td>
                            </template>
                        </CDataTable>
                    </CCardBody>
                    <CCardFooter>
                        <CButton color="success" @click="addNew">Add new</CButton>
                    </CCardFooter>
                    <CModal title="Delete device" color="danger" :show.sync="deleteConfirmShow">
                        Do you want delete : <strong>{{selectedItem.name}}</strong> ?
                        <template #footer>
                            <CButton @click="removeDevice" color="danger">Delete</CButton>
                            <CButton @click="deleteConfirmShow = false" color="success">Cancel</CButton>
                        </template>
                    </CModal>
                </CCard>
            </transition>
        </CCol>
    </CRow>
</template>

<script>
    import {DEVICE_LIST_ALL, DEVICE_DELETE} from "../../graphql/zones";

    export default {
        name: 'Devices',
        data: () => {
            return {
                items: [],
                fields: [
                    {key: 'id', label: 'ID'},
                    {key: 'name', label: 'Name'},
                    {key: 'code', label: 'Code'},
                    {key: 'description', label: 'Description'},
                    {
                        key: 'actions', label: 'Action',
                        sorter: false,
                        filter: false
                    }

                ],
                perPage: 5,
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
            loadDevices() {
                this.$apollo.query({
                    query: DEVICE_LIST_ALL,
                    variables: {},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.items = response.data.deviceList;
                });
            },
            deviceLink(uid) {
                return `devices/${uid}/view`
            },
            viewDeviceDetails(uid) {
                const deviceLink = this.deviceLink(uid);
                this.$router.push({path: deviceLink})
            }
        }
    }
</script>
