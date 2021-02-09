<template>
    <CRow>
        <CCol col="12" lg="6">
            <CCard>
                <CCardHeader>
                    <strong> {{ $t("peripheral.view.nav_title") }}<span class="field-value">{{ peripheral.name }}</span> </strong>
                    <div class="card-header-actions">
                        <a @click="navEdit" style="cursor: pointer" class="card-header-action" rel="noreferrer noopener">
                            <small class="text-muted">{{ $t("actions.edit") }}</small> |
                        </a>
                        <a @click="navConfig" style="cursor: pointer" class="card-header-action" rel="noreferrer noopener">
                            <small class="text-muted">{{ $t("actions.config") }}</small> |
                        </a>
                        <a @click="navDelete" style="cursor: pointer" class="card-header-action" rel="noreferrer noopener">
                            <small class="text-muted">{{ $t("actions.delete") }}</small>
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
                                            <CBadge color="success" v-for="(badge) in item.value" style="margin-right: 2px" v-bind:key="badge.uid">
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
        <CModal title="$t('modal.delete.title')" color="danger" :show.sync="deleteConfirmShow">
            {{ $t("modal.delete.confirmation_msg") }}<strong>{{peripheral.name}}</strong> ?
            <template #footer>
                <CButton @click="removePeripheral" color="danger">{{ $t("actions.delete") }}</CButton>
                <CButton @click="deleteConfirmShow = false" color="success">{{ $t("actions.cancel") }}</CButton>
            </template>
        </CModal>
    </CRow>
</template>

<script>
    import {PERIPHERAL_GET_BY_ID_CHILDS, PERIPHERAL_DELETE} from "../../graphql/queries";
    import i18n from './../../i18n'

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
                },
                deleteConfirmShow: false
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
                    variables: {id: this.$route.params.id},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    peripheral = response.data.devicePeripheral;
                    const peripheralDetailsToMap = peripheral ? Object.entries(peripheral) : [['id', 'Not found']];
                    this.peripheralDetails = peripheralDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly);
                    if (peripheral.category != null) {
                        this.peripheralDetails.push({key: i18n.t('peripheral.fields.category'), value: peripheral.category.name});
                    }

                    function joinArray(array) {
                        let label = '';
                        array.forEach(function (item, index) {
                            label += '<b>' + item.name + '</b>'
                        });
                        return label;
                    }

                    this.peripheralDetails.push({key: i18n.t('peripheral.fields.connectedTo'), value: peripheral.connectedTo});
                    this.peripheralDetails.push({key: i18n.t('peripheral.fields.zones'), value: peripheral.zones});

                    this.peripheral = peripheral;
                });
                this.loading = false
            },
            navEdit(item, index) {
                this.$router.push({path: "/peripherals/" + this.$route.params.id + "/edit"})
            },
            navConfig(item, index) {
                this.$router.push({path: "/peripherals/" + this.peripheral.id + "/configurations"})
            },
            navDelete(item, index) {
                this.deleteConfirmShow = true;
            },
            removePeripheral() {
                this.$apollo.mutate({
                    mutation: PERIPHERAL_DELETE, variables: {id: this.peripheral.id}
                }).then(response => {
                    this.deleteConfirmShow = false;
                    this.$router.push({path: "/peripherals/"})
                });
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