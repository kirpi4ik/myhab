<template>
    <CRow>
        <CCol sm="12">
            <CCard>
                <CForm>
                    <CCardHeader>
                        <strong>Port </strong> <small>{{port.name}}</small>
                        <div class="card-header-actions">
                            <a @click="navEdit" style="cursor: pointer" class="card-header-action" rel="noreferrer noopener">
                                <small class="text-muted">{{ $t("actions.edit") }}</small> |
                            </a>
                            <a @click="navConfig" style="cursor: pointer" class="card-header-action" rel="noreferrer noopener">
                                <small class="text-muted">{{ $t("actions.config") }}</small> |
                            </a>
                            <a @click="$router.push({path: '/devices/' + $route.params.idPrimary + '/view'})" style="cursor: pointer" class="card-header-action" rel="noreferrer noopener">
                                <small class="text-muted">{{ $t("actions.cancel") }}</small>
                            </a>
                        </div>
                    </CCardHeader>
                    <CCardBody>
                        <CRow>
                            <CCol>
                                <CRow>
                                    <CCol>
                                        <CDataTable
                                                striped
                                                hover
                                                small
                                                fixed
                                                :items="portDetails"
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
                                                        <CIcon v-if="item.value === true" name="cil-chevron-bottom"/>
                                                        <CIcon v-if="item.value === false" name="cil-square"/>
                                                        <div v-if="item.value != true && item.value != false">
                                                            {{item.value}}
                                                        </div>
                                                    </div>
                                                    <div v-if="Array.isArray(item.value)">
                                                        <CBadge color="success" v-for="(badge) in item.value" style="margin-right: 2px" v-bind:key="badge.id">
                                                            {{badge.name}}
                                                        </CBadge>
                                                    </div>
                                                </td>
                                            </template>
                                        </CDataTable>
                                    </CCol>
                                </CRow>
                            </CCol>
                        </CRow>
                        <CRow>
                            <CCol sm="1">
                                {{ $t("cables.list.nav_title") }}
                            </CCol>
                            <CCol>
                                <CDataTable
                                        hover
                                        bordered
                                        striped
                                        :items="port.cables"
                                        :fields="cableTmpl"
                                        :items-per-page="5"
                                        index-column
                                        sorter
                                        clickable-rows
                                        @row-clicked="$router.push({path: '/cables/' +  $event.id + '/view'})"
                                >
                                    <template #name="data">
                                        <td>
                                            <strong>{{data.item.code}}</strong>
                                        </td>
                                    </template>
                                    <template #actions="data">
                                        <td>
                                            <a @click="$router.push({path: '/cables/' + data.item.id + '/edit'})" style="padding-right: 1em">
                                                <font-awesome-icon icon="edit" size="1x"/>
                                            </a>
                                            <a @click="modalCheck(data.item)">
                                                <font-awesome-icon icon="ban" size="1x"/>
                                            </a>
                                        </td>
                                    </template>
                                </CDataTable>
                            </CCol>
                        </CRow>
                        <CRow>
                            <CCol sm="1">
                                {{ $t("peripheral.list.nav_title") }}
                            </CCol>
                            <CCol>
                                <CDataTable
                                        hover
                                        bordered
                                        striped
                                        :items="port.peripherals"
                                        :fields="cableTmpl"
                                        :items-per-page="5"
                                        index-column
                                        sorter
                                        clickable-rows
                                        @row-clicked="$router.push({path: '/peripherals/' +  $event.id + '/view'})"

                                >
                                    <template #name="data">
                                        <td>
                                            <strong>{{data.item.name}}</strong>
                                        </td>
                                    </template>
                                    <template #actions="data">
                                        <td>
                                            <a @click="$router.push({path: '/peripherals/' + data.item.id + '/edit'})" style="padding-right: 1em">
                                                <font-awesome-icon icon="edit" size="1x"/>
                                            </a>
                                            <a @click="modalCheck(data.item)">
                                                <font-awesome-icon icon="ban" size="1x"/>
                                            </a>
                                        </td>
                                    </template>
                                </CDataTable>
                            </CCol>
                        </CRow>
                    </CCardBody>
                    <CCardFooter>
                        <CButton type="reset" size="sm" color="danger" @click="navDelete" style="margin-left: 5px">
                            <CIcon name="cil-ban"/>
                            {{ $t("actions.delete") }}
                        </CButton>
                    </CCardFooter>
                </CForm>
            </CCard>
        </CCol>
    </CRow>
</template>


<script>
    import {PORT_GET_BY_ID} from "../../graphql/queries";
    import i18n from './../../i18n'

    export default {
        name: 'PortEditor',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        data: () => {
            return {
                portDetails: [],
                port: [],
                readonly: ["id", "__typename", "uid", "device", "cables", "peripherals"],
                deleteConfirmShow: false,
                cableTmpl: [
                    {key: 'id', label: 'ID'},
                    {key: 'name', label: 'Name'},
                    {
                        key: 'actions', label: i18n.t('table.header.actions'),
                        sorter: false,
                        filter: false
                    }

                ]
            }
        },
        created() {
            this.init();
        },
        methods: {
            init() {
                let port = {};

                this.$apollo.query({
                    query: PORT_GET_BY_ID,
                    variables: {id: this.$route.params.id},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    let removeReadonly = function (keyMap) {
                        return !this.readonly.includes(keyMap.key)
                    }.bind(this);

                    this.port = response.data.devicePort;
                    const portDetailsToMap = port ? Object.entries(this.port) : [['id', 'Not found']];
                    this.portDetails = portDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly);
                });
            },
            check(value, key) {
                this.portToUpdate[key] = value

            },
            updateFieldValue(value, key) {
                this.portToUpdate[key] = value
            },
            save() {
                this.portToUpdate.category = this.categories.selected;
                this.portToUpdate.zones = this.zones.selected;
                this.portToUpdate.connectedTo = this.connectedTo.selected;
                this.$apollo.mutate({
                    mutation: PERIPHERAL_CREATE, variables: {devicePeripheral: this.portToUpdate}
                }).then(response => {
                    this.$router.push({path: "/devices/" + this.$route.params.idPrimary + "/ports/" + response.data.devicePort.id + "/view"})
                });
            },
            navEdit(item, index) {
                this.$router.push({path: "/devices/" + this.$route.params.idPrimary + "/ports/" + this.port.id + "/edit"})
            },
            navConfig(item, index) {
                this.$router.push({path: "/devices/" + this.$route.params.idPrimary + "/ports/" + this.port.id + "/configurations"})
            },
            navDelete(item, index) {
                this.deleteConfirmShow = true;
            },
        }
    }
</script>
