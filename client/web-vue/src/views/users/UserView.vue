<template>
    <CRow>
        <CCol col="12" lg="6">
            <CCard>
                <CCardHeader>
                    <strong>Detalii utilizator : <span class="field-value">{{ user.name }}</span> </strong>
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
                                    :items="userDetails"
                                    :fields="$options.fields"
                            />
                        </CCol>
                    </CRow>
                    <CRow v-for="(role, index) in roles" :key="`role-${index}`">
                        <CCol sm="12">
                            <CInputCheckbox
                                    :key="role.authority"
                                    :label="role.authority"
                                    :value="role.checked"
                                    :checked="role.checked"
                                    :inline="true"
                                    :ref="role.authority"
                                    disabled="true"
                            />
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
    import {USER_GET_BY_ID_WITH_ROLES} from "@/graphql/queries";


    export default {
        name: 'UserView',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        data: () => {
            return {
                userDetails: [],
                roles: [],
                readonly: ["id", "__typename", "uid", "name"],
                user: {}
            }
        },
        mounted() {
            this.loadUser();
        },
        watch: {
            '$root.componentKey': 'loadUser'
        },
        methods: {
            loadUser() {
                this.loading = true;
                let user = {};
                let removeReadonly = function (keyMap) {
                    return !this.readonly.includes(keyMap.key)
                }.bind(this);

                this.$apollo.query({
                    query: USER_GET_BY_ID_WITH_ROLES,
                    variables: {id: this.$route.params.idPrimary},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    user = response.data.userById;
                    const userDetailsToMap = user ? Object.entries(user) : [['id', 'Not found']]
                    this.userDetails = userDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly)
                    this.user = user

                    this.roles = response.data.roleList.map(function (role, index) {
                        let found = response.data.userRolesForUser.filter(function (hasRole) {
                            return role.id == hasRole.roleId
                        });
                        return {id: role.id, authority: role.authority, checked: found.length > 0};
                    });
                });
                this.loading = false
            },
            rowClicked(item, index) {
                this.$router.push({path: "/users/" + this.$route.params.idPrimary + "/edit"})
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