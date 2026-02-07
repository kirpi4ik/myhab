<template>
  <q-select 
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :options="filteredPorts"
    :disable="disabled || loading"
    :loading="loading"
    input-debounce="0"
    @filter="filterPorts"
    :option-label="opt => {
      if (!opt) return '';
      const parts = [];
      if (opt.id) parts.push(opt.id);
      if (opt.internalRef) parts.push(opt.internalRef);
      if (opt.name) parts.push(opt.name);
      if (opt.device?.name) parts.push(opt.device.name);
      return parts.join(' | ') || '';
    }"
    map-options
    option-value="id"
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
      <q-icon name="mdi-ethernet"/>
    </template>
  </q-select>
</template>

<script>
import { defineComponent, ref, onMounted } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { PORT_LIST_ALL } from '@/graphql/queries';
import _ from 'lodash';

export default defineComponent({
  name: 'PortSelector',
  props: {
    /**
     * Selected ports (v-model)
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
      default: 'Ports'
    },
    /**
     * Hint text
     */
    hint: {
      type: String,
      default: 'Select ports for this scenario'
    },
    /**
     * Disable the selector
     */
    disabled: {
      type: Boolean,
      default: false
    },
    /**
     * Auto-load ports on mount
     */
    autoLoad: {
      type: Boolean,
      default: true
    }
  },
  emits: ['update:modelValue'],
  setup(props) {
    const { client } = useApolloClient();
    const ports = ref([]);
    const filteredPorts = ref([]);
    const loading = ref(false);

    /**
     * Load ports from backend
     */
    const loadPorts = async () => {
      loading.value = true;
      try {
        const response = await client.query({
          query: PORT_LIST_ALL,
          variables: {},
          fetchPolicy: 'network-only',
        });

        ports.value = _.transform(response.data.devicePortList,
          function (result, value) {
            result.push({
              id: value.id,
              internalRef: value.internalRef,
              name: value.name,
              description: value.description,
              type: value.type,
              state: value.state,
              value: value.value,
              device: value.device
            });
          });
        filteredPorts.value = [...ports.value];
      } catch (error) {
        console.error('Error loading ports:', error);
      } finally {
        loading.value = false;
      }
    };

    /**
     * Filter ports based on search input
     * Searches across id, internalRef, name, description, and device name/code
     */
    const filterPorts = (val, update) => {
      if (val === '') {
        update(() => {
          filteredPorts.value = ports.value;
        });
        return;
      }

      update(() => {
        const needle = val.toLowerCase();
        filteredPorts.value = ports.value.filter(
          v => 
            String(v.id).toLowerCase().includes(needle) ||
            (v.internalRef && v.internalRef.toLowerCase().includes(needle)) ||
            (v.name && v.name.toLowerCase().includes(needle)) ||
            (v.description && v.description.toLowerCase().includes(needle)) ||
            (v.device?.name && v.device.name.toLowerCase().includes(needle)) ||
            (v.device?.code && v.device.code.toLowerCase().includes(needle))
        );
      });
    };

    onMounted(() => {
      if (props.autoLoad) {
        loadPorts();
      }
    });

    return {
      ports,
      filteredPorts,
      loading,
      filterPorts,
      loadPorts
    };
  }
});
</script>

