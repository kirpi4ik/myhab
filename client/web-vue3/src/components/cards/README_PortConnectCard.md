# PortConnectCard Component

A reusable Vue 3 component for managing port connections in device/cable/peripheral editors.

## Features

- **Device Selection**: Filterable dropdown to select a device
- **Port Selection**: Filterable dropdown to select a port from the selected device
- **Connect Action**: Add the selected port to the connected ports list
- **Create New Port**: Navigate to create a new port for the selected device
- **Connected Ports Table**: Display and manage currently connected ports
- **Remove Action**: Remove ports from the connected list
- **Row Navigation**: Click on table rows to navigate to port details

## Props

| Prop | Type | Required | Default | Description |
|------|------|----------|---------|-------------|
| `modelValue` | Array | No | `[]` | v-model binding for the list of connected ports |
| `deviceList` | Array | Yes | - | List of all available devices (must include `ports` array) |
| `title` | String | No | `'Port connect'` | Title displayed above the card |
| `tableTitle` | String | No | `'Connected to ports'` | Title displayed above the table |
| `customColumns` | Array | No | `null` | Custom column definitions for the table |
| `enableRowNavigation` | Boolean | No | `true` | Enable/disable navigation when clicking table rows |
| `newPortRoute` | String | No | `'/admin/ports/new'` | Route path for creating new ports |

## Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:modelValue` | Array | Emitted when the connected ports list changes |
| `port-connected` | Object | Emitted when a port is added to the list |
| `port-removed` | Object | Emitted when a port is removed from the list |
| `row-click` | Object | Emitted when a table row is clicked |

## Device List Format

The `deviceList` prop must be an array of objects with the following structure:

```javascript
[
  {
    id: 1,
    name: 'Device Name',
    ports: [
      {
        id: 101,
        name: 'Port 1',
        internalRef: 'REF-001'
      },
      {
        id: 102,
        name: 'Port 2',
        internalRef: 'REF-002'
      }
    ]
  }
]
```

## Usage Example

### Basic Usage (CableEdit.vue)

```vue
<template>
  <q-card flat bordered>
    <q-card-section>
      <!-- Other cable fields -->
    </q-card-section>
    
    <PortConnectCard
      v-model="cable.connectedTo"
      :device-list="deviceList"
      title="Port connect"
      table-title="Connected to ports"
    />
    
    <q-card-actions>
      <q-btn color="accent" type="submit">Save</q-btn>
    </q-card-actions>
  </q-card>
</template>

<script>
import { ref } from 'vue';
import PortConnectCard from '@/components/cards/PortConnectCard.vue';

export default {
  components: {
    PortConnectCard
  },
  setup() {
    const cable = ref({
      connectedTo: []
    });
    
    const deviceList = ref([]);
    
    // Fetch device list from API
    const fetchData = () => {
      // ... fetch logic
      deviceList.value = response.data.deviceList;
    };
    
    return {
      cable,
      deviceList
    };
  }
};
</script>
```

### Advanced Usage with Custom Columns

```vue
<template>
  <PortConnectCard
    v-model="equipment.connectedPorts"
    :device-list="devices"
    title="Equipment Port Connections"
    table-title="Active Port Connections"
    :custom-columns="customColumns"
    :enable-row-navigation="false"
    new-port-route="/custom/ports/create"
    @port-connected="onPortConnected"
    @port-removed="onPortRemoved"
    @row-click="handleRowClick"
  />
</template>

<script>
import { ref } from 'vue';
import PortConnectCard from '@/components/cards/PortConnectCard.vue';

export default {
  components: {
    PortConnectCard
  },
  setup() {
    const equipment = ref({
      connectedPorts: []
    });
    
    const devices = ref([]);
    
    const customColumns = [
      { name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true },
      { name: 'name', label: 'Port Name', field: 'name', align: 'left', sortable: true },
      { name: 'device', label: 'Device', field: row => row.device?.name || '', align: 'left' },
      { name: 'status', label: 'Status', field: 'status', align: 'center' },
      { name: 'actions', label: 'Actions', field: () => '', align: 'right', sortable: false }
    ];
    
    const onPortConnected = (port) => {
      console.log('Port connected:', port);
      // Custom logic after port connection
    };
    
    const onPortRemoved = (port) => {
      console.log('Port removed:', port);
      // Custom logic after port removal
    };
    
    const handleRowClick = (row) => {
      console.log('Row clicked:', row);
      // Custom navigation or action
    };
    
    return {
      equipment,
      devices,
      customColumns,
      onPortConnected,
      onPortRemoved,
      handleRowClick
    };
  }
};
</script>
```

### Integration with GraphQL Mutations

```vue
<script>
import { ref } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { prepareForMutation } from '@/_helpers';
import PortConnectCard from '@/components/cards/PortConnectCard.vue';
import { CABLE_VALUE_UPDATE } from '@/graphql/queries';

export default {
  components: {
    PortConnectCard
  },
  setup() {
    const { client } = useApolloClient();
    const cable = ref({ connectedTo: [] });
    
    const onSave = () => {
      const cleanCable = prepareForMutation(cable.value, ['__typename', 'device']);
      delete cleanCable.id;
      
      // Clean connectedTo array - keep only id field
      if (cleanCable.connectedTo && Array.isArray(cleanCable.connectedTo)) {
        cleanCable.connectedTo = cleanCable.connectedTo.map(port => ({
          id: port.id
        }));
      }
      
      client.mutate({
        mutation: CABLE_VALUE_UPDATE,
        variables: { id: route.params.idPrimary, cable: cleanCable },
        fetchPolicy: 'no-cache',
        update: () => {}
      }).then(response => {
        // Handle success
      });
    };
    
    return {
      cable,
      onSave
    };
  }
};
</script>
```

## Default Table Columns

If no `customColumns` prop is provided, the component uses these default columns:

```javascript
[
  { name: 'id', label: 'ID', field: 'id', align: 'left', sortable: true },
  { name: 'name', label: 'Name', field: 'name', align: 'left', sortable: true },
  { name: 'internalRef', label: 'Int Ref', field: 'internalRef', align: 'left', sortable: true },
  { name: 'actions', label: 'Actions', field: () => '', align: 'right', sortable: false }
]
```

## Styling

The component uses Quasar's built-in styling. You can customize it by:

1. **Scoped Styles**: Add custom styles in the component's `<style scoped>` section
2. **Quasar Theme**: Modify your Quasar theme configuration
3. **CSS Classes**: Add custom classes to the component wrapper

## Notes

- The component requires the device list to include a `ports` array for each device
- Port filtering is case-insensitive and searches by name
- The "New" button navigates to the create port page with the selected device ID as a query parameter
- Row navigation can be disabled if you want to handle clicks differently
- The component is fully reactive and updates when props change

## Related Components

- Use this component for: Cables, Equipment, Network Devices
- For multi-select port connections (like Peripherals), consider using a different pattern with `q-select` and `multiple` prop

## Migration from Inline Code

If you have existing inline port connection code, follow these steps:

1. Import the `PortConnectCard` component
2. Replace the inline `<q-card-section>` with `<PortConnectCard>`
3. Pass `v-model` bound to your object's `connectedTo` array
4. Pass `:device-list` with your device list
5. Remove unused variables: `newPortDevice`, `newPort`, `portList`, `options`, `portColumns`
6. Remove unused functions: `selectDevice`, `connectPort`, `removePortFromConnected`, `filterFn`, `portFilterFn`, `viewPort`
7. Test the functionality

## Support

For issues or questions, refer to the component source code at:
`src/components/cards/PortConnectCard.vue`

