<template>
    <CRow>
        <CCol col="12" lg="6">
            <CCard>
                <CCardHeader>
                    <strong> {{ $t("peripheral.view.nav_title") }}<span class="field-value">{{ peripheral.name }}</span> </strong>
                    <div class="card-header-actions" @click="rowClicked">
                        <a style="cursor: pointer" class="card-header-action" rel="noreferrer noopener">
                            <small class="text-muted">{{ $t("actions.edit") }}</small>
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
                            >
                                <template #key="{item}">
                                    <td>
                                        <div>
                                            <b>{{item.key}}</b>
                                        </div>

                                    </td>
                                </template>
                                <template #value="{item}">
                                    <td>
                                        <div v-if="!Array.isArray(item.value)">
                                            {{item.value}}
                                        </div>
                                        <div v-if="Array.isArray(item.value)">
                                            <CBadge color="success" v-for="(badge) in item.value" style="margin-right: 2px" v-bind:key="badge">
                                                {{badge.name}}
                                            </CBadge>
                                        </div>
                                    </td>
                                </template>
                            </CDataTable>
                        </CCol>
                    </CRow>
                </CCardBody>
                <CCardFooter>
                    <CButton color="primary" @click="goBack">{{ $t("actions.nav.back") }}</CButton>
                </CCardFooter>
            </CCard>
        </CCol>
    </CRow>
</template>

<script>
    import {PERIPHERAL_GET_BY_ID_CHILDS} from "../../graphql/zones";

    export default {
        name: 'PeripheralView',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        data: () => {
            return {
                peripheralDetails: [],
                roles: [],
                readonly: ["id", "__typename", "uid", "category", "connectedTo", "zones"],
                peripheral: {},
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
                    query: PERIPHERAL_GET_BY_ID_CHILDS,
                    variables: {uid: this.$route.params.id},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    peripheral = response.data.devicePeripheralByUid;
                    const peripheralDetailsToMap = peripheral ? Object.entries(peripheral) : [['id', 'Not found']];
                    this.peripheralDetails = peripheralDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly);
                    this.peripheralDetails.push({key: "category", value: peripheral.category.name});

                    function joinArray(array) {
                        let label = '';
                        array.forEach(function (item, index) {
                            label += '<b>' + item.name + '</b>'
                        });
                        return label;
                    }

                    this.peripheralDetails.push({key: "Locatii", value: peripheral.zones});
                    this.peripheralDetails.push({key: "Port", value: peripheral.connectedTo});

                    this.peripheral = peripheral;
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
<style src="vue-multiselect/dist/vue-multiselect.min.css"></style>
<style>
    .field-value {
        color: #abb3c0;
    }
</style>