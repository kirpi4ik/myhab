<template>
  <div class="timeout-selector">
    <!-- Header -->
    <div class="timeout-header q-pa-md bg-grey-2">
      <div class="text-subtitle1 text-weight-medium text-grey-8">
        <q-icon name="mdi-timer-outline" size="20px" class="q-mr-sm"/>
        {{ $t('timeout_selector.title') }}
      </div>
      <div class="text-caption text-grey-6">{{ $t('timeout_selector.description') }}</div>
    </div>

    <q-separator/>

    <!-- Current Selection Indicator -->
    <div v-if="currentTimeout !== null && currentTimeout !== undefined" class="current-selection q-pa-sm bg-blue-1">
      <div class="row items-center no-wrap">
        <q-icon name="mdi-clock-check" color="primary" size="18px" class="q-mr-xs"/>
        <span class="text-caption text-primary">
          {{ $t('timeout_selector.current') }}: {{ formatDuration(currentTimeout * 1000) }}
        </span>
      </div>
    </div>

    <!-- Timeout Options Groups -->
    <q-list class="timeout-list">
      <!-- Quick Options -->
      <q-item-label header class="text-caption text-weight-medium text-grey-7 q-px-md">
        {{ $t('timeout_selector.quick_options') }}
      </q-item-label>
      
      <q-item
        v-for="item in quickOptions"
        :key="item.value"
        clickable
        v-ripple
        v-close-popup
        class="timeout-item"
        :class="{ 'is-active': currentTimeout === item.value }"
        @click="handleSetTimeout(item.value)"
      >
        <q-item-section avatar>
          <q-avatar :color="getAvatarColor(item.value)" text-color="white" size="32px">
            <q-icon :name="getIcon(item.value)" size="18px"/>
          </q-avatar>
        </q-item-section>
        <q-item-section>
          <q-item-label class="text-weight-medium">
            {{ formatDuration(item.value * 1000) }}
          </q-item-label>
          <q-item-label caption class="text-grey-6">
            {{ $t(getTimeDescription(item.value)) }}
          </q-item-label>
        </q-item-section>
        <q-item-section side v-if="currentTimeout === item.value">
          <q-icon name="mdi-check-circle" color="primary" size="20px"/>
        </q-item-section>
      </q-item>

      <q-separator spaced inset/>

      <!-- Extended Options -->
      <q-item-label header class="text-caption text-weight-medium text-grey-7 q-px-md">
        {{ $t('timeout_selector.extended_durations') }}
      </q-item-label>
      
      <q-item
        v-for="item in extendedOptions"
        :key="item.value"
        clickable
        v-ripple
        v-close-popup
        class="timeout-item"
        :class="{ 'is-active': currentTimeout === item.value }"
        @click="handleSetTimeout(item.value)"
      >
        <q-item-section avatar>
          <q-avatar :color="getAvatarColor(item.value)" text-color="white" size="32px">
            <q-icon :name="getIcon(item.value)" size="18px"/>
          </q-avatar>
        </q-item-section>
        <q-item-section>
          <q-item-label class="text-weight-medium">
            {{ formatDuration(item.value * 1000) }}
          </q-item-label>
          <q-item-label caption class="text-grey-6">
            {{ $t(getTimeDescription(item.value)) }}
          </q-item-label>
        </q-item-section>
        <q-item-section side v-if="currentTimeout === item.value">
          <q-icon name="mdi-check-circle" color="primary" size="20px"/>
        </q-item-section>
      </q-item>
    </q-list>

    <q-separator/>

    <!-- Remove Timeout -->
    <q-item
      clickable
      v-ripple
      v-close-popup
      class="remove-timeout-item"
      @click="handleDeleteTimeout"
    >
      <q-item-section avatar>
        <q-avatar color="red-1" text-color="negative" size="32px">
          <q-icon name="mdi-timer-off" size="18px"/>
        </q-avatar>
      </q-item-section>
      <q-item-section>
        <q-item-label class="text-weight-medium text-negative">
          {{ $t('timeout_selector.remove_timeout') }}
        </q-item-label>
        <q-item-label caption class="text-grey-6">
          {{ $t('timeout_selector.keep_running') }}
        </q-item-label>
      </q-item-section>
    </q-item>
  </div>
</template>

<script>
import { defineComponent, computed } from 'vue';
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
        { value: 900, label: '15 minutes' },
        { value: 1800, label: '30 minutes' },
        { value: 3600, label: '1 hour' },
        { value: 5400, label: '1 hour 30 minutes' },
        { value: 7200, label: '2 hours' },
        { value: 10800, label: '3 hours' },
        { value: 18000, label: '5 hours' },
      ]
    },
    /**
     * Current timeout value in seconds (for highlighting current selection)
     */
    currentTimeout: {
      type: Number,
      default: null
    }
  },
  emits: ['set-timeout', 'delete-timeout'],
  setup(props, { emit }) {
    /**
     * Split options into quick (< 1 hour) and extended (>= 1 hour)
     */
    const quickOptions = computed(() => {
      return props.timeoutOptions.filter(item => item.value < 3600);
    });

    const extendedOptions = computed(() => {
      return props.timeoutOptions.filter(item => item.value >= 3600);
    });

    /**
     * Format duration in human-readable format
     */
    const formatDuration = (milliseconds) => {
      return humanizeDuration(milliseconds, { 
        largest: 2, 
        language: 'en', 
        round: true,
        conjunction: ' ',
        serialComma: false
      });
    };

    /**
     * Get icon based on duration
     */
    const getIcon = (seconds) => {
      if (seconds < 60) return 'mdi-timer-sand';
      if (seconds < 600) return 'mdi-timer';
      if (seconds < 3600) return 'mdi-clock-outline';
      return 'mdi-clock-time-four-outline';
    };

    /**
     * Get avatar color based on duration
     */
    const getAvatarColor = (seconds) => {
      if (seconds < 300) return 'orange-6';
      if (seconds < 1800) return 'amber-6';
      if (seconds < 3600) return 'blue-6';
      return 'indigo-6';
    };

    /**
     * Get time description
     */
    const getTimeDescription = (seconds) => {
      if (seconds < 300) return 'timeout_selector.durations.quick';
      if (seconds < 1800) return 'timeout_selector.durations.short';
      if (seconds < 3600) return 'timeout_selector.durations.medium';
      return 'timeout_selector.durations.long';
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
      quickOptions,
      extendedOptions,
      formatDuration,
      getIcon,
      getAvatarColor,
      getTimeDescription,
      handleSetTimeout,
      handleDeleteTimeout,
    };
  },
});
</script>

<style scoped lang="scss">
.timeout-selector {
  min-width: 320px;
  max-width: 400px;

  .timeout-header {
    border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  }

  .current-selection {
    border-left: 3px solid var(--q-primary);
    animation: fadeIn 0.3s ease-in-out;
  }

  .timeout-list {
    max-height: 400px;
    overflow-y: auto;
    
    /* Custom scrollbar */
    &::-webkit-scrollbar {
      width: 6px;
    }
    
    &::-webkit-scrollbar-track {
      background: #f1f1f1;
    }
    
    &::-webkit-scrollbar-thumb {
      background: #888;
      border-radius: 3px;
      
      &:hover {
        background: #555;
      }
    }
  }

  .timeout-item {
    transition: all 0.2s ease;
    
    &:hover {
      background-color: rgba(0, 0, 0, 0.03);
      
      .q-avatar {
        transform: scale(1.05);
        transition: transform 0.2s ease;
      }
    }

    &.is-active {
      background-color: rgba(25, 118, 210, 0.08);
      border-left: 3px solid var(--q-primary);
    }

    &:active {
      background-color: rgba(0, 0, 0, 0.05);
    }
  }

  .remove-timeout-item {
    transition: all 0.2s ease;
    background-color: rgba(244, 67, 54, 0.02);

    &:hover {
      background-color: rgba(244, 67, 54, 0.08);

      .q-avatar {
        transform: scale(1.05);
        transition: transform 0.2s ease;
      }
    }

    &:active {
      background-color: rgba(244, 67, 54, 0.12);
    }
  }

  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(-5px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
}
</style>
