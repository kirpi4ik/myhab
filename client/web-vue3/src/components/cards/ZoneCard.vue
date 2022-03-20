<template>
  <q-card class="text-white zone-card" style="cursor: pointer" @click="router.push({ name: 'zoneById', params: { zoneId: `${zone.id}` }, query: { category: route.query.category } })">
    <q-item>
      <q-item-section v-ripple>
        <q-item-label class="text-weight-bold text-h5">{{ zone.name }}</q-item-label>
      </q-item-section>
      <q-item-section side>
        <q-item-label>
          <heat-scheduler
            :zone="zone"
            v-if="authzService.hasAnyRole([Role.User,Role.Admin]) && ['HEAT', 'TEMP'].includes(route.query.category)"
          />
        </q-item-label>
        <q-item-label>
          <temp-display :zone="zone" :name="zone.name" v-if="['HEAT', 'TEMP'].includes(route.query.category)"/>
        </q-item-label>
      </q-item-section>
    </q-item>
  </q-card>
</template>
<script>
  import {defineComponent} from 'vue';
  import {authzService} from '@/_services';
  import {Role} from '@/_helpers';
  import {useRoute, useRouter} from 'vue-router';
  import HeatScheduler from '@/components/HeatScheduler';
  import TempDisplay from '@/components/TempDisplay';

  export default defineComponent({
    name: 'ZoneCard',
    props: {
      zone: Object,
    },
    components: {
      HeatScheduler,
      TempDisplay
    },
    setup(props) {
      const router = useRouter();
      const route = useRoute();
      return {router, route, authzService, Role};
    },
  });
</script>
