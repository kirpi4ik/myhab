<template>
    <CRow>
        <CCol col="12" lg="6">
            <CCard>
                <CCardHeader>
                    <strong>Port </strong> <small>{{cable.code}}</small>
                    <div class="card-header-actions">
                        <a @click="$router.push({path: '/cables/'+$route.params.idPrimary+'/edit'})" style="cursor: pointer" class="card-header-action" rel="noreferrer noopener">
                            <small class="text-muted">{{ $t("actions.edit") }}</small> |
                        </a>
                        <a @click="$router.push({path: '/cables/'+$route.params.idPrimary+'/configurations'})" style="cursor: pointer" class="card-header-action" rel="noreferrer noopener">
                            <small class="text-muted">{{ $t("actions.config") }}</small> |
                        </a>
                        <a @click="$router.push({path: '/cables'})" style="cursor: pointer" class="card-header-action" rel="noreferrer noopener">
                            <small class="text-muted">{{ $t("actions.cancel") }}</small>
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
                                        <div v-if="Array.isArray(item.value)">
                                            <CBadge color="success" v-for="(badge) in item.value" style="margin-right: 2px" v-bind:key="badge.id">
                                                <a v-if="badge.__typename == 'DevicePeripheral'" @click="$router.push({path: '/peripherals/'+badge.id+'/view'})">
                                                    {{badge.name}}
                                                </a>
                                                <a v-else-if="badge.__typename == 'Zone'" @click="$router.push({path: '/zones/'+badge.id+'/view'})">
                                                    {{badge.name}}
                                                </a>
                                                <a v-else-if="badge.__typename == 'DevicePort'" @click="$router.push({path: '/devices/'+badge.device.id+'/ports/'+badge.id+'/view'})">
                                                    {{badge.name}}
                                                </a>
                                                <span v-else>
                                                    {{badge.name}}
                                                </span>
                                            </CBadge>
                                        </div>
                                        <div v-else-if="item.value instanceof Object && item.value.name!=null">
                                            {{item.value.name}}
                                        </div>
                                        <div v-else-if="item.value instanceof Object && item.value.title!=null">
                                            {{item.value.title}}
                                        </div>
                                        <div v-else-if="item.value instanceof Object && item.value.code!=null">
                                            {{item.value.code}}
                                        </div>
                                        <div v-else-if="item.value === true || item.value === false">
                                            <CIcon v-if="item.value === true" name="cil-chevron-bottom"/>
                                            <CIcon v-if="item.value === false" name="cil-square"/>
                                            <div v-if="item.value != true && item.value != false">
                                                {{item.value}}
                                            </div>
                                        </div>
                                        <div v-else-if="!(item.value instanceof Object)">
                                            {{item.value}}
                                        </div>
                                    </td>
                                </template>
                            </CDataTable>
                        </CCol>
                    </CRow>
                </CCardBody>
                <CCardFooter>
                    <CButton color="primary" @click="$router.push({path: '/cables'})">Back</CButton>
                </CCardFooter>
            </CCard>
        </CCol>
    </CRow>
</template>

<script>
    import {CABLE_BY_ID} from "../../graphql/queries";


    export default {
        name: 'CableView',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        data: () => {
            return {
                cableDetails: [],
                roles: [],
                readonly: ["id", "__typename", "uid", "rack"],
                cable: {}
            }
        },
        mounted() {
            this.init();
        },
        methods: {
            init() {

                let cable = {};

                this.$apollo.query({
                    query: CABLE_BY_ID,
                    variables: {id: this.$route.params.idPrimary},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    let removeReadonly = function (keyMap) {
                        return !this.readonly.includes(keyMap.key)
                    }.bind(this);

                    this.cable = response.data.cable;
                    const cableDetailsToMap = cable ? Object.entries(this.cable) : [['id', 'Not found']];
                    this.cableDetails = cableDetailsToMap.map(([key, value]) => {
                        debugger
                        return {key, value}
                    }).filter(removeReadonly);
                });
            }
        }
    }
</script>
<style>
    .field-value {
        color: #abb3c0;
    }
</style>