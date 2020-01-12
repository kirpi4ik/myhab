<template>
    <div>
        <CRow>
            <CCol sm="6">
                <CCard>
                    <CForm>
                        <CCardHeader>
                            <strong>Edit peripheral </strong> <small>{{peripheral.name}}</small>
                            <div class="card-header-actions">
                                <a style="cursor: pointer" class="card-header-action" rel="noreferrer noopener"
                                   @click="$router.go(-1)">
                                    <small class="text-muted">Cancel</small>
                                </a>
                            </div>
                        </CCardHeader>
                        <CCardBody>
                            <CRow v-for="(peripheralDetail, index) in peripheralDetails" :key="`detail-${index}`">
                                <CCol sm="12">
                                    <CInputCheckbox
                                            v-if="peripheralDetail.key.endsWith('ed')"
                                            :key="peripheralDetail.key"
                                            :label="peripheralDetail.key"
                                            :value="peripheralDetail.value"
                                            :checked="peripheralDetail.value"
                                            @update:checked="check($event, peripheralDetail.key)"
                                            :inline="true"
                                            :ref="peripheralDetail.key"
                                    />
                                    <CInput v-if="!peripheralDetail.key.endsWith('ed')"
                                            :label="peripheralDetail.key"
                                            :placeholder="peripheralDetail.key"
                                            :value="peripheralDetail.value"
                                            @input="updateFieldValue($event, peripheralDetail.key)"
                                            :ref="peripheralDetail.key"/>
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
    import {USER_VALUE_UPDATE, USER_GET_BY_ID, ROLES_GET_FOR_USER, ROLES_SAVE} from "../../graphql/zones";

    export default {
        name: 'PeripheralEdit',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        data: () => {
            return {
                peripheralDetails: [],
                peripheral: [],
                roles: [],
                peripheralToUpdate: {},
                readonly: ["id", "__typename", "uid", "name"]
            }
        },
        created() {
            this.loadPeripheralByUidFromGet();
        },
        methods: {
            check(value, key) {
                this.peripheralToUpdate[key] = value

            },
            updateFieldValue(value, key) {
                this.peripheralToUpdate[key] = value
            },
            updateRoleValue(value, key) {
                debugger
                this.roles.forEach(function (role, index) {
                    if (role.id == key) {
                        role.checked = value;
                    }
                });

            },
            save() {
                this.$apollo.mutate({
                    mutation: USER_VALUE_UPDATE, variables: {id: this.peripheral.id, peripheral: this.peripheralToUpdate}
                }).then(response => {
                    let roles = {
                        "peripheralUid": this.peripheral.uid,
                        "peripheralRoles": this.roles.filter(function (role) {
                            debugger
                            return role.checked
                        }.bind(this)).map(function (role, index) {
                            return {"peripheralId": this.peripheral.id, "roleId": role.id};
                        }.bind(this))
                    };
                    this.$apollo.mutate({
                        mutation: ROLES_SAVE, variables: {input: roles}
                    }).then(response => {
                        this.$router.push({path: "/peripherals/" + this.$route.params.id + "/profile"})
                    });
                });
            },
            loadPeripheralByUidFromGet() {
                let removeReadonly = function (keyMap) {
                    return !this.readonly.includes(keyMap.key)
                }.bind(this);


                let loadRoles = function () {
                    this.$apollo.query({
                        query: ROLES_GET_FOR_USER,
                        variables: {uid: this.peripheral.uid},
                        fetchPolicy: 'network-only'
                    }).then(response => {
                        this.roles = response.data.roleList.map(function (role, index) {
                            let found = response.data.peripheralRolesForPeripheral.filter(function (hasRole) {
                                return role.id == hasRole.roleId
                            });
                            return {id: role.id, authority: role.authority, checked: found.length > 0};
                        });
                    });
                }.bind(this);

                this.$apollo.query({
                    query: USER_GET_BY_ID,
                    variables: {uid: this.$route.params.id},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.peripheral = response.data.peripheralByUid;
                    const peripheralDetailsToMap = this.peripheral ? Object.entries(this.peripheral) : [['id', 'Not found']]
                    this.peripheralDetails = peripheralDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly)
                    loadRoles()
                });
            }
        }
    }
</script>
