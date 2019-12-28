<template>
    <CRow>
        <CCol col="12" xl="8">
            <transition name="slide">
                <CCard>
                    <CCardHeader>
                        Users
                    </CCardHeader>
                    <CCardBody>
                        <CDataTable
                                hover
                                striped
                                :items="items"
                                :fields="fields"
                                :items-per-page="perPage"
                                @row-clicked="rowClicked"
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
                                    <CBadge :color="getBadge(data.item.status)">
                                        {{data.item.status}}
                                    </CBadge>
                                </td>
                            </template>
                        </CDataTable>
                    </CCardBody>
                </CCard>
            </transition>
        </CCol>
    </CRow>
</template>

<script>
    import {USERS_GET_ALL} from "../../graphql/zones";

    export default {
        name: 'Users',
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
            loadUsers() {
                this.$apollo.query({
                    query: USERS_GET_ALL,
                    variables: {}
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
            userLink(uid) {
                return `users/${uid}`
            },
            rowClicked(item, index) {
                const userLink = this.userLink(item.uid)
                this.$router.push({path: userLink})
            }
        }
    }
</script>
