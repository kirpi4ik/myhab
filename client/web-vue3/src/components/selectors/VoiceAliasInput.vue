<template>
  <q-select
    ref="selectRef"
    :model-value="modelValue"
    @update:model-value="val => $emit('update:modelValue', val || [])"
    :label="label"
    :hint="hint"
    multiple
    use-input
    use-chips
    hide-dropdown-icon
    input-debounce="0"
    color="orange"
    filled
    dense
    stack-label
    clearable
    @new-value="onNewValue"
    @input-value="onInputValue"
    @focusout="commitPending"
  >
    <template v-slot:prepend>
      <q-icon name="mdi-bullhorn-variant-outline"/>
    </template>
  </q-select>
</template>

<script>
import { defineComponent, ref } from 'vue';

/**
 * Voice aliases as chips. Enter, a comma or leaving the field turns the typed text into
 * chips; pasted comma-separated text becomes one chip per alias.
 */
export default defineComponent({
  name: 'VoiceAliasInput',
  props: {
    modelValue: { type: Array, default: () => [] },
    label: { type: String, default: 'Voice aliases' },
    hint: { type: String, default: 'Alternate names for voice control — press Enter or comma after each' }
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const selectRef = ref(null);
    let pending = '';

    const add = (text) => {
      const parts = (text || '').split(',').map(s => s.trim()).filter(Boolean);
      if (parts.length === 0) return;
      const next = [...props.modelValue];
      parts.forEach(p => {
        if (!next.some(a => a.toLowerCase() === p.toLowerCase())) next.push(p);
      });
      emit('update:modelValue', next);
    };

    const onNewValue = (val, done) => {
      add(val);
      pending = '';
      done();
    };

    const onInputValue = (val) => {
      pending = val;
      if (val.includes(',')) {
        add(val);
        pending = '';
        selectRef.value?.updateInputValue('', true);
      }
    };

    const commitPending = () => {
      if (!pending.trim()) return;
      add(pending);
      pending = '';
      selectRef.value?.updateInputValue('', true);
    };

    return { selectRef, onNewValue, onInputValue, commitPending };
  }
});
</script>
