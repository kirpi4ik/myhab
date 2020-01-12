<template>
    <div>
        <CRow>
            <CCol sm="6">
                <CCard>
                    <CForm>
                        <CCardHeader>
                            <strong>Edit device </strong> <small>{{device.name}}</small>
                            <div class="card-header-actions">
                                <a style="cursor: pointer" class="card-header-action" rel="noreferrer noopener"
                                   @click="$router.go(-1)">
                                    <small class="text-muted">Cancel</small>
                                </a>
                            </div>
                        </CCardHeader>
                        <CCardBody>
                            <CRow v-for="(deviceDetail, index) in deviceDetails" :key="`detail-${index}`">
                                <CCol sm="12">
                                    <CInputCheckbox
                                            v-if="deviceDetail.key.endsWith('ed')"
                                            :key="deviceDetail.key"
                                            :label="deviceDetail.key"
                                            :value="deviceDetail.value"
                                            :checked="deviceDetail.value"
                                            @update:checked="check($event, deviceDetail.key)"
                                            :inline="true"
                                            :ref="deviceDetail.key"
                                    />
                                    <CInput v-if="!deviceDetail.key.endsWith('ed')"
                                            :label="deviceDetail.key"
                                            :placeholder="deviceDetail.key"
                                            :value="deviceDetail.value"
                                            @input="updateFieldValue($event, deviceDetail.key)"
                                            :ref="deviceDetail.key"/>
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
        name: 'DeviceEdit',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        data: () => {
            return {
                deviceDetails: [],
                device: [],
                roles: [],
                deviceToUpdate: {},
                readonly: ["id", "__typename", "uid", "name"]
            }
        },
        created() {
            this.loadDeviceByUidFromGet();
        },
        methods: {
            check(value, key) {
                this.deviceToUpdate[key] = value

            },
            updateFieldValue(value, key) {
                this.deviceToUpdate[key] = value
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
                    mutation: USER_VALUE_UPDATE, variables: {id: this.device.id, device: this.deviceToUpdate}
                }).then(response => {
                    let roles = {
                        "deviceUid": this.device.uid,
                        "deviceRoles": this.roles.filter(function (role) {
                            debugger
                            return role.checked
                        }.bind(this)).map(function (role, index) {
                            return {"deviceId": this.device.id, "roleId": role.id};
                        }.bind(this))
                    };
                    this.$apollo.mutate({
                        mutation: ROLES_SAVE, variables: {input: roles}
                    }).then(response => {
                        this.$router.push({path: "/devices/" + this.$route.params.id + "/profile"})
                    });
                });
            },
            loadDeviceByUidFromGet() {
                let removeReadonly = function (keyMap) {
                    return !this.readonly.includes(keyMap.key)
                }.bind(this);


                let loadRoles = function () {
                    this.$apollo.query({
                        query: ROLES_GET_FOR_USER,
                        variables: {uid: this.device.uid},
                        fetchPolicy: 'network-only'
                    }).then(response => {
                        this.roles = response.data.roleList.map(function (role, index) {
                            let found = response.data.deviceRolesForDevice.filter(function (hasRole) {
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
                    this.device = response.data.deviceByUid;
                    const deviceDetailsToMap = this.device ? Object.entries(this.device) : [['id', 'Not found']]
                    this.deviceDetails = deviceDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly)
                    loadRoles()
                });
            }
        }
    }
</script>
