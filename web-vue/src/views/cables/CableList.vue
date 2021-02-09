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
                                sorter
                        >
                            <template #category="data">
                                <td>
                                    <strong>{{data.item.category.name}}</strong>
                                </td>
                            </template>
                            <template #patchPanel="data">
                                <td>
                                    <strong v-if="data.item.patchPanel != null">{{data.item.patchPanel.code}}</strong>
                                </td>
                            </template>
                            <template #actions="data">
                                <td>
                                    <CButton color="success" @click="$router.push({path: '/cables/'+data.item.id +'/view'})">View</CButton>
                                    |
                                    <CButton color="danger" @click="modalCheck(data.item)" class="mr-auto">
                                        Remove
                                    </CButton>
                                </td>
                            </template>
                        </CDataTable>
                    </CCardBody>
                    <CCardFooter>
                        <CButton color="success" @click="addNew">Add new</CButton>
                    </CCardFooter>
                    <CModal title="Delete cable" color="danger" :show.sync="deleteConfirmShow">
                        Do you want delete : <strong>{{selectedItem.cablename}}</strong> ?
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
                    {key: 'code', label: 'Code'},
                    {key: 'codeNew', label: 'Code nou'},
                    {key: 'codeOld', label: 'Code vechi'},
                    {key: 'category', label: 'Categorie'},
                    {key: 'patchPanel', label: 'Patch'},
                    {
                        key: 'actions', label: 'Action',
                        sorter: false,
                        filter: false
                    }
                ],
                perPage: 5,
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
                    this.deleteConfirmShow = false
                    this.loadCables();
                });
            },
            addNew() {
                this.$router.push({path: "/cables/create"})
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
