<template>
  <q-card class="my-card">
    <q-item class="bg-blue-grey-7 text-blue-grey-1">
      <q-item-section avatar>
        <q-avatar>
          <q-icon name="mdi-transmission-tower"/>
        </q-avatar>
      </q-item-section>

      <q-item-section>
        <q-item-label>{{ $t('electric_meter.title') }}</q-item-label>
        <q-item-label caption class="text-blue-grey-3">{{ $t('electric_meter.sub_title') }}</q-item-label>
      </q-item-section>
    </q-item>

    <q-card-section class="bg-blue-grey-5 text-grey-1" vertical>
      <div class="row text-center">
        <q-card class="col">
          <q-card-section class="bg-blue-grey-3 text-grey-3 text-h6 row">
            <div class="col">
              <q-skeleton v-if="!deviceDetails['rt_active_power_load']" type="QBadge"/>
              <span v-if="deviceDetails['rt_active_power_load']">{{
                  deviceDetails['rt_active_power_load']['value']
                }} </span><br/>
              <span class="text-overline text-grey-4">KWh </span></div>
            <q-separator vertical></q-separator>
            <div class="col">
              <q-skeleton v-if="!deviceDetails['agg_active_power_60min']" type="QBadge"/>
              <span v-if="deviceDetails['agg_active_power_60min']">
                {{ deviceDetails['agg_active_power_60min']['value'] }}
                <span v-if="deviceDetails['agg_active_power_60min']['deltaDiff'] != null">
                  <q-icon name="mdi-arrow-top-right-thin-circle-outline" size="ms" class="text-red" v-if="deviceDetails['agg_active_power_60min']['deltaDiff'] > 0"/>
                  <q-icon name="mdi-arrow-bottom-right-thin-circle-outline" size="ms" class="text-green-9" v-else-if="deviceDetails['agg_active_power_60min']['deltaDiff'] < 0"/>
                  <q-icon name="mdi-equal-box" size="ms" class="text-yellow-7" v-else/>
                </span>
              </span>

              <br/>
              <span class="text-overline text-grey-4">KW1H</span></div>
            <q-separator vertical/>
            <div class="col">
              <q-skeleton v-if="!deviceDetails['agg_active_power_24h']" type="QBadge"/>
              <span v-if="deviceDetails['agg_active_power_24h']">
                {{ deviceDetails['agg_active_power_24h']['value'] }}
               <span v-if="deviceDetails['agg_active_power_24h']['deltaDiff'] != null">
                  <q-icon name="mdi-arrow-top-right-thin-circle-outline" size="ms" class="text-red" v-if="deviceDetails['agg_active_power_24h']['deltaDiff'] > 0"/>
                  <q-icon name="mdi-arrow-bottom-right-thin-circle-outline" size="ms" class="text-green-9" v-else-if="deviceDetails['agg_active_power_24h']['deltaDiff'] < 0"/>
                  <q-icon name="mdi-equal-box" size="ms" class="text-yellow-7" v-else/>
                </span>
              </span><br/>
              <span class="text-overline text-grey-4">KW24H</span>
            </div>
            <q-separator vertical/>
            <div class="col">
              <q-skeleton v-if="!deviceDetails['agg_active_power_1month']" type="QBadge"/>
              <span v-if="deviceDetails['agg_active_power_1month']">
                {{deviceDetails['agg_active_power_1month']['value']}}
                <span v-if="deviceDetails['agg_active_power_1month']['deltaDiff'] != null">
                  <q-icon name="mdi-arrow-top-right-thin-circle-outline" size="ms" class="text-red" v-if="deviceDetails['agg_active_power_1month']['deltaDiff'] > 0"/>
                  <q-icon name="mdi-arrow-bottom-right-thin-circle-outline" size="ms" class="text-green-9" v-else-if="deviceDetails['agg_active_power_1month']['deltaDiff'] < 0"/>
                  <q-icon name="mdi-equal-box" size="ms" class="text-yellow-7" v-else/>
                </span>
              </span><br/>
              <span class="text-overline text-grey-4">KW1M</span>
            </div>
          </q-card-section>
        </q-card>
      </div>
      &nbsp;
      <div class="row text-center" v-if="deviceDetails['total_active_power']">
        <q-card class="col">
          <q-card-section class="bg-blue-grey-4 row" vertical>
            <div class="col">
              <span class="text-grey-3 text-h6" v-if="indexSpan">{{ Number(deviceDetails['total_active_power']['value']) + Number(indexSpan) }}</span>
              <span class="text-overline text-grey-5">  KWh </span>
            </div>
            <q-separator vertical/>
            <div class="col">
              <span class="text-grey-5 text-h6">{{ deviceDetails['total_active_power']['value'] }} </span><span
              class="text-overline text-grey-5">  KWh </span>
            </div>
          </q-card-section>
        </q-card>
      </div>
      &nbsp;
      <div class="row text-center">
        <q-card class="col bg-grey-7">
          <q-card-section class="bg-grey-7 text-grey-2 text-h6">
            <span v-if="deviceDetails['l1_current']">
                <span class="text-amber-9">{{ deviceDetails['l1_current']['value'] }}</span>
                <q-icon name="mdi-alpha-a-circle-outline" size="ms" class="text-grey-6"/>
            </span>
            <q-separator/>
            <span v-if="deviceDetails['l1_voltage']">
                <span class="text-deep-orange-5">{{ deviceDetails['l1_voltage']['value'] }}</span>
                <q-icon name="mdi-alpha-v-circle-outline" size="ms" class="text-grey-6"/>
            </span>
          </q-card-section>
        </q-card>
        &nbsp;
        <q-card class="col bg-grey-7">
          <q-card-section class="bg-grey-7 text-grey-2 text-h6">
            <span v-if="deviceDetails['l2_current']">
                <span class="text-amber-9">{{ deviceDetails['l2_current']['value'] }}</span>
                <q-icon name="mdi-alpha-a-circle-outline" size="ms" class="text-grey-6"/>
            </span>
            <q-separator/>
            <span v-if="deviceDetails['l2_voltage']">
                <span class="text-deep-orange-5">{{ deviceDetails['l2_voltage']['value'] }}</span>
                <q-icon name="mdi-alpha-v-circle-outline" size="ms" class="text-grey-6"/>
            </span>
          </q-card-section>
        </q-card>
        &nbsp;
        <q-card class="col bg-grey-7">
          <q-card-section class="bg-grey-7 text-grey-2 text-h6">
            <span v-if="deviceDetails['l3_current']">
                <span class="text-amber-9">{{ deviceDetails['l3_current']['value'] }}</span>
                <q-icon name="mdi-alpha-a-circle-outline" size="ms" class="text-grey-6"/>
            </span>
            <q-separator/>
            <span v-if="deviceDetails['l3_voltage']">
                <span class="text-deep-orange-5">{{ deviceDetails['l3_voltage']['value'] }}</span>
                <q-icon name="mdi-alpha-v-circle-outline" size="ms" class="text-grey-6"/>
            </span>
          </q-card-section>
        </q-card>
      </div> &nbsp;
      <div class="row">
        <q-card class="col">
          <q-card-section class="bg-blue-grey-4 text-grey-2" vertical>
            <q-icon name="mdi-transmission-tower"/>
            <q-skeleton v-if="!deviceDetails['total_reactive_power']" type="QBadge"/>
            <span class="text-grey-4" v-if="deviceDetails['total_reactive_power']">{{ deviceDetails['total_reactive_power']['value'] }}</span>
          </q-card-section>
        </q-card>
      </div>
    </q-card-section>
  </q-card>
</template>
<script>

import {computed, defineComponent, onMounted, ref, toRefs, watch} from 'vue';
import {CONFIG_GLOBAL_GET_STRING_VAL, DEVICE_GET_BY_ID_WITH_PORT_VALUES, TIMESERIES_GET_LATEST_BY_KEYS} from '@/graphql/queries';
import {useApolloClient, useQuery} from "@vue/apollo-composable";
import {useStore} from "vuex";
import _ from "lodash";

export default defineComponent({
  name: 'ElectricMeter',
  props: {
    deviceId: Number,
  },
  components: {},
  setup(props, {emit}) {
    const {client} = useApolloClient();
    const store = useStore();
    let {deviceId: deviceId} = toRefs(props);
    let device = ref({})
    let deviceDetails = ref({})
    let portIds = ref([])
    let indexSpan = ref({})
    client.query({
      query: CONFIG_GLOBAL_GET_STRING_VAL,
      variables: {key: 'emeter.indexSpan'},
      fetchPolicy: 'network-only',
    }).then(data => {
      indexSpan.value = data.data.config
    })
    const loadStatistics = () => {
      if (device.value) {
        let keyPrefix = 'emeters.' + device.value.code;
        client.query({
          query: TIMESERIES_GET_LATEST_BY_KEYS,
          variables: {
            keys: [
              keyPrefix + '.agg_active_power_60min'
              , keyPrefix + '.agg_active_power_24h'
              , keyPrefix + '.agg_active_power_1month']
          },
          fetchPolicy: 'network-only',
        }).then(data => {
          let statMap = _.reduce(data.data.statLatestByKeys, function (hash, item) {
            if (item != null) {
              hash[item['key']] = ref(item);
            }
            return hash;
          }, {})
          deviceDetails.value['agg_active_power_60min'] = statMap[keyPrefix + '.agg_active_power_60min'];
          deviceDetails.value['agg_active_power_24h'] = statMap[keyPrefix + '.agg_active_power_24h'];
          deviceDetails.value['agg_active_power_1month'] = statMap[keyPrefix + '.agg_active_power_1month'];
        });
      }
    }
    const loadDetails = () => {
      client.query({
        query: DEVICE_GET_BY_ID_WITH_PORT_VALUES,
        variables: {id: deviceId.value},
        fetchPolicy: 'network-only',
      }).then(data => {
        device.value = data.data.device
        deviceDetails.value = _.reduce(
          data.data.device.ports,
          function (hash, value) {
            hash[value['internalRef'].toString()] = ref(value);
            return hash;
          },
          {},
        );
        portIds.value = _.reduce(
          data.data.device.ports,
          function (hash, value) {
            hash.push(value['id'])
            return hash;
          },
          [],
        );
        loadStatistics();
      });

    };
    onMounted(() => {
      loadDetails()
    })
    const wsMessage = computed(() => store.getters.ws.message);
    watch(
      () => store.getters.ws.message,
      function () {
        if (wsMessage.value.eventName == 'evt_port_value_persisted') {
          let payload = JSON.parse(wsMessage.value.jsonPayload);
          if (portIds.value.includes(Number(payload.p2))) {
            deviceDetails.value[payload.p3].value = payload.p4
          }
        } else if (wsMessage.value.eventName == 'evt_stat_value_changed') {

          loadStatistics();
        }
      });
    return {
      device,
      deviceDetails,
      indexSpan
    }

  }
});
</script>
<style>


</style>

