# LocationSelector Component

A reusable component for selecting zones/locations for entities.

## Features

- ✅ Multi-select zones with chips
- ✅ Search/filter zones by name
- ✅ Auto-loads zones from backend
- ✅ Consistent styling across the app
- ✅ Loading state indicator
- ✅ Customizable labels and hints

## Usage

### Basic Usage

```vue
<template>
  <LocationSelector
    v-model="entity.zones"
  />
</template>

<script>
import LocationSelector from '@/components/selectors/LocationSelector.vue';

export default {
  components: { LocationSelector },
  setup() {
    const entity = ref({ zones: [] });
    return { entity };
  }
};
</script>
```

### With Custom Labels

```vue
<LocationSelector
  v-model="peripheral.zones"
  label="Peripheral Locations"
  hint="Select zones where this peripheral is installed"
/>
```

### Disabled State

```vue
<LocationSelector
  v-model="device.zones"
  :disabled="!device.isActive"
/>
```

### Manual Load Control

```vue
<template>
  <LocationSelector
    v-model="entity.zones"
    :auto-load="false"
    ref="locationSelector"
  />
  <q-btn @click="loadZones">Load Zones</q-btn>
</template>

<script>
import { ref } from 'vue';
import LocationSelector from '@/components/selectors/LocationSelector.vue';

export default {
  components: { LocationSelector },
  setup() {
    const locationSelector = ref(null);
    const entity = ref({ zones: [] });
    
    const loadZones = () => {
      locationSelector.value.loadZones();
    };
    
    return { entity, locationSelector, loadZones };
  }
};
</script>
```

## Props

| Prop | Type | Default | Description |
|------|------|---------|-------------|
| `modelValue` | `Array` | `[]` | Selected zones (v-model) |
| `label` | `String` | `'Zones'` | Label for the selector |
| `hint` | `String` | `'Select zones where this item is located'` | Hint text below the selector |
| `disabled` | `Boolean` | `false` | Disable the selector |
| `autoLoad` | `Boolean` | `true` | Auto-load zones on mount |

## Events

| Event | Payload | Description |
|-------|---------|-------------|
| `update:modelValue` | `Array` | Emitted when selection changes |

## Data Structure

Each zone object has the following structure:

```javascript
{
  id: Number,        // Zone ID
  name: String,      // Zone name
  description: String // Zone description (optional)
}
```

## Examples in the Codebase

- **PeripheralEdit.vue**: Select zones for peripheral location
- **CableEdit.vue**: Select zones where cable is routed (if needed)
- **DeviceEdit.vue**: Select zones for device location (if needed)

## Implementation Details

### GraphQL Query

Uses `ZONES_GET_ALL` query to fetch all available zones:

```graphql
query {
  zoneList {
    id
    name
    description
  }
}
```

### Filtering

Implements client-side filtering for instant search results:
- Case-insensitive search
- Matches zone names containing the search term
- Updates filtered list in real-time

### Loading State

Shows a loading indicator while fetching zones from the backend.

## Styling

- Uses Quasar's filled, dense style for consistency
- Displays selected zones as chips
- Includes map marker icon for visual clarity
- Supports dark mode (inherited from Quasar theme)

## Future Enhancements

- [ ] Add zone hierarchy support (parent/child zones)
- [ ] Add zone icons/colors
- [ ] Support single-select mode
- [ ] Add zone creation from selector
- [ ] Cache zones to reduce API calls
- [ ] Add zone grouping by type/category

## Related Components

- `PortConnectCard` - For selecting port connections
- `EntityInfoPanel` - For displaying entity information
- `EntityFormActions` - For form action buttons

