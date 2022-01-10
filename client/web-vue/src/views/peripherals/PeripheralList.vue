<template>
    <CRow>
        <CCol col="12" xl="8">
            <transition name="slide">
                <CCard>
                    <CCardHeader>
                        {{ $t("peripheral.list.nav_title") }}
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
                                @row-clicked="$router.push({path: '/peripherals/' +  $event.id + '/view'})"
                        >
                            <template #name="data">
                                <td>
                                    <strong>{{data.item.name}}</strong>
                                </td>
                            </template>

                            <template #actions="data">
                                <td>
                                    <a @click="$router.push({path: '/peripherals/' +  data.item.id + '/edit'})" style="padding-right: 1em">
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
                        <CButton color="success" @click="addNew">Add new</CButton>
                    </CCardFooter>
                    <CModal title="Delete peripheral" color="danger" :show.sync="deleteConfirmShow">
                        Do you want delete : <strong>{{selectedItem.name}}</strong> ?
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
    import {PERIPHERAL_LIST_ALL, PERIPHERAL_DELETE} from "../../graphql/queries";

    export default {
        name: 'PeripheralList',
        data: () => {
            return {
                items: [],
                fields: [
                    {key: 'id', label: 'ID'},
                    {key: 'name', label: 'Name'},
                    {key: 'code'},
                    {key: 'description'},
                    {
                        key: 'actions', label: 'Action',
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
                    mutation: PERIPHERAL_DELETE, variables: {id: this.selectedItem.id}
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
                    query: PERIPHERAL_LIST_ALL,
                    variables: {},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.items = response.data.devicePeripheralList;
                });
            },
            peripheralLink(uid) {
                return `peripherals/${uid}/view`
            },
            viewPeripheralDetails(id) {
                const peripheralLink = this.peripheralLink(id)
                this.$router.push({path: peripheralLink})
            }
        }
    }
</script>
