<template>
    <div>
        <CRow>
            <CCol sm="6">
                <CCard>
                    <CForm>
                        <CCardHeader>
                            <strong>New user </strong> <small>{{user.name}}</small>
                            <div class="card-header-actions">
                                <a style="cursor: pointer" class="card-header-action" rel="noreferrer noopener"
                                   @click="$router.go(-1)">
                                    <small class="text-muted">Cancel</small>
                                </a>
                            </div>
                        </CCardHeader>
                        <CCardBody>
                            <CRow>
                                <CCol sm="12">
                                    <CInput
                                            label="Username"
                                            placeholder="username"
                                            @input="updateFieldValue($event, 'username')"
                                            ref="username"/>
                                    <CInput
                                            label="Password"
                                            placeholder="password"
                                            @input="updateFieldValue($event, 'password')"
                                            ref="password"/>
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
    import {USER_CREATE} from "../../graphql/zones";

    export default {
        name: 'User',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        name: 'Users',
        data: () => {
            return {
                user: [],
                userToCreate: {
                    passwordExpired: true,
                    accountLocked: true,
                    accountExpired: true,
                    enabled: false
                },
                readonly: ["id", "__typename", "uid", "name"]
            }
        },
        methods: {
            updateFieldValue(value, key) {
                this.userToCreate[key] = value
            },
            save() {
                this.$apollo.mutate({
                    mutation: USER_CREATE, variables: {user: this.userToCreate}
                }).then(response => {
                    this.$router.push({path: "/users/" + response.data.userCreate.uid+ "/profile"})
                });
            }
        }
    }
</script>
