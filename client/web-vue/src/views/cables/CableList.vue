<template>
    <CRow>
        <CCol col="12" xl="8">
            <transition name="slide">
                <CCard>
                    <CCardHeader>
                        Cabluri
                    </CCardHeader>
                    <CCardBody>
                        <CDataTable
                                hover
                                striped
                                :items="items"
                                :fields="fields"
                                :items-per-page="perPage"
                                :pagination="$options.paginationProps"
                                index-column
                                clickable-rows
                                table-filter
                                columnFilter
                                sorter
                                responsive
                                @row-clicked="navView"
                        >
                            <template #category="data">
                                <td>
                                    <strong>{{data.item.category.name}}</strong>
                                </td>
                            </template>
                            <template #patchPanelPort="data">
                                <td>
                                    <strong>{{data.item.patchPanelPort}}</strong>
                                </td>
                            </template>
                            <template #patchPanel="data">
                                <td>
                                    <strong v-if="data.item.patchPanel">{{data.item.patchPanel.code}}</strong>
                                </td>
                            </template>
                            <template #actions="data">
                                <td>
                                    <a @click="$router.push({path: '/cables/'+data.item.id +'/edit'})" style="padding-right: 1em">
                                        <font-awesome-icon icon="edit" size="1x"/>
                                    </a>
                                    <a @click="modalCheck(data.item)">
                                        <font-awesome-icon icon="ban" size="1x"/>
                                    </a>
                                </td>
                            </template>
                        </CDataTable>
                    </CCardBody>
                    <CCardFooter>
                        <CButton color="success" @click="addNew">Add new</CButton>
                    </CCardFooter>
                    <CModal title="Delete cable" color="danger" :show.sync="deleteConfirmShow">
                        Do you want delete : <strong>{{selectedItem.description}}</strong> ?
                        <template #footer>
                            <CButton @click="removeCable" color="danger">Delete</CButton>
                            <CButton @click="deleteConfirmShow = false" color="success">Cancel</CButton>
                        </template>
                    </CModal>
                </CCard>
            </transition>
        </CCol>
    </CRow>
</template>

<script>
    import {CABLE_LIST_ALL, CABLE_DELETE} from "../../graphql/queries";

    export default {
        name: 'CableList',
        data: () => {
            return {
                items: [],
                fields: [
                    {key: 'id', label: 'ID'},
                    {key: 'code', label: 'Cod'},
                    {key: 'codeNew', label: 'Cod nou'},
                    {key: 'codeOld', label: 'Cod vechi'},
                    {key: 'category', label: 'Categorie'},
                    {key: 'description', label: 'Descriere'},
                    {key: 'patchPanel', label: 'Patch'},
                    {key: 'patchPanelPort', label: 'Patch port'},
                    {
                        key: 'actions', label: 'Actions',
                        sorter: false,
                        filter: false
                    }
                ],
                perPage: 10,
                deleteConfirmShow: false,
                selectedItem: {}
            }
        },
        paginationProps: {
            align: 'center',
            doubleArrows: false,
            previousButtonHtml: 'prev',
            nextButtonHtml: 'next'
        },
        mounted() {
            this.loadCables();
        },
        watch: {
            '$route.fullPath': 'loadCables'
        },
        methods: {
            modalCheck(item) {
                this.deleteConfirmShow = true;
                this.selectedItem = item
            },
            removeCable() {
                this.$apollo.mutate({
                    mutation: CABLE_DELETE, variables: {id: this.selectedItem.id}
                }).then(response => {
                    this.deleteConfirmShow = false;
                    this.loadCables();
                });
            },
            addNew() {
                this.$router.push({path: "/cables/create"})
            },
            navView(cable) {
                this.$router.push({path: '/cables/' + cable.id + '/view'})
            },
            loadCables() {
                this.$apollo.query({
                    query: CABLE_LIST_ALL,
                    variables: {},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.items = response.data.cableList;
                });
            }
        }
    }
</script>
