<template>
  <q-card class="my-card">
    <q-item class="bg-light-green-7 text-light-green-1">
      <q-item-section avatar>
        <q-avatar>
          <q-icon name="mdi-water-boiler"/>
        </q-avatar>
      </q-item-section>

      <q-item-section>
        <q-item-label>{{ $t('heat_pump.title') }}</q-item-label>
        <q-item-label caption class="text-light-green-3">{{ $t('electric_meter.sub_title') }}</q-item-label>
      </q-item-section>
    </q-item>

    <q-card-section class="bg-light-green-5 text-light-green-1" vertical>
      <div class="row text-center">
        <q-card class="col">
          <q-card-section class="bg-light-green-3 text-h6" vertical>
            <!--outdoor temp.-->
            <q-icon name="mdi-sun-thermometer" size="md" class="text-orange-8" left/>
            <span class="text-light-green-1"
                  v-if="deviceDetails['40004']">{{ deviceDetails['40004']['value'] / 10 }} </span><span
            class="text-h5 text-light-green-7">°C</span>
          </q-card-section>
        </q-card>
      </div>
      &nbsp;
      <div class="row text-center">
        <q-card class="col">
          <q-card-section class="bg-light-green-4 " vertical>
            <!--hot water charging-->
            <q-icon name="mdi-water-thermometer" size="sm" class="text-blue-8" left/>
            <span class="text-light-green-1" v-if="deviceDetails['40014']">{{
                deviceDetails['40014']['value'] / 10
              }} </span>
            <span
              class="text-h5 text-light-green-7">°C</span>
          </q-card-section>
        </q-card>
      </div>
      &nbsp;
      <div class="row text-center">
        <q-card class="col">
          <q-card-section class="bg-light-green-3 text-h6" vertical>
            <!--room temperature-->
            <q-icon name="mdi-home-thermometer-outline" size="md" class="text-light-blue-4"/>
            <span class="text-light-green-1"
                  v-if="deviceDetails['40033']">{{ deviceDetails['40033']['value'] / 10 }} </span><span
            class="text-h5 text-light-green-7">°C</span>
          </q-card-section>
        </q-card>
      </div>
      &nbsp;
      <div class="row text-center">
        <q-card class="col bg-light-green-7">
          <q-card-section class="bg-light-green-7 text-light-green-2 text-h6">
            <span v-if="deviceDetails['43009']">
              <!--Flow temp-->
                <span class="text-amber-5">{{ deviceDetails['43009']['value'] / 10 }}°C</span>
            </span>
            <q-separator/>
            <span v-if="deviceDetails['40012']">
              <!--Return flow temp-->
                <span class="text-deep-orange-3">{{ deviceDetails['40012']['value'] / 10 }}°C</span>
            </span>
          </q-card-section>
        </q-card>
        &nbsp;
        <q-card class="col bg-light-green-7">
          <!-- Temperatura antigel-->
          <q-card-section class="bg-light-green-7 text-light-green-2 text-h6">
            <span v-if="deviceDetails['40015']">
              <!--IN-->
                <q-icon name="mdi-waves-arrow-right" size="ms" class="text-light-green-4"/>
                <span class="text-amber-5"> {{ deviceDetails['40015']['value'] / 10 }}°C</span>
            </span>
            <q-separator/>
            <span v-if="deviceDetails['40016']">
              <!--OUT-->
                <q-icon name="mdi-waves-arrow-left" size="ms" class="text-light-green-4"/>
                <span class="text-deep-orange-3"> {{ deviceDetails['40016']['value'] / 10 }}°C</span>
            </span>
          </q-card-section>
        </q-card>
        &nbsp;
        <q-card class="col bg-light-green-7">
          <q-card-section class="bg-light-green-7 text-light-green-2 text-h6">
            <span v-if="deviceDetails['43084']">
              <!--electrical addition power-->
                <span class="text-amber-5">{{ deviceDetails['43084']['value'] / 100 }}KW</span>
                <span class="text-light-green-4 text-overline">now</span>
            </span>
            <q-separator/>
            <span v-if="deviceDetails['47212']">
              <!--set max electrical add-->
                <span class="text-deep-orange-3">{{ deviceDetails['47212']['value'] / 100 }}KW</span>
                <span class="text-light-green-4 text-overline">max</span>
            </span>
          </q-card-section>
        </q-card>
      </div>
    </q-card-section>
  </q-card>
</template>
<script>
import {computed, defineComponent, onMounted, ref, toRefs, watch} from 'vue';

import {useApolloClient} from "@vue/apollo-composable";
import {useWebSocketStore} from "@/store/websocket.store";

import {DEVICE_GET_BY_ID_WITH_PORT_VALUES} from '@/graphql/queries';

import _ from "lodash";



export default defineComponent({
  name: 'HeatPump',
  props: {
    deviceId: Number,
  },
  components: {},
  setup(props, {emit}) {
    const {client} = useApolloClient();
    const wsStore = useWebSocketStore();
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
    const wsMessage = computed(() => wsStore.ws.message);
    watch(
      () => wsStore.ws.message,
      function () {
        if (wsMessage.value?.eventName == 'evt_port_value_persisted') {
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
