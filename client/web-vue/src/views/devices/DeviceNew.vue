<template>
    <div>
        <CRow>
            <CCol sm="6">
                <CCard>
                    <CForm>
                        <CCardHeader>
                            <strong>{{ $t("device.edit.nav_title") }} </strong> <small>{{device.name}}</small>
                            <div class="card-header-actions">
                                <a style="cursor: pointer" class="card-header-action" rel="noreferrer noopener"
                                   @click="$router.go(-1)">
                                    <small class="text-muted">{{ $t("actions.cancel") }}</small>
                                </a>
                            </div>
                        </CCardHeader>
                        <CCardBody>
                            <CRow v-for="(deviceDetail, index) in deviceDetails" :key="`detail-${index}`">
                                <CCol sm="12">
                                    <CInputCheckbox
                                            v-if="deviceDetail.key.endsWith('ed')"
                                            :key="deviceDetail.key"
                                            :label="deviceDetail.key.charAt(0).toUpperCase()+deviceDetail.key.slice(1)"
                                            :value="deviceDetail.value"
                                            :checked="deviceDetail.value"
                                            @update:checked="device[deviceDetail.key] =  $event"
                                            :inline="true"
                                            :ref="deviceDetail.key"
                                    />
                                    <CInput v-if="!deviceDetail.key.endsWith('ed')"
                                            :label="deviceDetail.key.charAt(0).toUpperCase()+deviceDetail.key.slice(1)"
                                            :placeholder="deviceDetail.key"
                                            :value="deviceDetail.value"
                                            @input="device[deviceDetail.key] =  $event"
                                            :ref="deviceDetail.key"/>
                                </CCol>
                            </CRow>
                            <CRow>
                                <b> {{ $t("device.fields.model.label") }}</b>
                                <multiselect
                                        v-model="device.model"
                                        :options="deviceModels.options">
                                </multiselect>
                            </CRow>
                            <CRow>
                                <b> {{ $t("device.fields.type.label") }}</b>
                                <multiselect
                                        v-model="device.type"
                                        :options="deviceTypes.options"
                                        track-by="name"
                                        label="name">
                                </multiselect>
                            </CRow>
                            <CRow style="margin-top: 15px">
                                <b> {{ $t("device.fields.rack.label") }}</b>
                                <multiselect
                                        v-model="device.rack"
                                        :options="deviceRacks.options"
                                        track-by="name"
                                        label="name">
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
    import {
        DEVICE_CREATE,
        DEVICE_META_GET
    } from "../../graphql/queries";
    import Multiselect from 'vue-multiselect'

    export default {
        name: 'DeviceNew',
        components: {
            'multiselect': Multiselect
        },
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        data: () => {
            return {
                deviceDetails: [],
                device: {
                    name: "",
                    code: "",
                    type: null,
                    rack: [],
                },
                deviceModels: {
                    options: ['MEGAD_2561_RTC', 'ESP8266_1', 'TMEZON_INTERCOM', 'NIBE_F1145_8_EM']
                },
                deviceTypes: {
                    options: []
                },
                deviceRacks: {
                    options: []
                },
                readonly: ["id", "__typename", "uid", "type", "networkAddress", "authAccounts", "rack", "ports"],

            }
        },
        created() {
            this.init();
        },
        methods: {
            init() {
                let removeReadonly = function (keyMap) {
                    return !this.readonly.includes(keyMap.key)
                }.bind(this);

                this.$apollo.query({
                    query: DEVICE_META_GET,
                    variables: {},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.deviceTypes.options = response.data.deviceTypeList;
                    this.deviceRacks.options = response.data.rackList;

                    const deviceDetailToMap = this.device ? Object.entries(this.device) : [['id', 'Not found']];
                    this.deviceDetails = deviceDetailToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly);
                });
            },
            save() {
                this.$apollo.mutate({
                    mutation: DEVICE_CREATE, variables: {device: this.device}
                }).then(response => {
                    this.$router.push({path: "/devices/" + response.data.deviceCreate.id + "/edit"})
                });
            }
        }
    }
</script>
<style src="vue-multiselect/dist/vue-multiselect.min.css"></style>