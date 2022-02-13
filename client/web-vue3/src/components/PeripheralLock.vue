<template>
  <q-card class="q-ma-md-xs" style="background-color: #a0d299">
    <q-card-section class="bg-teal-5 text-amber-1 text-h6">
      <event-logger :peripheral="peripheral"/>
      Poarta mica
      <q-icon name="fas fa-door-open" class="float-right" size="40px"/>
    </q-card-section>
    <q-separator color="white"/>
    <q-card-section class="q-pa-none" vertical align="center">
      <div class="q-pa-sm">
        <q-btn flat class="text-h6" icon="fas fa-lock-open" label="Deschide" no-caps @click="passDialog = true"></q-btn>
      </div>
    </q-card-section>
  </q-card>
  <q-dialog v-model="passDialog" transition-show="jump-up" transition-hide="jump-down">
    <q-card class="bg-white">
      <q-bar class="bg-deep-orange-7 text-white">
        <q-icon name="lock"/>
        <div>Alert</div>
        <q-space/>
        <q-btn dense flat icon="close" v-close-popup>
          <q-tooltip class="bg-primary">Close</q-tooltip>
        </q-btn>
      </q-bar>

      <q-card-section>
        <div class="text-h6">Atentie! Doriti sa deschideti ?</div>
      </q-card-section>

      <q-card-section class="q-pa-none" vertical align="center">
        <div class="q-pa-sm">
          <q-btn flat class="text-h6" icon="fas fa-lock-open" label="Deschide" no-caps @click="open"></q-btn>
        </div>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>
<script>
  import EventLogger from 'components/EventLogger.vue';
  import {defineComponent, ref} from 'vue';
  import {PERIPHERAL_GET_BY_ID, PUSH_EVENT} from '@/graphql/queries';
  import {authenticationService} from '@/_services';
  import _ from 'lodash';

  export default defineComponent({
    name: 'PeripheralLock',
    components: {
      EventLogger
    },
    setup() {
      return {
        passDialog: ref(false),
        peripheral: {}
      };
    },
    mounted() {
      this.init();
    },
    methods: {
      init() {
        this.$apollo
          .query({
            query: PERIPHERAL_GET_BY_ID,
            variables: {id: 3599655},
            fetchPolicy: 'network-only',
          })
          .then(response => {
            let data = _.cloneDeep(response.data);
            this.peripheral = data.devicePeripheral;
            this.peripheral.deviceState = this.peripheral.connectedTo[0].device.status;
          });
      },
      open() {
        let event = {
          p0: 'evt_intercom_door_lock',
          p1: 'PERIPHERAL',
          p2: this.peripheral.id,
          p3: 'mweb',
          p4: 'open',
          p5: "{'unlocked'}",
          p6: authenticationService.currentUserValue.login,
        };
        this.$apollo
          .mutate({
            mutation: PUSH_EVENT,
            variables: {input: event},
          })
          .then(resp => {
            this.passDialog = false;
            console.log('Unlock');
          });
      },
    },
  });
</script>
