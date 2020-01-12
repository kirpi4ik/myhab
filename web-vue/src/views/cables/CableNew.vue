<template>
    <div>
        <CRow>
            <CCol sm="6">
                <CCard>
                    <CForm>
                        <CCardHeader>
                            <strong>New cable </strong> <small>{{cable.name}}</small>
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
                                            label="Cablename"
                                            placeholder="cablename"
                                            @input="updateFieldValue($event, 'cablename')"
                                            ref="cablename"/>
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
        name: 'CableNew',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        name: 'Cables',
        data: () => {
            return {
                cable: [],
                cableToCreate: {
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
                this.cableToCreate[key] = value
            },
            save() {
                this.$apollo.mutate({
                    mutation: USER_CREATE, variables: {cable: this.cableToCreate}
                }).then(response => {
                    this.$router.push({path: "/cables/" + response.data.cableCreate.uid+ "/profile"})
                });
            }
        }
    }
</script>
