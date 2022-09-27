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
            <q-icon name="mdi-sun-thermometer" size="md" class="text-orange-8" left/>
            <span class="text-light-green-1">17 </span><span class="text-h5 text-light-green-7">°C</span>
          </q-card-section>
        </q-card>
      </div>
      &nbsp;
      <div class="row text-center">
        <q-card class="col">
          <q-card-section class="bg-light-green-4 " vertical>
            <q-icon name="mdi-water-thermometer" size="md" class="text-blue-8" left/>
            <span class="text-light-green-1">52 </span><span class="text-h5 text-light-green-7">°C</span>
          </q-card-section>
        </q-card>
      </div>
      &nbsp;
      <div class="row text-center">
        <q-card class="col">
          <q-card-section class="bg-light-green-3 text-h6" vertical>
            <q-icon name="mdi-home-thermometer-outline" size="md" class="text-light-blue-4"/>
            <span class="text-light-green-1">17 </span><span class="text-h5 text-light-green-7">°C</span>
          </q-card-section>
        </q-card>
      </div>
      &nbsp;
      <div class="row text-center">
        <q-card class="col">
          <q-card-section class="bg-light-green-7 text-light-green-2 text-h6">
            <span>
                <q-icon name="mdi-thermometer-lines" size="md" class="text-light-green-4"/>
                <span class="text-amber-5">22 °C</span>
            </span>
            <q-separator/>
            <span>
                <q-icon name="mdi-alpha-v-circle-outline" size="md" class="text-light-green-4"/>
                <span class="text-deep-orange-3">220</span>
            </span>
          </q-card-section>
        </q-card>
        &nbsp;
        <q-card class="col">
          <q-card-section class="bg-light-green-7 text-light-green-2 text-h6">
            <span>
                <q-icon name="mdi-alpha-a-circle-outline" size="md" class="text-light-green-4"/>
                <span class="text-amber-5">3.0</span>
            </span>
            <q-separator/>
            <span>
                <q-icon name="mdi-alpha-v-circle-outline" size="md" class="text-light-green-4"/>
                <span class="text-deep-orange-3">220</span>
            </span>
          </q-card-section>
        </q-card>
        &nbsp;
        <q-card class="col">
          <q-card-section class="bg-light-green-7 text-light-green-2 text-h6">
            <span>
                <q-icon name="mdi-alpha-a-circle-outline" size="md" class="text-light-green-4"/>
                <span class="text-amber-5">3.0</span>
            </span>
            <q-separator/>
            <span>
                <q-icon name="mdi-alpha-v-circle-outline" size="md" class="text-light-green-4"/>
                <span class="text-deep-orange-3">220</span>
            </span>
          </q-card-section>
        </q-card>
      </div>
    </q-card-section>
  </q-card>
</template>
<script>
import {defineComponent, ref} from 'vue';
import {PERIPHERAL_GET_BY_ID, PUSH_EVENT} from '@/graphql/queries';
import {authzService} from '@/_services';
import _ from 'lodash';

export default defineComponent({
  name: 'HeatPump',
  components: {},
  setup() {
    return {
      peripheral: {id: process.env.DOOR_LOCK_ID},
    };
  },

});
</script>
<style>


</style>
