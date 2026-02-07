<template>
  <q-select 
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :options="filteredZones"
    :disable="disabled || loading"
    :loading="loading"
    input-debounce="0"
    @filter="filterZones"
    :option-label="opt => opt && opt.name ? opt.name : ''"
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
      <q-icon name="mdi-map-marker"/>
    </template>
  </q-select>
</template>

<script>
import { defineComponent, ref, onMounted } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { ZONES_GET_ALL } from '@/graphql/queries';
import _ from 'lodash';

export default defineComponent({
  name: 'LocationSelector',
  props: {
    /**
     * Selected zones (v-model)
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
      default: 'Zones'
    },
    /**
     * Hint text
     */
    hint: {
      type: String,
      default: 'Select zones where this item is located'
    },
    /**
     * Disable the selector
     */
    disabled: {
      type: Boolean,
      default: false
    },
    /**
     * Auto-load zones on mount
     */
    autoLoad: {
      type: Boolean,
      default: true
    }
  },
  emits: ['update:modelValue'],
  setup(props) {
    const { client } = useApolloClient();
    const zones = ref([]);
    const filteredZones = ref([]);
    const loading = ref(false);

    /**
     * Load zones from backend
     */
    const loadZones = async () => {
      loading.value = true;
      try {
        const response = await client.query({
          query: ZONES_GET_ALL,
          variables: {},
          fetchPolicy: 'network-only',
        });

        zones.value = _.transform(response.data.zoneList,
          function (result, value) {
            result.push({
              id: value.id,
              name: value.name,
              description: value.description
            });
          });
        filteredZones.value = [...zones.value];
      } catch (error) {
        console.error('Error loading zones:', error);
      } finally {
        loading.value = false;
      }
    };

    /**
     * Filter zones based on search input
     */
    const filterZones = (val, update) => {
      if (val === '') {
        update(() => {
          filteredZones.value = zones.value;
        });
        return;
      }

      update(() => {
        const needle = val.toLowerCase();
        filteredZones.value = zones.value.filter(
          v => v.name.toLowerCase().indexOf(needle) > -1
        );
      });
    };

    onMounted(() => {
      if (props.autoLoad) {
        loadZones();
      }
    });

    return {
      zones,
      filteredZones,
      loading,
      filterZones,
      loadZones
    };
  }
});
</script>

