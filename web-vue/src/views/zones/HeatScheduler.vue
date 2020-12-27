<template>
    <div>
        <CButton type="submit" size="sm" @click="openScheduler(zone)" color="success">
            <CIcon name="cil-clock"/>
        </CButton>
        <CModal :title="'Termostat ['+zone.name+']'"
                color="success"
                :show.sync="heatScheduler.show">
            <form class="form-inline" action="" method="post">
                <div class="form-group" style="display: inline; width: 100%">
                    <datetime type="time"
                              v-model="heatScheduler.time"
                              size="4"
                              style="display: inline-block; vertical-align: top"
                              :phrases="{ok: 'Ok', cancel: 'Renunta'}"
                              :minute-step="15">
                    </datetime>
                    <round-slider v-model="heatScheduler.temp"
                                  :name="zone.uid"
                                  min="15"
                                  max="30"
                                  start-angle="10"
                                  end-angle="+160"
                                  line-cap="round"
                                  radius="60"
                                  animation="true"
                                  mouseScrollAction="true"
                                  pathColor="#e99c9c"
                                  rangeColor="#39f"
                                  style="margin-left: 1em; display: inline-block; vertical-align: top"/>
                    <CButton size="sm"
                             color="success"
                             style="margin-left: 2em;display: inline-block; float: right"
                             @click="addListItemConfig(zone.id, 'key.temp.schedule.list.value', heatScheduler.time, heatScheduler.temp)">
                        <CIcon name="cil-plus"/>
                        Adauga
                    </CButton>
                </div>
            </form>
            <CDataTable :items="heatScheduler.scheduleItems" :fields="heatScheduler.fields">
                <template #actions="item">
                    <td class="py-2">
                        <CButton
                                color="danger"
                                variant="outline"
                                square
                                size="sm" @click="deleteConfig(item.item.id)">
                            <CIcon name="cil-x-circle"/>
                        </CButton>
                    </td>
                </template>
            </CDataTable>
            <template #footer>
                <CButton @click="heatScheduler.show = false" color="danger">Inchide</CButton>
            </template>
        </CModal>
    </div>
</template>
<script>
    import RoundSlider from "vue-round-slider";
    import {
        CONFIGURATION_ADDLIST_CONFIG_VALUE,
        CONFIGURATION_DELETE,
        CONFIGURATION_REMOVE_CONFIG,
        CONFIGURATION_GET_LIST_VALUE,
        CONFIGURATION_GET_VALUE,
        CONFIGURATION_SET_VALUE,
    } from "../../graphql/zones";

    export default {
        name: 'HeatScheduler',
        props: {
            zone: Object
        },
        components: {
            RoundSlider
        },
        data() {
            return {
                heatScheduler: {
                    show: false,
                    scheduleItems: [],
                    temp: 10,
                    time: null,
                    fields: [
                        {key: 'time', label: 'Ora'},
                        {key: 'temp', label: '℃'},
                        {
                            key: 'actions', label: 'Sterge',
                            sorter: false,
                            filter: false
                        }
                    ]
                },
            }
        },
        created() {
            this.loadInitial();
        },
        methods: {
            loadInitial() {
                let scheduleItems = [];
                this.$apollo.query({
                    query: CONFIGURATION_GET_LIST_VALUE,
                    variables: {
                        entityId: this.zone.id,
                        entityType: 'ZONE',
                        key: 'key.temp.schedule.list.value'
                    },
                    fetchPolicy: 'network-only'
                }).then(response => {
                    response.data.configListByKey.forEach(function (config, index) {
                        let configValue = JSON.parse(config.value);
                        scheduleItems.push({id: config.id, time: configValue.time, temp: configValue.temp});
                    });
                    this.heatScheduler.scheduleItems = scheduleItems;
                });
            },
            openScheduler: function (zone) {
                this.heatScheduler.show = true;
                this.zone = zone
            },
            deleteConfig: function (id) {
                this.$apollo.mutate({
                    mutation: CONFIGURATION_REMOVE_CONFIG,
                    variables: {id: id}
                }).then(response => {
                    this.loadInitial();
                });
                return true;
            },
            addListItemConfig: function (zoneId, key, time, temp) {
                let date = new Date(time);
                let jsonValue = JSON.stringify({time: (date.getHours() < 10 ? '0' : '') + date.getHours() + ':' + (date.getMinutes() < 10 ? '0' : '') + date.getMinutes(), temp: temp});
                let dropdown = this.$refs['dropdown-' + zoneId];
                if (jsonValue != null) {
                    this.$apollo.mutate({
                        mutation: CONFIGURATION_ADDLIST_CONFIG_VALUE,
                        variables: {key: key, value: jsonValue, entityId: zoneId, entityType: 'ZONE'}
                    }).then(response => {
                        this.loadInitial();
                    });
                }
                return true;
            }
        }
    }
</script>