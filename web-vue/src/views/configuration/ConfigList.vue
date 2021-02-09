<template>
    <div>
        <CRow>
            <CCol col="12" xl="8">
                <transition name="slide">
                    <CCard>
                        <CCardHeader>
                            {{ $t("configurations.list.title") }}
                            <div class="card-header-actions">
                                <a style="cursor: pointer" class="card-header-action" rel="noreferrer noopener"
                                   @click="$router.go(-1)">
                                    <small class="text-muted">{{ $t("actions.cancel") }}</small>
                                </a>
                            </div>
                        </CCardHeader>
                        <CCardBody>
                            <CDataTable
                                    striped
                                    hover
                                    small
                                    fixed
                                    :items="configurations"
                                    :fields="$options.fields"
                            >
                                <template #key="{item}">
                                    <td>
                                        <div v-if="item.id != null">
                                            <b>{{item.key}}</b>
                                        </div>
                                        <div v-if="item.id == null">
                                            <CInput :placeholder="item.key" :value="item.key" @input="updateFieldKey($event, item)" :ref="item.key"/>
                                        </div>
                                    </td>
                                </template>
                                <template #value="{item}">
                                    <td>
                                        <CInputCheckbox
                                                v-if="item.key!=null && item.key.endsWith('ed')"
                                                :key="item.key"
                                                :value="item.value"
                                                :checked="item.value"
                                                @update:checked="check($event, item)"
                                                :inline="true"
                                                :ref="item.key"
                                        />
                                        <CInput v-if="item.key==null || !item.key.endsWith('ed')"
                                                :value="item.value"
                                                @input="updateFieldValue($event, item)"
                                                :ref="item.key"/>
                                    </td>
                                </template>
                                <template #actions="{item}">
                                    <td>
                                        <CButton type="submit" size="sm" color="success" @click="save(item)" :disabled="confToUpdate.indexOf(item) == -1">
                                            <CIcon name="cil-check"/>
                                            {{ $t("actions.save") }}
                                        </CButton>
                                        <CButton type="submit" size="sm" color="danger" @click="deleteConfirm(item)" style="margin-left: 2px">
                                            <CIcon name="cil-ban"/>
                                            {{ $t("actions.delete") }}
                                        </CButton>
                                    </td>
                                </template>
                            </CDataTable>
                            <CRow>
                                <CButton type="submit" size="sm" color="success" @click="addNew">
                                    <CIcon name="cil-plus"/>
                                    {{ $t("actions.add_new") }}
                                </CButton>
                            </CRow>
                        </CCardBody>
                    </CCard>
                </transition>
            </CCol>
        </CRow>
        <CModal :title="$t('modal.delete.title')" color="danger" :show.sync="deleteConfirmShow">
            {{ $t("modal.delete.confirmation_msg") }} <strong>{{selectedItem.key}} = {{selectedItem.value}}</strong> ?
            <template #footer>
                <CButton @click="remove" color="danger">{{ $t("actions.delete") }}</CButton>
                <CButton @click="deleteConfirmShow = false" color="success">{{ $t("actions.cancel") }}</CButton>
            </template>
        </CModal>
    </div>
</template>
<script>
    import {CONFIGURATION_LIST, CONFIGURATION_SET_VALUE, CONFIGURATION_REMOVE_CONFIG} from "../../graphql/queries";
    import i18n from './../../i18n';
    import {uid} from 'uid';

    export default {
        name: 'ConfigList',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value'},
            {
                key: 'actions', label: i18n.t('table.header.actions'),
                sorter: false,
                filter: false
            }
        ],
        data: () => {
            return {
                configurations: [],
                confToUpdate: [],
                deleteConfirmShow: false,
                selectedItem: {}
            }
        },
        mounted() {
            this.init();
        },
        methods: {
            init() {
                this.$apollo.query({
                    query: CONFIGURATION_LIST,
                    variables: {entityId: this.$route.params.id, entityType: this.$route.meta.entityType},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    this.configurations = response.data.configurationListByEntity;
                });
            },
            updateFieldKey(value, item) {
                item.key = value;
                if (this.confToUpdate.indexOf(item) == -1) {
                    this.confToUpdate.push(item)
                }
            },
            updateFieldValue(value, item) {
                item.value = value;
                if (this.confToUpdate.indexOf(item) == -1) {
                    this.confToUpdate.push(item)
                }
            },
            save: function (item) {

                let tempId = item.tempId;
                this.$apollo.mutate({
                    mutation: CONFIGURATION_SET_VALUE,
                    variables: {key: item.key, value: item.value, entityId: item.entityId, entityType: item.entityType}
                }).then(response => {
                    let index = null;
                    if (item.id != null) {
                        index = this.configurations.findIndex(k => k.id == item.id);
                    } else if (tempId != null) {
                        index = this.configurations.findIndex(k => k.tempId == item.tempId);
                    }
                    this.$set(this.configurations, index, response.data.savePropertyValue)


                });
                return true;
            },
            deleteConfirm(item) {
                this.deleteConfirmShow = true;
                this.selectedItem = item
            },
            remove: function () {
                if (this.selectedItem.id != null) {
                    this.$apollo.mutate({
                        mutation: CONFIGURATION_REMOVE_CONFIG,
                        variables: {id: this.selectedItem.id}
                    }).then(response => {
                        this.init();
                        this.deleteConfirmShow = false;
                    });
                } else if (this.selectedItem.tempId != null) {
                    let index = this.configurations.findIndex(k => k.tempId != null && k.tempId == this.selectedItem.tempId);
                    this.$delete(this.configurations, index);
                }
                this.deleteConfirmShow = false;
            },
            addNew: function () {
                let newItem = {
                    entityType: this.$route.meta.entityType,
                    entityId: this.$route.params.id,
                    key: null,
                    value: null,
                    tempId: uid()
                };
                if (this.configurations.length > 0) {
                    this.configurations.push(newItem);
                } else {
                    this.configurations = [newItem];
                }
            }
        }
    }
</script>