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
          <q-card-section class="bg-blue-grey-3 text-grey-3 text-h6" vertical>
            <span>38 </span><span class="text-overline text-grey-4">KW/24h</span>
          </q-card-section>
        </q-card>
      </div>
      &nbsp;
      <div class="row text-center">
        <q-card class="col">
          <q-card-section class="bg-blue-grey-4 " vertical>
            <span class="text-grey-3 text-h6">54543 KW/h </span><span class="text-overline text-grey-5">index</span>
          </q-card-section>
        </q-card>
      </div>
      &nbsp;
      <div class="row text-center">
        <q-card class="col">
          <q-card-section class="bg-grey-7 text-grey-2 text-h6">
            <span>
                <q-icon name="mdi-alpha-a-circle-outline" size="md" class="text-grey-6"/>
                <span class="text-amber-9">3.0</span>
            </span>
            <q-separator/>
            <span>
                <q-icon name="mdi-alpha-v-circle-outline" size="md" class="text-grey-6"/>
                <span class="text-deep-orange-5">220</span>
            </span>
          </q-card-section>
        </q-card>
        &nbsp;
        <q-card class="col">
          <q-card-section class="bg-grey-7 text-grey-2 text-h6">
            <span>
                <q-icon name="mdi-alpha-a-circle-outline" size="md" class="text-grey-6"/>
                <span class="text-amber-9">3.0</span>
            </span>
            <q-separator/>
            <span>
                <q-icon name="mdi-alpha-v-circle-outline" size="md" class="text-grey-6"/>
                <span class="text-deep-orange-5">220</span>
            </span>
          </q-card-section>
        </q-card>
        &nbsp;
        <q-card class="col">
          <q-card-section class="bg-grey-7 text-grey-2 text-h6">
            <span>
                <q-icon name="mdi-alpha-a-circle-outline" size="md" class="text-grey-6"/>
                <span class="text-amber-9">3.0</span>
            </span>
            <q-separator/>
            <span>
                <q-icon name="mdi-alpha-v-circle-outline" size="md" class="text-grey-6"/>
                <span class="text-deep-orange-5">220</span>
            </span>
          </q-card-section>
        </q-card>
      </div> &nbsp;
      <div class="row">
        <q-card class="col">
          <q-card-section class="bg-blue-grey-4 text-grey-2" vertical>
            <q-icon name="mdi-transmission-tower"/>

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
  name: 'ElectricMeter',
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
