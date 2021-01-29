<template>
    <div>
        <CRow>
            <CCol sm="6">
                <CCard>
                    <CForm>
                        <CCardHeader>
                            <strong>New peripheral </strong> <small>{{peripheral.name}}</small>
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
                                            :label="peripheralDetail.key.charAt(0).toUpperCase()+peripheralDetail.key.slice(1)"
                                            :placeholder="peripheralDetail.key"
                                            :value="peripheralDetail.value"
                                            @input="updateFieldValue($event, peripheralDetail.key)"
                                            :ref="peripheralDetail.key"/>
                                </CCol>
                            </CRow>
                            <CRow>
                                Categorie
                                <multiselect
                                        v-model="categories.selected"
                                        :options="categories.options"
                                        track-by="name"
                                        label="name">
                                </multiselect>
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
    import {PERIPHERAL_CREATE, PERIPHERAL_VALUE_UPDATE, PERIPHERAL_META_GET} from "../../graphql/zones";

    import Multiselect from 'vue-multiselect'

    export default {
        name: 'PeripheralNew',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        components: {
            'multiselect': Multiselect
        },
        data: () => {
            return {
                peripheralDetails: [
                    {key: "name", value: ""},
                    {key: "description", value: ""},
                    {key: "code", value: ""},
                    {key: "codeOld", value: ""},
                    {key: "model", value: ""},
                    {key: "maxAmp", value: ""}
                ],
                peripheral: [],
                roles: [],
                peripheralToUpdate: {},
                readonly: ["id", "__typename", "uid", "category", "connectedTo", "zones"],
                zones: {
                    selected: null,
                    options: []
                },
                categories: {
                    selected: null,
                    options: []
                },
                connectedTo: {
                    selected: null,
                    options: []
                }
            }
        },
        created() {
            this.init();
        },
        methods: {
            init() {
                this.$apollo.query({
                    query: PERIPHERAL_META_GET,
                    variables: {},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    let cleanup = function (item, index) {
                        delete item["__typename"]
                    };

                    response.data.peripheralCategoryList.forEach(cleanup);
                    response.data.zoneList.forEach(cleanup);
                    response.data.devicePortList.forEach(cleanup);

                    this.categories.options = response.data.peripheralCategoryList;
                    this.zones.options = response.data.zoneList;
                    this.connectedTo.options = response.data.devicePortList;
                });
            },
            check(value, key) {
                this.peripheralToUpdate[key] = value

            },
            updateFieldValue(value, key) {
                this.peripheralToUpdate[key] = value
            },
            save() {
                this.peripheralToUpdate.category = this.categories.selected;
                this.peripheralToUpdate.zones = this.zones.selected;
                this.peripheralToUpdate.connectedTo = this.connectedTo.selected;
                this.$apollo.mutate({
                    mutation: PERIPHERAL_CREATE, variables: {devicePeripheral: this.peripheralToUpdate}
                }).then(response => {
                    this.$router.push({path: "/peripherals/" + response.data.devicePeripheralCreate.uid + "/view"})
                });
            },
        }
    }
</script>
<style src="vue-multiselect/dist/vue-multiselect.min.css"></style>
