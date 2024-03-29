<template>
    <div>
        <CRow>
            <CCol sm="6">
                <CCard>
                    <CForm>
                        <CCardHeader>
                            <strong>Edit user </strong> <small>{{user.name}}</small>
                            <div class="card-header-actions">
                                <a style="cursor: pointer" class="card-header-action" rel="noreferrer noopener"
                                   @click="$router.go(-1)">
                                    <small class="text-muted">Cancel</small>
                                </a>
                            </div>
                        </CCardHeader>
                        <CCardBody>
                            <CRow v-for="(userDetail, index) in userDetails" :key="`detail-${index}`">
                                <CCol sm="12">
                                    <CInputCheckbox
                                            v-if="userDetail.key.endsWith('ed')"
                                            :key="userDetail.key"
                                            :label="userDetail.key.charAt(0).toUpperCase()+userDetail.key.slice(1)"
                                            :value="userDetail.value"
                                            :checked="userDetail.value"
                                            @update:checked="check($event, userDetail.key)"
                                            :inline="true"
                                            :ref="userDetail.key"
                                    />
                                    <CInput v-if="!userDetail.key.endsWith('ed')"
                                            :label="userDetail.key.charAt(0).toUpperCase()+userDetail.key.slice(1)"
                                            :placeholder="userDetail.key"
                                            :value="userDetail.value"
                                            @input="updateFieldValue($event, userDetail.key)"
                                            :ref="userDetail.key"/>
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
                                            @update:checked="updateRoleValue($event, role.id)"
                                            :ref="role.authority"
                                    />
                                </CCol>
                            </CRow>
                        </CCardBody>
                        <CCardFooter>
                            <CButton type="submit" size="sm" color="primary" @click="save">
                                <CIcon name="cil-check-circle"/>
                                Save
                            </CButton>
                            <CButton type="reset" size="sm" color="danger" @click="$router.go(-1)">
                                <CIcon name="cil-ban"/>
                                Cancel
                            </CButton>
                        </CCardFooter>
                    </CForm>
                </CCard>
            </CCol>
        </CRow>
    </div>
</template>


<script>
    import {USER_VALUE_UPDATE, USER_GET_BY_ID, ROLES_GET_FOR_USER, ROLES_SAVE} from "../../graphql/queries";

    export default {
        name: 'UserEdit',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        data: () => {
            return {
                userDetails: [],
                user: [],
                roles: [],
                userToUpdate: {},
                readonly: ["id", "__typename", "uid", "name"]
            }
        },
        created() {
            this.loadUserByUidFromGet();
        },
        methods: {
            check(value, key) {
                this.userToUpdate[key] = value

            },
            updateFieldValue(value, key) {
                this.userToUpdate[key] = value
            },
            updateRoleValue(value, key) {
                this.roles.forEach(function (role, index) {
                    if (role.id == key) {
                        role.checked = value;
                    }
                });

            },
            save() {
                this.$apollo.mutate({
                    mutation: USER_VALUE_UPDATE, variables: {id: this.user.id, user: this.userToUpdate}
                }).then(response => {
                    let roles = {
                        "userId": this.user.id,
                        "userRoles": this.roles.filter(function (role) {
                            return role.checked
                        }.bind(this)).map(function (role, index) {
                            return {"userId": this.user.id, "roleId": role.id};
                        }.bind(this))
                    };
                    this.$apollo.mutate({
                        mutation: ROLES_SAVE, variables: {input: roles}
                    }).then(response => {
                        this.$router.push({path: "/users/" + this.$route.params.idPrimary + "/profile"})
                    });
                });
            },
            loadUserByUidFromGet() {
                let removeReadonly = function (keyMap) {
                    return !this.readonly.includes(keyMap.key)
                }.bind(this);


                let loadRoles = function () {
                    this.$apollo.query({
                        query: ROLES_GET_FOR_USER,
                        variables: {id: this.user.id},
                        fetchPolicy: 'network-only'
                    }).then(response => {
                        this.roles = response.data.roleList.map(function (role, index) {
                            let found = response.data.userRolesForUser.filter(function (hasRole) {
                                return role.id == hasRole.roleId
                            });
                            return {id: role.id, authority: role.authority, checked: found.length > 0};
                        });
                    });
                }.bind(this);

                this.$apollo.query({
                    query: USER_GET_BY_ID,
                    variables: {id: this.$route.params.idPrimary},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.user = response.data.userById;
                    const userDetailsToMap = this.user ? Object.entries(this.user) : [['id', 'Not found']]
                    this.userDetails = userDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly)
                    loadRoles()
                });
            }
        }
    }
</script>
