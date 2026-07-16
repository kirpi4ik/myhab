<template>
  <q-select
    :model-value="modelValue"
    :options="filteredOptions"
    :label="label"
    use-input
    clearable
    dense
    outlined
    emit-value
    input-debounce="200"
    :loading="loading"
    @update:model-value="$emit('update:modelValue', $event)"
    @filter="onFilter"
  >
    <template #prepend>
      <q-icon :name="modelValue || 'mdi-shape-outline'" />
    </template>
    <template #option="scope">
      <q-item v-bind="scope.itemProps" dense>
        <q-item-section avatar>
          <q-icon :name="scope.opt" size="20px" />
        </q-item-section>
        <q-item-section>{{ scope.opt }}</q-item-section>
      </q-item>
    </template>
    <template #no-option>
      <q-item dense>
        <q-item-section class="text-grey">No matching icon</q-item-section>
      </q-item>
    </template>
  </q-select>
</template>

<script setup>
import { ref } from 'vue';
import { mdiExportToName } from '@/_helpers/screen-widget';

/**
 * Searchable picker over the full Material Design Icons catalog (the mdi
 * webfont is already bundled via quasar extras, so previews are free).
 * v-model holds the icon name ('mdi-lightbulb') or null (= inherit default).
 */
defineProps({
  modelValue: { type: String, default: null },
  label: { type: String, default: 'Icon' },
});

defineEmits(['update:modelValue']);

const MAX_OPTIONS = 100;
const loading = ref(false);
const filteredOptions = ref([]);
let allNames = null; // lazily built from the mdi svg module keys

const ensureCatalog = async () => {
  if (allNames) return;
  loading.value = true;
  try {
    const module = await import('@quasar/extras/mdi-v6');
    allNames = Object.keys(module)
      .filter((key) => key.startsWith('mdi'))
      .map(mdiExportToName);
  } finally {
    loading.value = false;
  }
};

const onFilter = (val, update) => {
  ensureCatalog().then(() => {
    update(() => {
      const needle = (val || '').toLowerCase();
      const matches = needle ? allNames.filter((name) => name.includes(needle)) : allNames;
      filteredOptions.value = matches.slice(0, MAX_OPTIONS);
    });
  });
};
</script>
