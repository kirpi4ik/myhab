<template>
  <q-btn class="glossy" color="blue-3" icon="mdi-format-list-bulleted-square" @click="openLog"/>
  <q-dialog v-model="showLog" transition-show="jump-up" transition-hide="jump-down">
    <q-card class="bg-white">
      <q-bar class="bg-green-5 text-white">
        <q-icon name="lock"/>
        <div>Events</div>
        <q-space/>
        <q-btn dense flat icon="close" v-close-popup>
          <q-tooltip class="bg-primary">Close</q-tooltip>
        </q-btn>
      </q-bar>

      <q-card-section>
        <div class="q-pa-md">
          <q-table :rows="data.items" :columns="data.columns" row-key="name"/>
        </div>
      </q-card-section>

    </q-card>
  </q-dialog>
</template>
<script>
  import {defineComponent, ref, toRef} from 'vue';
  import {PERIPHERAl_EVENT_LOGS} from '@/graphql/queries';
  import {format} from 'date-fns'
  import _ from 'lodash';

  export default defineComponent({
    name: 'EventLogger',
    props: {
      peripheral: Object
    },
    components: {},
    setup(props) {
      return {
        asset: toRef(props, 'peripheral'),
        showLog: ref(false),
        data: {
          columns: [
            {
              name: 'date',
              required: true,
              label: 'Date',
              align: 'left',
              field: row => row.strDate,
              format: val => `${val}`,
              sortable: true
            },
            {
              name: 'p4',
              required: true,
              label: 'Val',
              align: 'left',
              field: row => row.p4,
              format: val => `${val}`,
              sortable: true
            },
            {
              name: 'p3',
              required: true,
              label: 'Sursa',
              align: 'left',
              field: row => row.p3,
              format: val => `${val}`,
              sortable: true
            },
            {
              name: 'p6',
              required: true,
              label: 'Context',
              align: 'left',
              field: row => row.p6,
              format: val => `${val}`,
              sortable: true
            }
          ],
          items: []
        },
      };
    },
    mounted() {
    },
    methods: {
      init() {
        this.$apollo
          .query({
            query: PERIPHERAl_EVENT_LOGS,
            variables: {
              p2: String(this.asset.id),
              count: 10,
              offset: 0
            },
            fetchPolicy: 'network-only',
          })
          .then(response => {
            let data = _.cloneDeep(response.data)
            data.eventsByP2.forEach(function (event, index) {
              event.strDate = format(new Date(event.tsCreated), 'yyyy/MM/dd HH:mm')
            });
            this.data.items = data.eventsByP2
          });
      },
      openLog: function () {
        this.showLog = true;
        this.init();
      }
    },
  });
</script>
