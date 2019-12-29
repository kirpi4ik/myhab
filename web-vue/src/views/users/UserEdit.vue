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
                                            :label="userDetail.key"
                                            :value="userDetail.value"
                                            :checked="userDetail.value"
                                            @update:checked="check($event, userDetail.key)"
                                            :inline="true"
                                            :ref="userDetail.key"
                                    />
                                    <CInput v-if="!userDetail.key.endsWith('ed')"
                                            :label="userDetail.key"
                                            :placeholder="userDetail.key"
                                            :value="userDetail.value"
                                            @input="updateFieldValue($event, userDetail.key)"
                                            :ref="userDetail.key"/>
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
    import {UPDATE_USER_VALUE, USER_GET_BY_ID} from "../../graphql/zones";

    export default {
        name: 'User',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        name: 'Users',
        data: () => {
            return {
                userDetails: [],
                user: [],
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
            save() {
                this.$apollo.mutate({
                    mutation: UPDATE_USER_VALUE, variables: {id: this.user.id, user: this.userToUpdate}
                }).then(response => {
                    this.$router.push({path: "/users/" + this.$route.params.id + "/profile"})
                });
            },
            loadUserByUidFromGet() {
                let removeReadonly = function (keyMap) {
                    return !this.readonly.includes(keyMap.key)
                }.bind(this);

                this.$apollo.query({
                    query: USER_GET_BY_ID,
                    variables: {uid: this.$route.params.id},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.user = response.data.userByUid;
                    const userDetailsToMap = this.user ? Object.entries(this.user) : [['id', 'Not found']]
                    this.userDetails = userDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly)
                });
            }
        }
    }
</script>
