<template>
    <div style="display: inline-block">
        <CButton type="submit" size="sm" @click="openLog()" color="info" style="cursor: pointer">
            <CIcon name="cil-list" style="cursor: pointer"/>
        </CButton>
        <CModal title="Evenimente"
                color="success"
                :show.sync="showLog">
            <CDataTable :items="events" :fields="tbl.fields" :striped="true" :border="true" :dark="false">
                <template #actions="item">
                    <div>{{item}}</div>
                </template>
            </CDataTable>
            <template #footer>
                <CButton @click="showLog = false" color="danger">Inchide</CButton>
            </template>
        </CModal>
    </div>
</template>
<script>
    import {PERIPHERAl_EVENT_LOGS} from "../../graphql/queries";
    import {format} from 'date-fns'
    import _ from "lodash";

    export default {
        name: 'EventLogger',
        props: {
            peripheral: Object
        },
        data() {
            return {
                showLog: false,
                events: [],
                tbl: {
                    fields: [
                        {key: 'strDate', label: 'Data'},
                        {key: 'p4', label: 'Valoare'},
                        {key: 'p3', label: 'Sursa'},
                        {key: 'p6', label: 'Context'},
                    ]
                }
            }
        },
        methods: {
            loadInitial() {
                this.$apollo.query({
                    query: PERIPHERAl_EVENT_LOGS,
                    variables: {
                        p2: this.peripheral.id,
                        count: 10,
                        offset: 0
                    },
                    fetchPolicy: 'network-only'
                }).then(response => {
                    let data = _.cloneDeep(response.data)
                    data.eventsByP2.forEach(function (event, index) {
                        event.strDate = format(new Date(event.tsCreated), 'yyyy/MM/dd HH:mm')
                    });
                    this.events = data.eventsByP2
                });
            },
            openLog: function () {
                this.showLog = true;
                this.loadInitial();
            }
        }
    }
</script>