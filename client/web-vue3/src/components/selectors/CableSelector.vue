<template>
  <q-select 
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :options="filteredCables"
    :disable="disabled || loading"
    :loading="loading"
    input-debounce="0"
    @filter="filterCables"
    :option-label="opt => {
      if (!opt) return '';
      const parts = [];
      if (opt.id) parts.push(opt.id);
      if (opt.code) parts.push(opt.code);
      if (opt.description) parts.push(opt.description);
      return parts.join(' | ') || '';
    }"
    map-options
    stack-label
    use-chips
    use-input
    filled
    dense
    multiple
    :label="label"
    :hint="hint"
    clearable
  >
    <template v-slot:prepend>
      <q-icon name="mdi-cable-data"/>
    </template>
  </q-select>
</template>

<script>
import { defineComponent, ref, onMounted } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { CABLE_LIST_ALL } from '@/graphql/queries';
import _ from 'lodash';

export default defineComponent({
  name: 'CableSelector',
  props: {
    /**
     * Selected cables (v-model)
     */
    modelValue: {
      type: Array,
      default: () => []
    },
    /**
     * Label for the selector
     */
    label: {
      type: String,
      default: 'Cables'
    },
    /**
     * Hint text
     */
    hint: {
      type: String,
      default: 'Select cables connected to this peripheral'
    },
    /**
     * Disable the selector
     */
    disabled: {
      type: Boolean,
      default: false
    },
    /**
     * Auto-load cables on mount
     */
    autoLoad: {
      type: Boolean,
      default: true
    }
  },
  emits: ['update:modelValue'],
  setup(props) {
    const { client } = useApolloClient();
    const cables = ref([]);
    const filteredCables = ref([]);
    const loading = ref(false);

    /**
     * Load cables from backend
     */
    const loadCables = async () => {
      loading.value = true;
      try {
        const response = await client.query({
          query: CABLE_LIST_ALL,
          variables: {},
          fetchPolicy: 'network-only',
        });

        cables.value = _.transform(response.data.cableList,
          function (result, value) {
            result.push({
              id: value.id,
              code: value.code,
              codeNew: value.codeNew,
              codeOld: value.codeOld,
              description: value.description,
              category: value.category
            });
          });
        filteredCables.value = [...cables.value];
      } catch (error) {
        console.error('Error loading cables:', error);
      } finally {
        loading.value = false;
      }
    };

    /**
     * Filter cables based on search input
     * Searches across id, code, codeOld, codeNew, and description
     */
    const filterCables = (val, update) => {
      if (val === '') {
        update(() => {
          filteredCables.value = cables.value;
        });
        return;
      }

      update(() => {
        const needle = val.toLowerCase();
        filteredCables.value = cables.value.filter(
          v => 
            String(v.id).toLowerCase().includes(needle) ||
            (v.code && v.code.toLowerCase().includes(needle)) ||
            (v.codeOld && v.codeOld.toLowerCase().includes(needle)) ||
            (v.codeNew && v.codeNew.toLowerCase().includes(needle)) ||
            (v.description && v.description.toLowerCase().includes(needle))
        );
      });
    };

    onMounted(() => {
      if (props.autoLoad) {
        loadCables();
      }
    });

    return {
      cables,
      filteredCables,
      loading,
      filterCables,
      loadCables
    };
  }
});
</script>

