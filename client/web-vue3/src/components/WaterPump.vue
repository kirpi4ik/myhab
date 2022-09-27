<template>
  <q-card class="q-ma-md-xs bg-blue-4" >
    <q-card-section class="bg-blue-6 text-blue-1 text-h6">
      <event-logger :peripheral="asset"/>
      Pompa apa
      <q-icon name="mdi-water-pump" class="float-right" size="40px"/>
    </q-card-section>
    <q-separator color="white"/>
    <q-card-section>
      <div class="q-pa-sm text-grey-8">
        <toggle v-model="asset['state']"/>
      </div>
    </q-card-section>
  </q-card>
</template>
<script>
import {defineComponent, toRefs} from 'vue';
import EventLogger from "components/EventLogger";
import Toggle from "@vueform/toggle";

export default defineComponent({
  name: 'WaterPump',
  components: {
    Toggle,
    EventLogger
  },
  setup(props, {emit}) {
    let asset = {
      state: true
    }
    return {
      asset,
    };
  },
});
</script>
<style src="@vueform/toggle/themes/default.css"></style>
