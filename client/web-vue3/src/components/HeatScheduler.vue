<template>
  <q-btn size="md" round icon="mdi-thermometer-lines" class="bg-blue-grey-8 text-blue-grey-1" @click="showNSc"/>
  <q-dialog v-model="visibleNSc" transition-show="jump-up" transition-hide="jump-down" class="heat-scheduler">
    <q-card class="bg-white" style="height: 320px; width: 500px; max-width: 80vw;">
      <q-bar class="bg-green-5 text-white">
        <q-icon name="mdi-thermometer"/>
        <div>Thermostat scheduler</div>
        <q-space/>
        <q-btn dense flat icon="close" v-close-popup>
          <q-tooltip class="bg-primary">Close</q-tooltip>
        </q-btn>
      </q-bar>

      <q-card-actions align="around">
        <slider v-model="heatScheduler.temp"
                orientation="circular"
                :height="16"
                :width="100 + 'px'"
                :min="0"
                :max="40"
                :step="1"
                tooltip
                color="#4080f7"
                tooltipText="%v ℃"
                tooltipColor="#4080f7"
                tooltipTextColor="#FEFEFE"
                sticky/>
        <q-separator vertical></q-separator>
        <div class="text-grey-10">
          <q-badge rounded color="secondary" :label="heatScheduler.temp+' ℃'" size="md"/>
          <q-input filled v-model="time" mask="time" :rules="['time']">
            <template v-slot:append>
              <q-icon name="access_time" class="cursor-pointer">
                <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                  <q-time v-model="time" color="orange">
                    <div class="row items-center justify-end">
                      <q-btn v-close-popup label="Set" color="dark" flat/>
                    </div>
                  </q-time>
                </q-popup-proxy>
              </q-icon>
            </template>
          </q-input>
          <q-btn label="Add" class="bg-green-5" icon="mdi-plus"/>
        </div>
      </q-card-actions>
      <q-table :rows="heatScheduler.scheduleItems" :columns="data.columns" row-key="name" dense>
        <template v-slot:body-cell-actions="props">
          <q-td :props="props">
            <q-btn dense round flat color="grey" icon="delete" @click="onDelete(props.row)"></q-btn>
          </q-td>
        </template>
      </q-table>
    </q-card>
  </q-dialog>
</template>
<script>
  import {defineComponent, ref} from 'vue';
  import slider from "vue3-slider"
  import {useApolloClient} from '@vue/apollo-composable';
  import {CONFIGURATION_GET_LIST_VALUE} from "@/graphql/queries";


  export default defineComponent({
    name: 'HeatScheduler',
    props: {
      zone: Object,
    },
    components: {
      slider
    },
    setup(props) {
      let visibleNSc = ref(false)
      const {client} = useApolloClient();
      const onDelete = (row) => {
        console.log(`Deleting row - '${row.name}'`)
      }

      const showNSc = () => {
        visibleNSc.value = true;
      }

      let heatScheduler = ref({
        show: false,
        scheduleItems: [],
        temp: ref(10),
        time: null
      })

      client.query({
        query: CONFIGURATION_GET_LIST_VALUE,
        variables: {
          entityId: props.zone.id,
          entityType: 'ZONE',
          key: 'key.temp.schedule.list.value'
        },
        fetchPolicy: 'network-only'
      }).then(response => {
        response.data.configListByKey.forEach(function (config, index) {
          let configValue = JSON.parse(config.value);
          heatScheduler.value['scheduleItems'].push({id: config.id, time: configValue.time, temp: configValue.temp});
        });
      })

      return {
        visibleNSc,
        showNSc,
        onDelete,
        heatScheduler,
        time: ref('10:56'),
        data: {
          columns: [
            {
              name: 'time',
              required: true,
              label: 'Ora',
              align: 'left',
              field: row => row.time,
              format: val => `${val}`,
              sortable: true,
            },
            {
              name: 'temp',
              required: true,
              label: '*C',
              align: 'left',
              field: row => row.temp,
              format: val => `${val}`,
              sortable: true,
            },
            {
              name: "actions",
              align: "right",
              label: "Action"
            }
          ]
        },
      };
    },
  });
</script>
<style>
  .heat-scheduler .q-badge {
    background-color: var(--q-primary);
    border-radius: 4px;
    color: #fff;
    font-size: 22px;
    font-weight: 400;
    line-height: 12px;
    min-height: 12px;
    padding: 13px 13px;
    vertical-align: initial;
  }
</style>
