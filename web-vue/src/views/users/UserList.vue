<template>
    <CRow>
        <CCol col="12" xl="8">
            <transition name="slide">
                <CCard>
                    <CCardHeader>
                        Utilizatori
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
                            <template #username="data">
                                <td>
                                    <strong>{{data.item.username}}</strong>
                                </td>
                            </template>

                            <template #status="data">
                                <td>
                                    <CButton color="success" @click="viewUserDetails(data.item.id)">View</CButton>
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
                    <CModal title="Delete user" color="danger" :show.sync="deleteConfirmShow">
                        Do you want delete : <strong>{{selectedItem.username}}</strong> ?
                        <template #footer>
                            <CButton @click="removeUser" color="danger">Delete</CButton>
                            <CButton @click="deleteConfirmShow = false" color="success">Cancel</CButton>
                        </template>
                    </CModal>
                </CCard>
            </transition>
        </CCol>
    </CRow>
</template>

<script>
    import {USERS_GET_ALL, USER_DELETE} from "../../graphql/queries";

    export default {
        name: 'UserList',
        data: () => {
            return {
                items: [],
                fields: [
                    {key: 'username', label: 'Name'},
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
            this.loadUsers();
        },
        watch: {
            '$route.fullPath': 'loadUsers'
        },
        methods: {
            modalCheck(item) {
                this.deleteConfirmShow = true;
                this.selectedItem = item
            },
            removeUser() {
                this.$apollo.mutate({
                    mutation: USER_DELETE, variables: {id: this.selectedItem.id}
                }).then(response => {
                    this.deleteConfirmShow = false;
                    this.loadUsers();
                });
            },
            addNew() {
                this.$router.push({path: "/users/create"})
            },
            loadUsers() {
                this.$apollo.query({
                    query: USERS_GET_ALL,
                    variables: {},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.items = response.data.userList;
                });
            },
            getBadge(status) {
                return status === 'Active' ? 'success'
                    : status === 'Inactive' ? 'secondary'
                        : status === 'Pending' ? 'warning'
                            : status === 'Banned' ? 'danger' : 'primary'
            },
            userLink(id) {
                return `users/${id}/profile`
            },
            viewUserDetails(id) {
                const userLink = this.userLink(id)
                this.$router.push({path: userLink})
            }
        }
    }
</script>
