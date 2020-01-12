<template>
    <CRow>
        <CCol col="12" xl="8">
            <transition name="slide">
                <CCard>
                    <CCardHeader>
                        Cabluri
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
                            <template #cablename="data">
                                <td>
                                    <strong>{{data.item.cablename}}</strong>
                                </td>
                            </template>

                            <template #status="data">
                                <td>
                                    <CButton color="success" @click="viewCableDetails(data.item.uid)">View</CButton>
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
                    <CModal title="Delete cable" color="danger" :show.sync="deleteConfirmShow">
                        Do you want delete : <strong>{{selectedItem.cablename}}</strong> ?
                        <template #footer>
                            <CButton @click="removeCable" color="danger">Delete</CButton>
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
        name: 'Cables',
        data: () => {
            return {
                items: [],
                fields: [
                    {key: 'cablename', label: 'Name'},
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
            this.loadCables();
        },
        watch: {
            '$route.fullPath': 'loadCables'
        },
        methods: {
            modalCheck(item) {
                this.deleteConfirmShow = true;
                this.selectedItem = item
            },
            removeCable() {
                this.$apollo.mutate({
                    mutation: USER_DELETE, variables: {id: this.selectedItem.id}
                }).then(response => {
                    this.deleteConfirmShow = false
                    this.loadCables();
                });
            },
            addNew() {
                this.$router.push({path: "/cables/create"})
            },
            loadCables() {
                this.$apollo.query({
                    query: USERS_GET_ALL,
                    variables: {},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.items = response.data.cableList;
                });
            },
            getBadge(status) {
                return status === 'Active' ? 'success'
                    : status === 'Inactive' ? 'secondary'
                        : status === 'Pending' ? 'warning'
                            : status === 'Banned' ? 'danger' : 'primary'
            },
            cableLink(uid) {
                return `cables/${uid}/profile`
            },
            viewCableDetails(uid) {
                const cableLink = this.cableLink(uid)
                this.$router.push({path: cableLink})
            }
        }
    }
</script>
