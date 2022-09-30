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
      <div class="row text-center" v-if="deviceDetails['rt_active_power_load']">
        <q-card class="col">
          <q-card-section class="bg-blue-grey-3 text-grey-3 text-h6" vertical>
            <span>{{ deviceDetails['rt_active_power_load']['value'] }} </span><span class="text-overline text-grey-4">KWh</span>
          </q-card-section>
        </q-card>
      </div>
      &nbsp;
      <div class="row text-center" v-if="deviceDetails['total_active_power']">
        <q-card class="col">
          <q-card-section class="bg-blue-grey-4 " vertical>
            <span class="text-grey-3 text-h6">{{ deviceDetails['total_active_power']['value'] }}  KWh </span><span class="text-overline text-grey-5">index</span>
          </q-card-section>
        </q-card>
      </div>
      &nbsp;
      <div class="row text-center">
        <q-card class="col">
          <q-card-section class="bg-grey-7 text-grey-2 text-h6">
            <span  v-if="deviceDetails['l1_current']">
                <q-icon name="mdi-alpha-a-circle-outline" size="md" class="text-grey-6"/>
                <span class="text-amber-9">{{ deviceDetails['l1_current']['value'] }}</span>
            </span>
            <q-separator/>
            <span v-if="deviceDetails['l1_voltage']">
                <q-icon name="mdi-alpha-v-circle-outline" size="md" class="text-grey-6"/>
                <span class="text-deep-orange-5">{{ deviceDetails['l1_voltage']['value'] }}</span>
            </span>
          </q-card-section>
        </q-card>
        &nbsp;
        <q-card class="col">
          <q-card-section class="bg-grey-7 text-grey-2 text-h6">
            <span  v-if="deviceDetails['l2_current']">
                <q-icon name="mdi-alpha-a-circle-outline" size="md" class="text-grey-6"/>
                <span class="text-amber-9">{{ deviceDetails['l2_current']['value'] }}</span>
            </span>
            <q-separator/>
            <span v-if="deviceDetails['l2_voltage']">
                <q-icon name="mdi-alpha-v-circle-outline" size="md" class="text-grey-6"/>
                <span class="text-deep-orange-5">{{ deviceDetails['l2_voltage']['value'] }}</span>
            </span>
          </q-card-section>
        </q-card>
        &nbsp;
        <q-card class="col">
          <q-card-section class="bg-grey-7 text-grey-2 text-h6">
            <span  v-if="deviceDetails['l3_current']">
                <q-icon name="mdi-alpha-a-circle-outline" size="md" class="text-grey-6"/>
                <span class="text-amber-9">{{ deviceDetails['l3_current']['value'] }}</span>
            </span>
            <q-separator/>
            <span v-if="deviceDetails['l3_voltage']">
                <q-icon name="mdi-alpha-v-circle-outline" size="md" class="text-grey-6"/>
                <span class="text-deep-orange-5">{{ deviceDetails['l3_voltage']['value'] }}</span>
            </span>
          </q-card-section>
        </q-card>
      </div> &nbsp;
      <div class="row" v-if="deviceDetails['total_reactive_power']">
        <q-card class="col">
          <q-card-section class="bg-blue-grey-4 text-grey-2" vertical>
            <q-icon name="mdi-transmission-tower"/>
            <span class="text-deep-orange-5">{{ deviceDetails['total_reactive_power']['value'] }}</span>
          </q-card-section>
        </q-card>
      </div>
    </q-card-section>
  </q-card>
</template>
<script>

import {computed, defineComponent, onMounted, ref, toRefs, watch} from 'vue';
import {DEVICE_GET_BY_ID_WITH_PORT_VALUES} from '@/graphql/queries';
import {useApolloClient} from "@vue/apollo-composable";
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
            loadDetails();
          }
        }
      });
    return {
      device,
      deviceDetails,
      loadDetails
    }

  },
});
</script>
<style>


</style>

