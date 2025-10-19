<template>
  <q-card class="q-ma-md-xs bg-blue-4">
    <q-card-section class="bg-blue-6 text-blue-1 text-h6 q-ma-md-md">
      <q-item-label class="text-weight-light text-teal-2 text-caption">
        <event-logger :peripheral="asset"/>
        <label class="text-h6">Pompa apa</label>
        <label v-if="config('key.on.timeout')">[ timer:
          {{ humanizeDuration(Number(config('key.on.timeout').value) * 1000, {largest: 2}) }}
        </label>
        <label v-if="asset && asset['data'] && asset.expiration" class="text-weight-light text-blue-grey-3">
          | off at :{{ format(new Date(Number(asset.expiration)), 'HH:mm') }}
        </label>
        <label v-if="config('key.on.timeout')">]</label>
        <q-icon name="mdi-water-pump" class="float-right" size="40px">
          <q-item>
            <q-item-section side>
              <q-item-label>
                <q-btn-dropdown size="sm" flat round icon="settings" class="text-white">
                  <q-list>
                    <q-item
                      clickable
                      v-close-popup
                      @click="setTimeout({ key: 'key.on.timeout', value: item.value, entityId: asset.id, entityType: 'PERIPHERAL' })"
                      v-for="item in stItems"
                      v-bind:key="item.value"
                    >
                      <q-item-section>
                        <q-item-label>Timeout: {{ humanizeDuration(item.value * 1000, {largest: 2}) }}</q-item-label>
                      </q-item-section>
                    </q-item>
                    <q-item
                      clickable
                      v-close-popup
                      @click="deleteTimeout({ entityId: asset.id, entityType: 'PERIPHERAL', key: 'key.on.timeout' })"
                    >
                      <q-item-section>
                        <q-item-label>Sterge timeout</q-item-label>
                      </q-item-section>
                    </q-item>
                    <q-item clickable v-close-popup
                            @click="$router.push({ path: '/admin/peripherals/' + asset.id + '/view' })">
                      <q-item-section>
                        <q-item-label>Detalii</q-item-label>
                      </q-item-section>
                    </q-item>
                  </q-list>
                </q-btn-dropdown>
              </q-item-label>
            </q-item-section>
          </q-item>
        </q-icon>
      </q-item-label>

    </q-card-section>
    <q-separator color="white"/>
    <q-card-section>
      <div class="q-pa-sm text-grey-8">
        <toggle v-if="asset && asset['data'] && asset['data']['state'] !== undefined"
                v-model="asset['data']['state']"
                :id="asset['id']"
                @change="peripheralService.toggle(asset)"/>
      </div>
    </q-card-section>
  </q-card>
</template>
<script>
import {computed, defineComponent, onMounted, ref, watch} from 'vue';
import EventLogger from "components/EventLogger";
import {peripheralService} from '@/_services/controls';
import Toggle from "@vueform/toggle";
import {CONFIGURATION_SET_VALUE, PERIPHERAL_GET_BY_ID} from "@/graphql/queries";
import _ from "lodash";
import humanizeDuration from 'humanize-duration';
import {useStore} from "vuex";
import {useApolloClient, useMutation} from "@vue/apollo-composable";
import {format} from "date-fns";

export default defineComponent({
  name: 'WaterPump',
  components: {
    Toggle,
    EventLogger
  },
  setup(props, {emit}) {
    const store = useStore();
    let asset = ref({})
    const {client} = useApolloClient();
    const wsMessage = computed(() => store?.getters?.ws?.message);
    const {mutate: setTimeout} = useMutation(CONFIGURATION_SET_VALUE, {
      update: () => {
        init();
      },
    });
    const deleteTimeout = (variables) => {
      // Implementation for deleting timeout configuration
      setTimeout({ ...variables, value: null });
    };
    const init = () => {
      client.query({
        query: PERIPHERAL_GET_BY_ID,
        variables: {id: process.env.WATER_PUMP_ID},
        fetchPolicy: 'network-only',
      }).then(response => {
        asset.value = peripheralService.peripheralInit(null, _.cloneDeep(response.data.devicePeripheral));
      });
    }
    const config = key => {
      if (asset.value != null) {
        return _.find(asset.value.configurations, function (cfg) {
          return cfg.key == key;
        });
      }
    }
    const stItems = [
      {value: 30},
      {value: 60},
      {value: 300},
      {value: 600},
      {value: 1800},
      {value: 3600},
      {value: 7200},
      {value: 10800},
      {value: 18000},
    ];
    watch(
      () => store?.getters?.ws?.message,
      function () {
        if (wsMessage.value?.eventName == 'evt_port_value_persisted') {
          let payload = JSON.parse(wsMessage.value.jsonPayload);
          if (asset.value?.data?.connectedTo?.[0]?.id == payload.p2) {
            asset.value['value'] = payload.p4;
            asset.value['state'] = payload.p4 === 'ON';
            asset.value['data']['state'] = payload.p4 === 'ON';
          }
        }
      },
    );
    onMounted(() => {
      init()
    });
    return {
      asset,
      humanizeDuration,
      peripheralService,
      passDialog: ref(false),
      peripheral: {id: process.env.WATER_PUMP_ID},
      stItems,
      setTimeout,
      deleteTimeout,
      config,
      format,

    };
  }
});
</script>
<style src="@vueform/toggle/themes/default.css"></style>
