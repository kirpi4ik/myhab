<template>
    <CRow>
        <CCol col="12" xl="8">
            <transition name="slide">
                <CCard>
                    <CCardHeader>
                        Periferice
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
                        >
                            <template #peripheralname="data">
                                <td>
                                    <strong>{{data.item.peripheralname}}</strong>
                                </td>
                            </template>

                            <template #status="data">
                                <td>
                                    <CButton color="success" @click="viewPeripheralDetails(data.item.uid)">View</CButton>
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
                    <CModal title="Delete peripheral" color="danger" :show.sync="deleteConfirmShow">
                        Do you want delete : <strong>{{selectedItem.peripheralname}}</strong> ?
                        <template #footer>
                            <CButton @click="removePeripheral" color="danger">Delete</CButton>
                            <CButton @click="deleteConfirmShow = false" color="success">Cancel</CButton>
                        </template>
                    </CModal>
                </CCard>
            </transition>
        </CCol>
    </CRow>
</template>

<script>
    import {USERS_GET_ALL, USER_DELETE} from "../../graphql/zones";

    export default {
        name: 'Peripherals',
        data: () => {
            return {
                items: [],
                fields: [
                    {key: 'peripheralname', label: 'Name'},
                    {key: 'registered'},
                    {key: 'role'},
                    {key: 'status'}
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
            this.loadPeripherals();
        },
        watch: {
            '$route.fullPath': 'loadPeripherals'
        },
        methods: {
            modalCheck(item) {
                this.deleteConfirmShow = true;
                this.selectedItem = item
            },
            removePeripheral() {
                this.$apollo.mutate({
                    mutation: USER_DELETE, variables: {id: this.selectedItem.id}
                }).then(response => {
                    this.deleteConfirmShow = false
                    this.loadPeripherals();
                });
            },
            addNew() {
                this.$router.push({path: "/peripherals/create"})
            },
            loadPeripherals() {
                this.$apollo.query({
                    query: USERS_GET_ALL,
                    variables: {},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.items = response.data.peripheralList;
                });
            },
            getBadge(status) {
                return status === 'Active' ? 'success'
                    : status === 'Inactive' ? 'secondary'
                        : status === 'Pending' ? 'warning'
                            : status === 'Banned' ? 'danger' : 'primary'
            },
            peripheralLink(uid) {
                return `peripherals/${uid}/profile`
            },
            viewPeripheralDetails(uid) {
                const peripheralLink = this.peripheralLink(uid)
                this.$router.push({path: peripheralLink})
            }
        }
    }
</script>
