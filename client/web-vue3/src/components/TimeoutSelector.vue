<template>
  <div>
    <!-- Timeout Options -->
    <q-item-label header class="text-weight-bold">Set Timeout</q-item-label>
    <q-item
      v-for="item in timeoutOptions"
      :key="item.value"
      clickable
      v-close-popup
      @click="handleSetTimeout(item.value)"
    >
      <q-item-section>
        <q-item-label>{{ formatDuration(item.value * 1000) }}</q-item-label>
      </q-item-section>
    </q-item>

    <q-separator/>

    <!-- Delete Timeout -->
    <q-item
      clickable
      v-close-popup
      @click="handleDeleteTimeout"
    >
      <q-item-section avatar>
        <q-icon name="mdi-timer-off" color="negative"/>
      </q-item-section>
      <q-item-section>
        <q-item-label>Remove Timeout</q-item-label>
      </q-item-section>
    </q-item>
  </div>
</template>

<script>
import { defineComponent } from 'vue';
import humanizeDuration from 'humanize-duration';

export default defineComponent({
  name: 'TimeoutSelector',
  props: {
    /**
     * Array of timeout options in seconds
     * Each option should have a 'value' property (in seconds)
     */
    timeoutOptions: {
      type: Array,
      default: () => [
        { value: 30, label: '30 seconds' },
        { value: 60, label: '1 minute' },
        { value: 300, label: '5 minutes' },
        { value: 600, label: '10 minutes' },
        { value: 1800, label: '30 minutes' },
        { value: 3600, label: '1 hour' },
        { value: 7200, label: '2 hours' },
        { value: 10800, label: '3 hours' },
        { value: 18000, label: '5 hours' },
      ]
    }
  },
  emits: ['set-timeout', 'delete-timeout'],
  setup(props, { emit }) {
    /**
     * Format duration in human-readable format
     */
    const formatDuration = (milliseconds) => {
      return humanizeDuration(milliseconds, { largest: 2, language: 'en', round: true });
    };

    /**
     * Handle set timeout
     */
    const handleSetTimeout = (timeoutValue) => {
      emit('set-timeout', timeoutValue);
    };

    /**
     * Handle delete timeout
     */
    const handleDeleteTimeout = () => {
      emit('delete-timeout');
    };

    return {
      formatDuration,
      handleSetTimeout,
      handleDeleteTimeout,
    };
  },
});
</script>

