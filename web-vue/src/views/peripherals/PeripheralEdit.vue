<template>
    <div>
        <CRow>
            <CCol sm="6">
                <CCard>
                    <CForm>
                        <CCardHeader>
                            <strong>{{ $t("peripheral.edit.nav_title") }} </strong> <small>{{peripheral.name}}</small>
                            <div class="card-header-actions">
                                <a style="cursor: pointer" class="card-header-action" rel="noreferrer noopener"
                                   @click="$router.go(-1)">
                                    <small class="text-muted">{{ $t("actions.cancel") }}</small>
                                </a>
                            </div>
                        </CCardHeader>
                        <CCardBody>
                            <CRow v-for="(peripheralDetail, index) in peripheralDetails" :key="`detail-${index}`">
                                <CCol sm="12">
                                    <CInputCheckbox
                                            v-if="peripheralDetail.key.endsWith('ed')"
                                            :key="peripheralDetail.key"
                                            :label="peripheralDetail.key.charAt(0).toUpperCase()+peripheralDetail.key.slice(1)"
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
                                {{ $t("peripheral.fields.category") }}
                                <multiselect
                                        v-model="categories.selected"
                                        :options="categories.options"
                                        track-by="name"
                                        label="name">
                                </multiselect>
                            </CRow>
                            <CRow style="margin-top: 3px">
                                {{ $t("peripheral.fields.connectedTo") }}
                                <multiselect
                                        v-model="connectedTo.selected"
                                        :options="connectedTo.options"
                                        track-by="name"
                                        label="name"
                                        multiple>
                                    <template slot="option" slot-scope="props">
                                        <div>
                                            <span>[{{props.option.id}}]{{ props.option.name }} - [{{props.option.internalRef}}] - {{ props.option.device.name }}[{{props.option.device.code}}]</span>
                                        </div>
                                    </template>
                                </multiselect>
                            </CRow>
                            <CRow style="margin-top: 3px">
                                {{ $t("peripheral.fields.zones") }}
                                <multiselect
                                        v-model="zones.selected"
                                        :options="zones.options"
                                        track-by="name"
                                        label="name"
                                        multiple>
                                    <template slot="option" slot-scope="props">
                                        <div>
                                            <span>{{ props.option.name }} - {{ props.option.description }}</span>
                                        </div>
                                    </template>
                                </multiselect>
                            </CRow>
                        </CCardBody>
                        <CCardFooter>
                            <CButton type="submit" size="sm" color="primary" @click="save">
                                <CIcon name="cil-check"/>
                                {{ $t("actions.save") }}
                            </CButton>
                            <CButton type="reset" size="sm" color="danger" @click="$router.go(-1)">
                                <CIcon name="cil-ban"/>
                                {{ $t("actions.cancel") }}
                            </CButton>
                        </CCardFooter>
                    </CForm>
                </CCard>
            </CCol>
        </CRow>
    </div>
</template>


<script>
    import {PERIPHERAL_GET_BY_ID_CHILDS, PERIPHERAL_VALUE_UPDATE} from "../../graphql/queries";
    import Multiselect from 'vue-multiselect'

    export default {
        name: 'PeripheralEdit',
        components: {
            'multiselect': Multiselect
        },
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
            this.loadPeripheralByUidFromGet();
        },
        methods: {
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
                //we don;t need anymore device field to pass
                delete this.peripheralToUpdate.connectedTo.forEach(function (item, index) {
                    delete item["device"]
                });
                this.$apollo.mutate({
                    mutation: PERIPHERAL_VALUE_UPDATE, variables: {id: this.peripheral.id, devicePeripheralUpdate: this.peripheralToUpdate}
                }).then(response => {
                    this.$router.push({path: "/peripherals/" + this.$route.params.id + "/view"})
                });
            },

            loadPeripheralByUidFromGet() {
                let removeReadonly = function (keyMap) {
                    return !this.readonly.includes(keyMap.key)
                }.bind(this);

                function cleanTypes() {

                }

                this.$apollo.query({
                    query: PERIPHERAL_GET_BY_ID_CHILDS,
                    variables: {id: this.$route.params.id},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.peripheral = response.data.devicePeripheral;
                    const peripheralDetailToMap = this.peripheral ? Object.entries(this.peripheral) : [['id', 'Not found']];
                    this.peripheralDetails = peripheralDetailToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly);
                    let peripheral = {
                        "id": this.peripheral.id
                    };
                    let cleanup = function (item, index) {
                        delete item["__typename"]
                    };
                    let setParent = function (item, index) {
                        item["peripherals"] = [peripheral]
                    };

                    cleanup(response.data.devicePeripheral.category);

                    response.data.devicePeripheral.zones.forEach(cleanup);
                    // response.data.devicePeripheral.zones.forEach(setParent);
                    response.data.devicePeripheral.connectedTo.forEach(cleanup);
                    // response.data.devicePeripheral.connectedTo.forEach(setParent);
                    response.data.peripheralCategoryList.forEach(setParent);
                    response.data.peripheralCategoryList.forEach(cleanup);
                    response.data.zoneList.forEach(cleanup);
                    response.data.zoneList.forEach(setParent);
                    response.data.devicePortList.forEach(cleanup);
                    // response.data.devicePortList.forEach(setParent);

                    this.categories.options = response.data.peripheralCategoryList;
                    this.categories.selected = response.data.devicePeripheral.category;
                    this.zones.options = response.data.zoneList;
                    this.zones.selected = response.data.devicePeripheral.zones;
                    this.connectedTo.options = response.data.devicePortList;
                    this.connectedTo.selected = response.data.devicePeripheral.connectedTo;
                });
            }
        }
    }
</script>
<style src="vue-multiselect/dist/vue-multiselect.min.css"></style>

