<template>
  <q-card-section>
    <q-item-label>{{ title }}</q-item-label>
    <div class="row q-col-gutter-xs">
      <q-select 
        v-model="selectedDevice"
        :options="filteredDevices"
        option-label="name"
        label="Device" 
        map-options 
        filled 
        dense 
        use-input 
        @filter="filterDevices" 
        color="green"
        class="col-lg-2 col-md-2"
        @update:model-value="onDeviceSelect"
      >
        <q-icon 
          name="cancel" 
          @click.stop.prevent="clearDevice" 
          class="cursor-pointer text-blue"
        />
      </q-select>
      
      <q-select 
        v-if="selectedDevice != null"
        v-model="selectedPort"
        :options="filteredPorts"
        option-label="name"
        label="Port" 
        map-options 
        filled 
        dense 
        use-input 
        @filter="filterPorts" 
        color="green"
        class="col-lg-2 col-md-2"
      >
        <q-icon 
          name="cancel" 
          @click.stop.prevent="clearPort" 
          class="cursor-pointer text-blue"
        />
      </q-select>
      
      <q-btn 
        icon="mdi-link-variant-plus" 
        @click="handleConnect" 
        color="green" 
        label="Connect"
        :disable="selectedPort == null"
      />
      
      <q-btn 
        icon="mdi-plus" 
        @click="handleCreateNewPort" 
        color="orange"
        label="New" 
        :disable="selectedPort != null || selectedDevice == null"
      />
    </div>
    
    <div class="row">
      <q-table
        class="col"
        :title="tableTitle"
        :rows="connectedPorts"
        :columns="columns"
        @row-click="handleRowClick"
        row-key="id"
        flat
        bordered
      >
        <template v-slot:body-cell-actions="props">
          <q-td :props="props">
            <q-btn 
              icon="delete" 
              @click.stop="handleRemove(props.row)"
              flat
              dense
              color="negative"
            />
          </q-td>
        </template>
      </q-table>
    </div>
  </q-card-section>
</template>

<script>
import { defineComponent, ref, watch, computed } from 'vue';
import { useRouter } from 'vue-router';

export default defineComponent({
  name: 'PortConnectCard',
  
  props: {
    // The list of connected ports (v-model)
    modelValue: {
      type: Array,
      default: () => []
    },
    
    // List of all available devices
    deviceList: {
      type: Array,
      required: true
    },
    
    // Card title
    title: {
      type: String,
      default: 'Port connect'
    },
    
    // Table title
    tableTitle: {
      type: String,
      default: 'Connected to ports'
    },
    
    // Custom columns for the table (optional)
    customColumns: {
      type: Array,
      default: null
    },
    
    // Enable/disable navigation on row click
    enableRowNavigation: {
      type: Boolean,
      default: true
    },
    
    // Custom route path for creating new port (optional)
    newPortRoute: {
      type: String,
      default: '/admin/ports/new'
    }
  },
  
  emits: ['update:modelValue', 'port-connected', 'port-removed', 'row-click'],
  
  setup(props, { emit }) {
    const router = useRouter();
    
    const selectedDevice = ref(null);
    const selectedPort = ref(null);
    const filteredDevices = ref([]);
    const filteredPorts = ref([]);
    
    // Initialize filtered devices
    filteredDevices.value = props.deviceList;
    
    // Watch for changes in deviceList
    watch(() => props.deviceList, (newList) => {
      filteredDevices.value = newList;
    });
    
    // Default columns
    const defaultColumns = [
      { name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true },
      { name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true },
      { name: 'internalRef', label: 'Int Ref', field: 'internalRef', align: 'left', sortable: true },
      { name: 'actions', label: 'Actions', field: () => '', align: 'right', sortable: false }
    ];
    
    // Use custom columns if provided, otherwise use defaults
    const columns = computed(() => props.customColumns || defaultColumns);
    
    // Computed property for connected ports
    const connectedPorts = computed(() => props.modelValue || []);
    
    // Device selection handler
    const onDeviceSelect = () => {
      selectedPort.value = null;
      if (selectedDevice.value && selectedDevice.value.ports) {
        filteredPorts.value = selectedDevice.value.ports;
      }
    };
    
    // Clear device selection
    const clearDevice = () => {
      selectedDevice.value = null;
      selectedPort.value = null;
      filteredPorts.value = [];
    };
    
    // Clear port selection
    const clearPort = () => {
      selectedPort.value = null;
    };
    
    // Filter devices
    const filterDevices = (val, update) => {
      if (val === '') {
        update(() => {
          filteredDevices.value = props.deviceList;
        });
        return;
      }
      
      update(() => {
        const needle = val.toLowerCase();
        filteredDevices.value = props.deviceList.filter(
          v => v.name.toLowerCase().includes(needle)
        );
      });
    };
    
    // Filter ports
    const filterPorts = (val, update) => {
      if (!selectedDevice.value || !selectedDevice.value.ports) {
        update(() => {
          filteredPorts.value = [];
        });
        return;
      }
      
      if (val === '') {
        update(() => {
          filteredPorts.value = selectedDevice.value.ports;
        });
        return;
      }
      
      update(() => {
        const needle = val.toLowerCase();
        filteredPorts.value = selectedDevice.value.ports.filter(
          v => v.name.toLowerCase().includes(needle)
        );
      });
    };
    
    // Connect port handler
    const handleConnect = () => {
      if (selectedPort.value) {
        const updatedPorts = [...connectedPorts.value, selectedPort.value];
        emit('update:modelValue', updatedPorts);
        emit('port-connected', selectedPort.value);
        
        // Reset selections
        selectedPort.value = null;
        selectedDevice.value = null;
        filteredPorts.value = [];
      }
    };
    
    // Remove port handler
    const handleRemove = (port) => {
      const updatedPorts = connectedPorts.value.filter(p => p.id !== port.id);
      emit('update:modelValue', updatedPorts);
      emit('port-removed', port);
    };
    
    // Row click handler
    const handleRowClick = (evt, row) => {
      if (evt.target.nodeName === 'TD' || evt.target.nodeName === 'DIV') {
        emit('row-click', row);
        
        if (props.enableRowNavigation) {
          router.push({ path: `/admin/ports/${row.id}/view` });
        }
      }
    };
    
    // Create new port handler
    const handleCreateNewPort = () => {
      if (selectedDevice.value) {
        router.push(`${props.newPortRoute}?deviceId=${selectedDevice.value.id}`);
      }
    };
    
    return {
      selectedDevice,
      selectedPort,
      filteredDevices,
      filteredPorts,
      columns,
      connectedPorts,
      onDeviceSelect,
      clearDevice,
      clearPort,
      filterDevices,
      filterPorts,
      handleConnect,
      handleRemove,
      handleRowClick,
      handleCreateNewPort
    };
  }
});
</script>

<style scoped>
/* Add any custom styles here if needed */
</style>

