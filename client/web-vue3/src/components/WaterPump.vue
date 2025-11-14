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
                    <!-- Timeout Selector -->
                    <timeout-selector
                      @set-timeout="handleSetTimeout"
                      @delete-timeout="handleDeleteTimeout"
                    />
                    
                    <q-separator/>
                    
                    <!-- View Details -->
                    <q-item clickable v-close-popup
                            @click="$router.push({ path: '/admin/peripherals/' + asset.id + '/view' })">
                      <q-item-section avatar>
                        <q-icon name="mdi-information" color="info"/>
                      </q-item-section>
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
                @change="handleToggle"/>
      </div>
    </q-card-section>
  </q-card>
</template>
<script>
import {computed, defineComponent, onMounted, ref} from 'vue';

import {useApolloClient, useMutation} from "@vue/apollo-composable";
import {useWebSocketListener} from "@/composables";

import {CONFIGURATION_SET_VALUE, PERIPHERAL_GET_BY_ID} from "@/graphql/queries";
import {peripheralService} from '@/_services/controls';

import _ from "lodash";
import {format} from "date-fns";
import EventLogger from "components/EventLogger";
import humanizeDuration from 'humanize-duration';
import Toggle from "@vueform/toggle";
import TimeoutSelector from "components/TimeoutSelector.vue";

export default defineComponent({
  name: 'WaterPump',
  components: {
    Toggle,
    EventLogger,
    TimeoutSelector
  },
  setup(props, {emit}) {
    let asset = ref({})
    const {client} = useApolloClient();
    const {mutate: setConfigValue} = useMutation(CONFIGURATION_SET_VALUE, {
      update: () => {
        init();
      },
    });
    
    const handleSetTimeout = (timeoutValue) => {
      setConfigValue({
        key: 'key.on.timeout',
        value: timeoutValue,
        entityId: asset.value.id,
        entityType: 'PERIPHERAL'
      });
    };

    const handleDeleteTimeout = () => {
      setConfigValue({
        key: 'key.on.timeout',
        value: null,
        entityId: asset.value.id,
        entityType: 'PERIPHERAL'
      });
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
    };
    
    // Listen for port value updates
    useWebSocketListener('evt_port_value_persisted', (payload) => {
      if (asset.value?.data?.connectedTo?.[0]?.id == payload.p2) {
        asset.value['value'] = payload.p4;
        asset.value['state'] = payload.p4 === 'ON';
        asset.value['data']['state'] = payload.p4 === 'ON';
      }
    });
    
    const handleToggle = () => {
      peripheralService.toggle(asset.value, 'evt_light');
    };

    onMounted(() => {
      init()
    });
    
    return {
      asset,
      humanizeDuration,
      peripheralService,
      passDialog: ref(false),
      peripheral: {id: process.env.WATER_PUMP_ID},
      handleSetTimeout,
      handleDeleteTimeout,
      config,
      format,
      handleToggle,
    };
  }
});

</script>
<style src="@vueform/toggle/themes/default.css"></style>
