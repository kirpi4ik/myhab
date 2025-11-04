<template>
  <q-card 
    class="zone-card text-white" 
    @click="navigateToZone"
  >
    <q-item clickable v-ripple>
      <q-item-section>
        <q-item-label class="text-weight-bold text-h5">
          {{ zone?.name || 'Unknown Zone' }}
        </q-item-label>
        <q-item-label v-if="zone?.description" caption class="text-blue-grey-3">
          {{ zone.description }}
        </q-item-label>
      </q-item-section>

      <q-item-section side>
        <!-- Heat Scheduler (for HEAT/TEMP categories) -->
        <q-item-label v-if="showHeatScheduler">
          <heat-scheduler :zone="zone" />
        </q-item-label>

        <!-- Temperature Display (for HEAT/TEMP categories) -->
        <q-item-label v-if="showTempDisplay">
          <temp-display :zone="zone" :name="zone.name" />
        </q-item-label>
      </q-item-section>
    </q-item>
  </q-card>
</template>
<script>
import {computed, defineComponent} from 'vue';
import {useRoute, useRouter} from 'vue-router';

import {authzService} from '@/_services';
import {Role} from '@/_helpers';
import HeatScheduler from '@/components/HeatScheduler';
import TempDisplay from '@/components/TempDisplay';

export default defineComponent({
  name: 'ZoneCard',
  components: {
    HeatScheduler,
    TempDisplay
  },
  props: {
    zone: {
      type: Object,
      required: true
    }
  },
  setup(props) {
    const router = useRouter();
    const route = useRoute();

    /**
     * Check if current category is HEAT or TEMP
     */
    const isHeatOrTempCategory = computed(() => {
      const category = route.query.category;
      return ['HEAT', 'TEMP'].includes(category);
    });

    /**
     * Check if user has permission to see heat scheduler
     */
    const hasSchedulerPermission = computed(() => {
      return authzService.hasAnyRole([Role.User, Role.Admin]);
    });

    /**
     * Show heat scheduler if user has permission and category is HEAT/TEMP
     */
    const showHeatScheduler = computed(() => {
      return hasSchedulerPermission.value && isHeatOrTempCategory.value;
    });

    /**
     * Show temperature display for HEAT/TEMP categories
     */
    const showTempDisplay = computed(() => {
      return isHeatOrTempCategory.value;
    });

    /**
     * Navigate to zone details page
     */
    const navigateToZone = () => {
      if (!props.zone?.id) {
        console.warn('Cannot navigate: zone ID is missing');
        return;
      }

      router.push({
        name: 'zoneById',
        params: { zoneId: String(props.zone.id) },
        query: { category: route.query.category }
      });
    };

    return {
      showHeatScheduler,
      showTempDisplay,
      navigateToZone
    };
  }
});

</script>

<style scoped>
.zone-card {  
  background: url('~assets/layer-1.png'), linear-gradient(#546e82, #7c919f);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  cursor: pointer;
}

</style>
