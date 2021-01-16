<template>
    <div style="display: inline-block">
        <CButton type="submit" size="sm" @click="openLog()" color="success">
            <CIcon name="cil-list"/>
        </CButton>
        <CModal title="Evenimente"
                color="success"
                :show.sync="showLog">
            <CDataTable :items="events" :fields="tbl.fields" :striped="true" :border="true" :dark="false">
                <template #actions="item">

                </template>
            </CDataTable>
            <template #footer>
                <CButton @click="showLog = false" color="danger">Inchide</CButton>
            </template>
        </CModal>
    </div>
</template>
<script>
    import {
        PERIPHERAl_EVENT_LOGS
    } from "../../graphql/zones";

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
                        {key: 'tsCreated', label: 'Data'},
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
                        p2: this.peripheral.data.uid,
                        count: 10,
                        offset: 0
                    },
                    fetchPolicy: 'network-only'
                }).then(response => {
                    response.data.eventsByP2.forEach(function (event, index) {
                        let date = new Date(event.tsCreated);
                        let strDate = (date.getMonth() < 10 ? '0' : '') + date.getMonth() + '/' + (date.getDate() < 10 ? '0' : '') + date.getDate() + ' ' + (date.getHours() < 10 ? '0' : '') + date.getHours() + ':' + (date.getMinutes() < 10 ? '0' : '') + date.getMinutes();
                        event.tsCreated = strDate
                    });
                    this.events = response.data.eventsByP2
                });
            },
            openLog: function () {
                this.showLog = true;
                this.loadInitial();
            }
        }
    }
</script>