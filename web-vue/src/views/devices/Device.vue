<template>
    <CRow>
        <CCol col="12" lg="6">
            <CCard>
                <CCardHeader>
                    <strong>Device details : <span class="field-value">{{ device.uid }}</span> </strong>
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
                                    :items="deviceDetails"
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
    import {DEVICE_GET_BY_ID_CHILDS} from "../../graphql/zones";


    export default {
        name: 'Device',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        data: () => {
            return {
                deviceDetails: [],
                roles: [],
                readonly: ["id", "__typename", "uid"],
                device: {}
            }
        },
        mounted() {
            this.loadDevice();
        },
        watch: {
            '$root.componentKey': 'loadDevice'
        },
        methods: {
            loadDevice() {
                this.loading = true;
                let device = {};
                let removeReadonly = function (keyMap) {
                    return !this.readonly.includes(keyMap.key)
                }.bind(this);

                this.$apollo.query({
                    query: DEVICE_GET_BY_ID_CHILDS,
                    variables: {uid: this.$route.params.id},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    device = response.data.deviceByUid;
                    const deviceDetailsToMap = device ? Object.entries(device) : [['id', 'Not found']]
                    this.deviceDetails = deviceDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly);
                    this.device = device;

                    this.roles = response.data.roleList.map(function (role, index) {
                        let found = response.data.deviceRolesForDevice.filter(function (hasRole) {
                            return role.id == hasRole.roleId
                        });
                        return {id: role.id, authority: role.authority, checked: found.length > 0};
                    });
                });
                this.loading = false
            },
            rowClicked(item, index) {
                this.$router.push({path: "/devices/" + this.$route.params.id + "/edit"})
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