<template>
    <CRow>
        <CCol col="12" lg="6">
            <CCard>
                <CCardHeader>
                    <strong>Detalii periferic : <span class="field-value">{{ peripheral.name }}</span> </strong>
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
                                    :items="peripheralDetails"
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
    import {PERIPHERAL_GET_BY_ID_CHILDS} from "../../graphql/zones";


    export default {
        name: 'Peripheral',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        data: () => {
            return {
                peripheralDetails: [],
                roles: [],
                readonly: ["id", "__typename", "uid", "name"],
                peripheral: {}
            }
        },
        mounted() {
            this.loadPeripheral();
        },
        watch: {
            '$root.componentKey': 'loadPeripheral'
        },
        methods: {
            loadPeripheral() {
                this.loading = true
                let peripheral = {};
                let removeReadonly = function (keyMap) {
                    return !this.readonly.includes(keyMap.key)
                }.bind(this);

                this.$apollo.query({
                    query: USER_GET_BY_ID_WITH_ROLES,
                    variables: {uid: this.$route.params.id},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    peripheral = response.data.peripheralByUid;
                    const peripheralDetailsToMap = peripheral ? Object.entries(peripheral) : [['id', 'Not found']]
                    this.peripheralDetails = peripheralDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly)
                    this.peripheral = peripheral

                    this.roles = response.data.roleList.map(function (role, index) {
                        let found = response.data.peripheralRolesForPeripheral.filter(function (hasRole) {
                            return role.id == hasRole.roleId
                        });
                        return {id: role.id, authority: role.authority, checked: found.length > 0};
                    });
                });
                this.loading = false
            },
            rowClicked(item, index) {
                this.$router.push({path: "/peripherals/" + this.$route.params.id + "/edit"})
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