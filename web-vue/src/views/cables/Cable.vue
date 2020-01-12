<template>
    <CRow>
        <CCol col="12" lg="6">
            <CCard>
                <CCardHeader>
                    <strong>Cable details : <span class="field-value">{{ cable.name }}</span> </strong>
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
                                    :items="cableDetails"
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
    import {USER_GET_BY_ID_WITH_ROLES} from "../../graphql/zones";


    export default {
        name: 'Cable',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        data: () => {
            return {
                cableDetails: [],
                roles: [],
                readonly: ["id", "__typename", "uid", "name"],
                cable: {}
            }
        },
        mounted() {
            this.loadCable();
        },
        watch: {
            '$root.componentKey': 'loadCable'
        },
        methods: {
            loadCable() {
                this.loading = true
                let cable = {};
                let removeReadonly = function (keyMap) {
                    return !this.readonly.includes(keyMap.key)
                }.bind(this);

                this.$apollo.query({
                    query: USER_GET_BY_ID_WITH_ROLES,
                    variables: {uid: this.$route.params.id},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    cable = response.data.cableByUid;
                    const cableDetailsToMap = cable ? Object.entries(cable) : [['id', 'Not found']]
                    this.cableDetails = cableDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly)
                    this.cable = cable

                    this.roles = response.data.roleList.map(function (role, index) {
                        let found = response.data.cableRolesForCable.filter(function (hasRole) {
                            return role.id == hasRole.roleId
                        });
                        return {id: role.id, authority: role.authority, checked: found.length > 0};
                    });
                });
                this.loading = false
            },
            rowClicked(item, index) {
                this.$router.push({path: "/cables/" + this.$route.params.id + "/edit"})
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