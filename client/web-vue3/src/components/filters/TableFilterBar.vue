<template>
  <div class="row items-center q-gutter-sm">
    <template v-for="field in fields" :key="field.key">
      <q-select
        v-if="field.type === 'select'"
        :model-value="modelValue[field.key]"
        @update:model-value="val => update(field.key, val)"
        :options="selectOptions[field.key]"
        :label="field.label"
        dense
        outlined
        clearable
        options-dense
        style="min-width: 160px"
      />
      <q-input
        v-else
        :model-value="modelValue[field.key]"
        @update:model-value="val => update(field.key, val)"
        :label="field.label"
        dense
        outlined
        clearable
        debounce="300"
        style="min-width: 160px"
      />
    </template>
    <q-btn
      v-if="hasActive"
      flat
      dense
      no-caps
      icon="mdi-filter-remove-outline"
      label="Clear"
      color="grey-7"
      @click="clearAll"
    />
  </div>
</template>

<script>
import {computed, defineComponent} from 'vue';

const isActive = (value) => value != null && value !== '';

/**
 * Generic, reusable filter bar for tables.
 *
 * Renders a combobox (q-select) or text input (q-input) per field, driven by a
 * `fields` config. Select options are derived from the distinct values present
 * in `rows` (so only values that actually exist are offered) unless the field
 * provides explicit `options`. Controlled via v-model over a filter-state object.
 *
 * Pair with the `useTableFilters` composable, which owns the filter state and
 * produces the filtered rows.
 */
export default defineComponent({
  name: 'TableFilterBar',
  props: {
    // [{ key, label, type: 'select' | 'text', options? }]
    fields: {
      type: Array,
      required: true,
    },
    // Source rows used to derive select options (for non-explicit option lists)
    rows: {
      type: Array,
      default: () => [],
    },
    // Filter-state object: { [field.key]: value }
    modelValue: {
      type: Object,
      default: () => ({}),
    },
  },
  emits: ['update:modelValue'],
  setup(props, {emit}) {
    const update = (key, val) => {
      emit('update:modelValue', {...props.modelValue, [key]: val});
    };

    // Distinct, sorted option values per select field, derived from rows.
    const selectOptions = computed(() => {
      const map = {};
      props.fields.forEach(field => {
        if (field.type !== 'select') return;
        if (field.options) {
          map[field.key] = field.options;
          return;
        }
        const seen = new Set();
        (props.rows || []).forEach(row => {
          const value = row[field.key];
          if (isActive(value)) seen.add(value);
        });
        map[field.key] = Array.from(seen).sort((a, b) =>
          String(a).localeCompare(String(b))
        );
      });
      return map;
    });

    const hasActive = computed(() =>
      props.fields.some(field => isActive(props.modelValue[field.key]))
    );

    const clearAll = () => {
      const cleared = {...props.modelValue};
      props.fields.forEach(field => {
        cleared[field.key] = field.type === 'text' ? '' : null;
      });
      emit('update:modelValue', cleared);
    };

    return {update, selectOptions, hasActive, clearAll};
  },
});
</script>
