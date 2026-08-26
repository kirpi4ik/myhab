<template>
  <peripheral-valve-card v-if="peripheral" :peripheral="peripheral"/>
  <q-skeleton v-else type="rect" height="140px"/>
</template>

<script>
import {defineComponent, onMounted, ref} from 'vue';

import {useApolloClient} from '@vue/apollo-composable';

import {PERIPHERAL_GET_BY_ID} from '@/graphql/queries';
import {peripheralService} from '@/_services/controls';

import PeripheralValveCard from 'components/cards/PeripheralValveCard.vue';

/**
 * Dashboard wrapper around the zone view's valve card: the card takes a peripheral
 * object, the dashboard only knows a configured id (`specialDevices.waterValve.peripheral.id`).
 *
 * The card re-reads the peripheral itself on mount; this first read exists so the
 * tile shows the valve's real name and state instead of a placeholder for the
 * length of that round-trip.
 */
export default defineComponent({
  name: 'ValveWidget',
  components: {PeripheralValveCard},
  props: {
    peripheralId: {
      type: Number,
      required: true
    }
  },
  setup(props) {
    const {client} = useApolloClient();
    const peripheral = ref(null);

    const load = async () => {
      try {
        const {data} = await client.query({
          query: PERIPHERAL_GET_BY_ID,
          variables: {id: props.peripheralId},
          fetchPolicy: 'network-only',
        });
        peripheral.value = peripheralService.peripheralInit(null, data.devicePeripheral);
      } catch (error) {
        console.error('Failed to load valve peripheral:', error);
      }
    };

    onMounted(load);

    return {peripheral};
  }
});
</script>
