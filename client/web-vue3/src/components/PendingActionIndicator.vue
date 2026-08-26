<template>
  <div v-if="pending" class="pending-action">
    <q-linear-progress
      :value="progress"
      size="4px"
      color="white"
      track-color="rgba(255, 255, 255, 0.18)"
      rounded
    />
    <div class="pending-action__label">
      <q-spinner-dots size="16px" class="q-mr-xs"/>
      {{ label }}
    </div>
  </div>

  <div v-else-if="unconfirmed" class="unconfirmed-action">
    <q-icon name="mdi-alert-outline" size="16px" class="q-mr-xs"/>
    {{ warning }}
  </div>
</template>

<script>
import {defineComponent} from 'vue';

/**
 * Shows that a command was sent and is waiting on the device, or that it was never
 * confirmed. Driven by `usePendingCommand` — this component holds no timing of its own.
 *
 * Rendered by every control that switches a peripheral, so the wait looks the same
 * wherever the command was sent from. Labels are props rather than i18n lookups because
 * the public share pages are not translated.
 */
export default defineComponent({
  name: 'PendingActionIndicator',
  props: {
    pending: {type: Boolean, default: false},
    unconfirmed: {type: Boolean, default: false},
    /** 0..1 across the command's window. */
    progress: {type: Number, default: 0},
    label: {type: String, default: 'Sending…'},
    warning: {type: String, default: 'No response from the device'}
  }
});
</script>

<style scoped lang="scss">
.pending-action {
  width: 100%;
  max-width: 340px;
  animation: fadeIn 0.2s ease-out;
}

.pending-action__label {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 4px;
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.5px;
  text-transform: uppercase;
  opacity: 0.85;
}

.unconfirmed-action {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px 12px;
  border-radius: 8px;
  background: rgba(217, 119, 6, 0.2);
  border: 1px solid rgba(251, 191, 36, 0.45);
  color: #fde68a;
  font-size: 0.75rem;
  font-weight: 600;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
