/**
 * Composable for structured, per-field table filtering.
 *
 * Layers on top of a rows ref (e.g. the text-filtered `filteredItems` from
 * useEntityList) and AND-combines a set of per-field filters:
 *   - type 'select' : exact match on the row field
 *   - type 'text'   : case-insensitive substring match on the row field
 *
 * `filters` is a ref holding a plain state object, so a parent can bind it with
 * `v-model="filters"` on <TableFilterBar> (controlled component, no prop mutation).
 *
 * @param {import('vue').Ref<Array>} rowsRef - reactive list of rows to filter
 * @param {Array<{key: string, label: string, type: 'select'|'text', options?: Array}>} fields
 * @returns {{ filters: import('vue').Ref<Object>, filteredRows: import('vue').ComputedRef<Array>, activeCount: import('vue').ComputedRef<number>, clear: Function }}
 */
import { ref, computed } from 'vue';

const isActive = (value) => value != null && value !== '';

const initialState = (fields) =>
  Object.fromEntries(fields.map(f => [f.key, f.type === 'text' ? '' : null]));

export function useTableFilters(rowsRef, fields) {
  const filters = ref(initialState(fields));

  const filteredRows = computed(() =>
    (rowsRef.value || []).filter(row =>
      fields.every(f => {
        const value = filters.value[f.key];
        if (!isActive(value)) return true; // inactive filter passes everything
        const cell = row[f.key];
        if (f.type === 'text') {
          return String(cell ?? '').toLowerCase().includes(String(value).toLowerCase());
        }
        return cell === value; // select: exact match
      })
    )
  );

  const activeCount = computed(() =>
    fields.filter(f => isActive(filters.value[f.key])).length
  );

  const clear = () => {
    filters.value = initialState(fields);
  };

  return { filters, filteredRows, activeCount, clear };
}
