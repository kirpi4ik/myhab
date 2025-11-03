# MyHAB Vue3 Development Guide

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Reusable Composables](#reusable-composables)
3. [Reusable Components](#reusable-components)
4. [Best Practices](#best-practices)
5. [Common Patterns](#common-patterns)
6. [GraphQL Guidelines](#graphql-guidelines)
7. [Testing Guidelines](#testing-guidelines)

---

## Architecture Overview

### Technology Stack
- **Framework:** Vue 3 (Composition API)
- **UI Library:** Quasar Framework
- **State Management:** Vuex (minimal usage, prefer composables)
- **GraphQL Client:** Apollo Client
- **Router:** Vue Router 4
- **Build Tool:** Quasar CLI (Webpack/Vite)

### Project Structure
```
src/
├── _helpers/          # Utility functions
├── _services/         # Service layer (API calls)
├── assets/            # Static assets (images, styles)
├── boot/              # Quasar boot files
├── components/        # Reusable Vue components
├── composables/       # Reusable composition functions
├── css/               # Global styles
├── graphql/           # GraphQL queries and mutations
├── i18n/              # Internationalization
├── layouts/           # Layout components
├── pages/             # Page components
│   └── infra/         # Infrastructure pages (/admin/*)
│       ├── cable/     # Cable management
│       ├── device/    # Device management
│       ├── peripheral/# Peripheral management
│       ├── port/      # Port management
│       ├── zone/      # Zone management
│       ├── user/      # User management
│       ├── job/       # Job management
│       └── scenario/  # Scenario management
├── router/            # Vue Router configuration
└── store/             # Vuex store modules
```

---

## Reusable Composables

### 1. useNotifications

**Purpose:** Standardized notification system across the application.

**Location:** `src/composables/useNotifications.js`

**Usage:**
```javascript
import { useNotifications } from '@/composables';

export default {
  setup() {
    const { notifySuccess, notifyError, notifyWarning, notifyInfo } = useNotifications();

    const saveData = async () => {
      try {
        // ... save logic
        notifySuccess('Data saved successfully');
      } catch (error) {
        notifyError('Failed to save data', error);
      }
    };

    return { saveData };
  }
};
```

**API:**
- `notifySuccess(message, options)` - Show success notification
- `notifyError(message, error, options)` - Show error notification with optional error object
- `notifyWarning(message, options)` - Show warning notification
- `notifyInfo(message, options)` - Show info notification
- `notifyValidationError(message, options)` - Show validation error
- `notifyLoading(message, options)` - Show loading notification (returns dismiss function)

---

### 2. useEntityCRUD

**Purpose:** Standardized CRUD operations for entities.

**Location:** `src/composables/useEntityCRUD.js`

**Usage:**
```javascript
import { useEntityCRUD } from '@/composables';
import { CABLE_EDIT_GET_DETAILS, CABLE_VALUE_UPDATE, CABLE_DELETE } from '@/graphql/queries';

export default {
  setup() {
    const {
      entity,
      loading,
      saving,
      fetchEntity,
      updateEntity,
      deleteEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Cable',
      entityPath: '/admin/cables',
      getQuery: CABLE_EDIT_GET_DETAILS,
      updateMutation: CABLE_VALUE_UPDATE,
      deleteMutation: CABLE_DELETE,
      excludeFields: ['__typename', 'uid', 'tsCreated', 'tsUpdated'],
      transformBeforeSave: (data) => {
        // Custom transformation before saving
        if (data.rack) data.rack = { id: data.rack.id };
        return data;
      }
    });

    onMounted(() => {
      fetchEntity();
    });

    const onSave = async () => {
      if (!validateRequired(entity.value, ['code', 'description'])) {
        return;
      }
      await updateEntity();
    };

    return {
      entity,
      loading,
      saving,
      onSave
    };
  }
};
```

**API:**
- `entity` - Reactive entity object
- `loading` - Loading state
- `saving` - Saving state
- `fetchEntity(id, additionalVariables)` - Fetch entity by ID
- `createEntity(data, additionalVariables)` - Create new entity
- `updateEntity(id, data, additionalVariables)` - Update existing entity
- `deleteEntity(id, confirmCallback)` - Delete entity
- `validateRequired(data, requiredFields)` - Validate required fields

---

### 3. useEntityList

**Purpose:** Standardized list operations for entities.

**Location:** `src/composables/useEntityList.js`

**Usage:**
```javascript
import { useEntityList } from '@/composables';
import { CABLE_LIST_ALL, CABLE_DELETE } from '@/graphql/queries';

export default {
  setup() {
    const {
      items,
      filteredItems,
      loading,
      filter,
      pagination,
      fetchList,
      viewItem,
      editItem,
      createItem,
      deleteItem,
      exportToCSV
    } = useEntityList({
      entityName: 'Cable',
      entityPath: '/admin/cables',
      listQuery: CABLE_LIST_ALL,
      deleteMutation: CABLE_DELETE,
      columns: [
        { name: 'code', label: 'Code', field: 'code' },
        { name: 'description', label: 'Description', field: 'description' }
      ]
    });

    onMounted(() => {
      fetchList();
    });

    return {
      items: filteredItems,
      loading,
      filter,
      pagination,
      viewItem,
      editItem,
      createItem,
      deleteItem,
      exportToCSV
    };
  }
};
```

**API:**
- `items` - Raw list of entities
- `filteredItems` - Filtered list based on search
- `loading` - Loading state
- `filter` - Search filter string
- `pagination` - Pagination state
- `fetchList(additionalVariables)` - Fetch list of entities
- `viewItem(item)` - Navigate to view page
- `editItem(item)` - Navigate to edit page
- `createItem()` - Navigate to create page
- `deleteItem(item)` - Delete entity with confirmation
- `refresh()` - Refresh the list
- `exportToCSV(columnKeys)` - Export list to CSV

---

## Reusable Components

### 1. EntityInfoPanel

**Purpose:** Display entity metadata (ID, UID, timestamps, custom info).

**Location:** `src/components/EntityInfoPanel.vue`

**Usage:**
```vue
<template>
  <EntityInfoPanel
    :entity="cable"
    title="Cable Information"
    icon="mdi-ethernet-cable"
    :show-timestamps="true"
    :extra-info="[
      { icon: 'mdi-ethernet', label: 'Connected Ports', value: cable.connectedTo?.length || 0 },
      { icon: 'mdi-cable-data', label: 'Wires', value: cable.nrWires }
    ]"
  />
</template>

<script>
import EntityInfoPanel from '@/components/EntityInfoPanel.vue';

export default {
  components: { EntityInfoPanel },
  // ...
};
</script>
```

**Props:**
- `entity` (Object, required) - Entity object with id, uid, timestamps
- `title` (String, default: 'Information') - Panel title
- `icon` (String, default: 'mdi-information') - Title icon
- `showTimestamps` (Boolean, default: true) - Show created/updated dates
- `showVersion` (Boolean, default: false) - Show version field
- `extraInfo` (Array, default: []) - Additional custom info items

---

### 2. EntityFormActions

**Purpose:** Standardized form action buttons (Save, Cancel, View, Delete).

**Location:** `src/components/EntityFormActions.vue`

**Usage:**
```vue
<template>
  <form @submit.prevent="onSave">
    <!-- Form fields -->

    <EntityFormActions
      :saving="saving"
      :show-view="true"
      :show-delete="false"
      :view-route="`/admin/cables/${cable.id}/view`"
      save-label="Save Cable"
      @cancel="$router.go(-1)"
    />
  </form>
</template>

<script>
import EntityFormActions from '@/components/EntityFormActions.vue';

export default {
  components: { EntityFormActions },
  // ...
};
</script>
```

**Props:**
- `showSave` (Boolean, default: true) - Show save button
- `showCancel` (Boolean, default: true) - Show cancel button
- `showView` (Boolean, default: false) - Show view button
- `showDelete` (Boolean, default: false) - Show delete button
- `saving` (Boolean, default: false) - Saving state
- `disabled` (Boolean, default: false) - Disable all buttons
- `saveLabel` (String, default: 'Save') - Save button label
- `cancelLabel` (String, default: 'Cancel') - Cancel button label
- `viewLabel` (String, default: 'View') - View button label
- `deleteLabel` (String, default: 'Delete') - Delete button label
- `saveIcon`, `cancelIcon`, `viewIcon`, `deleteIcon` - Button icons
- `viewRoute` (String|Object) - Route for view button
- `onCancel` (Function) - Custom cancel handler
- `onDelete` (Function) - Custom delete handler

**Events:**
- `cancel` - Emitted when cancel is clicked
- `delete` - Emitted when delete is clicked

**Slots:**
- `actions` - Custom action buttons

---

### 3. PortConnectCard

**Purpose:** Reusable component for connecting ports to an entity.

**Location:** `src/components/cards/PortConnectCard.vue`

**Documentation:** See `src/components/cards/README_PortConnectCard.md`

**Usage:**
```vue
<template>
  <PortConnectCard
    v-model="cable.connectedTo"
    :device-list="deviceList"
    title="Port Connections"
    table-title="Connected Ports"
  />
</template>

<script>
import PortConnectCard from '@/components/cards/PortConnectCard.vue';

export default {
  components: { PortConnectCard },
  // ...
};
</script>
```

---

## Best Practices

### 1. Component Structure

**Standard Edit Page Structure:**
```vue
<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" class="q-gutter-md" v-if="!loading && entity">
      <q-card flat bordered>
        <!-- Header Section -->
        <q-card-section>
          <div class="text-h5 text-primary">Edit {{ entityName }}: {{ entity.name }}</div>
        </q-card-section>

        <q-separator/>

        <!-- Information Panel -->
        <EntityInfoPanel :entity="entity" :extra-info="extraInfo"/>

        <q-separator/>

        <!-- Basic Information Section -->
        <q-card-section>
          <div class="text-subtitle2 text-weight-medium q-mb-sm">Basic Information</div>
        </q-card-section>

        <q-card-section class="q-gutter-md">
          <!-- Form fields with icons and hints -->
          <q-input
            v-model="entity.code"
            label="Code"
            hint="Unique identifier"
            clearable
            filled
            dense
            :rules="[val => !!val || 'Code is required']"
          >
            <template v-slot:prepend>
              <q-icon name="mdi-barcode"/>
            </template>
          </q-input>
        </q-card-section>

        <q-separator/>

        <!-- Actions -->
        <EntityFormActions
          :saving="saving"
          :show-view="true"
          :view-route="`${entityPath}/${entity.id}/view`"
        />
      </q-card>

      <!-- Loading Spinner -->
      <q-inner-loading :showing="loading">
        <q-spinner-gears size="50px" color="primary"/>
      </q-inner-loading>
    </form>
  </q-page>
</template>

<script>
import { defineComponent, onMounted } from 'vue';
import { useEntityCRUD, useNotifications } from '@/composables';
import EntityInfoPanel from '@/components/EntityInfoPanel.vue';
import EntityFormActions from '@/components/EntityFormActions.vue';

export default defineComponent({
  name: 'EntityEdit',
  components: {
    EntityInfoPanel,
    EntityFormActions
  },
  setup() {
    const { entity, loading, saving, fetchEntity, updateEntity, validateRequired } = useEntityCRUD({
      // ... configuration
    });

    onMounted(() => {
      fetchEntity();
    });

    const onSave = async () => {
      if (!validateRequired(entity.value, ['code', 'name'])) {
        return;
      }
      await updateEntity();
    };

    return {
      entity,
      loading,
      saving,
      onSave
    };
  }
});
</script>
```

---

### 2. Form Input Guidelines

**Always include:**
- Icon in prepend slot
- Hint text
- Validation rules
- `filled` and `dense` props
- `clearable` for text inputs

**Example:**
```vue
<q-input
  v-model="entity.code"
  label="Code"
  hint="Unique identifier for the cable"
  clearable
  clear-icon="close"
  color="orange"
  filled
  dense
  :rules="[val => !!val || 'Code is required']"
>
  <template v-slot:prepend>
    <q-icon name="mdi-barcode"/>
  </template>
</q-input>
```

---

### 3. Error Handling

**Always use the notification composable:**
```javascript
import { useNotifications } from '@/composables';

const { notifyError } = useNotifications();

try {
  // ... operation
} catch (error) {
  notifyError('Failed to perform operation', error);
  // Error is automatically logged to console
}
```

---

### 4. Data Cleaning for Mutations

**Always clean data before mutations:**
```javascript
import { prepareForMutation } from '@/_helpers/apollo-utils';

const cleanData = prepareForMutation(entity.value, [
  '__typename',
  'entityType',
  'id',
  'uid',
  'tsCreated',
  'tsUpdated'
]);

// Clean nested objects
if (cleanData.rack) cleanData.rack = { id: cleanData.rack.id };
if (cleanData.category) cleanData.category = { id: cleanData.category.id };

// Clean arrays of objects
if (cleanData.connectedTo) {
  cleanData.connectedTo = cleanData.connectedTo.map(port => ({ id: port.id }));
}
```

---

### 5. Loading States

**Always show loading indicators:**
```vue
<template>
  <q-page padding>
    <div v-if="!loading && entity">
      <!-- Content -->
    </div>

    <q-inner-loading :showing="loading">
      <q-spinner-gears size="50px" color="primary"/>
    </q-inner-loading>
  </q-page>
</template>
```

---

## Common Patterns

### 1. List Page Pattern

```vue
<template>
  <q-page padding>
    <q-card flat bordered>
      <!-- Header with search and add button -->
      <q-card-section>
        <div class="row items-center">
          <div class="text-h5 text-primary">{{ entityName }} List</div>
          <q-space/>
          <q-input
            v-model="filter"
            dense
            debounce="300"
            placeholder="Search..."
            clearable
          >
            <template v-slot:prepend>
              <q-icon name="mdi-magnify"/>
            </template>
          </q-input>
          <q-btn
            color="primary"
            icon="mdi-plus"
            @click="createItem"
            class="q-ml-sm"
          >
            Add {{ entityName }}
          </q-btn>
        </div>
      </q-card-section>

      <!-- Table -->
      <q-table
        :rows="items"
        :columns="columns"
        :loading="loading"
        :filter="filter"
        :pagination="pagination"
        row-key="id"
        flat
        @row-click="viewItem"
      >
        <!-- Custom columns and actions -->
      </q-table>
    </q-card>
  </q-page>
</template>
```

---

### 2. View Page Pattern

```vue
<template>
  <q-page padding>
    <q-card flat bordered v-if="!loading && entity">
      <!-- Header with actions -->
      <q-card-section>
        <div class="row items-center">
          <div class="text-h5 text-primary">{{ entity.name }}</div>
          <q-space/>
          <q-btn
            outline
            round
            color="amber-8"
            icon="mdi-pencil"
            :to="`${entityPath}/${entity.id}/edit`"
          >
            <q-tooltip>Edit</q-tooltip>
          </q-btn>
        </div>
      </q-card-section>

      <q-separator/>

      <!-- Information Panel -->
      <EntityInfoPanel :entity="entity"/>

      <q-separator/>

      <!-- Details sections -->
      <q-card-section>
        <!-- Display entity details -->
      </q-card-section>
    </q-card>

    <q-inner-loading :showing="loading">
      <q-spinner-gears size="50px" color="primary"/>
    </q-inner-loading>
  </q-page>
</template>
```

---

## GraphQL Guidelines

### 1. Query Naming Convention

- List queries: `ENTITY_LIST_ALL`
- Get by ID: `ENTITY_GET_BY_ID`
- Edit details: `ENTITY_EDIT_GET_BY_ID`
- Create: `ENTITY_CREATE`
- Update: `ENTITY_UPDATE` or `ENTITY_VALUE_UPDATE`
- Delete: `ENTITY_DELETE` or `ENTITY_DELETE_BY_ID`

### 2. Query Organization

**File structure:** `src/graphql/queries/[entity-plural].js`

Example: `cables.js`, `devices.js`, `peripherals.js`

### 3. Field Selection

**Only query fields you need:**
```graphql
# Good - minimal fields for list
query {
  cableList {
    id
    code
    description
  }
}

# Bad - over-fetching
query {
  cableList {
    id
    uid
    code
    description
    tsCreated
    tsUpdated
    rack { id name description }
    # ... many more fields
  }
}
```

### 4. Avoid N+1 Queries

**Use nested queries instead of multiple requests:**
```graphql
# Good - single query
query {
  cable(id: $id) {
    id
    code
    connectedTo {
      id
      name
      device {
        code
      }
    }
  }
}

# Bad - would require multiple queries
```

---

## Testing Guidelines

### 1. Unit Tests

**Test composables:**
```javascript
import { useNotifications } from '@/composables';

describe('useNotifications', () => {
  it('should show success notification', () => {
    const { notifySuccess } = useNotifications();
    notifySuccess('Test message');
    // Assert notification was shown
  });
});
```

### 2. Component Tests

**Test components with Vue Test Utils:**
```javascript
import { mount } from '@vue/test-utils';
import EntityInfoPanel from '@/components/EntityInfoPanel.vue';

describe('EntityInfoPanel', () => {
  it('should display entity ID', () => {
    const wrapper = mount(EntityInfoPanel, {
      props: {
        entity: { id: 123, uid: 'test-uid' }
      }
    });
    expect(wrapper.text()).toContain('123');
  });
});
```

---

## Migration Guide

### Migrating Existing Components

**Step 1:** Replace custom notifications with `useNotifications`

**Before:**
```javascript
$q.notify({
  color: 'positive',
  message: 'Success',
  icon: 'check',
  position: 'top'
});
```

**After:**
```javascript
const { notifySuccess } = useNotifications();
notifySuccess('Success');
```

**Step 2:** Replace custom CRUD logic with `useEntityCRUD`

**Before:**
```javascript
const loading = ref(false);
const entity = ref(null);

const fetchData = async () => {
  loading.value = true;
  try {
    const response = await client.query({...});
    entity.value = response.data.cable;
  } catch (error) {
    // error handling
  } finally {
    loading.value = false;
  }
};
```

**After:**
```javascript
const { entity, loading, fetchEntity } = useEntityCRUD({
  entityName: 'Cable',
  getQuery: CABLE_GET_BY_ID
});

onMounted(() => {
  fetchEntity();
});
```

**Step 3:** Add `EntityInfoPanel` and `EntityFormActions`

Replace custom info panels and action buttons with the reusable components.

---

## Conclusion

This guide provides the foundation for consistent, maintainable Vue 3 development in the MyHAB project. Always refer to this guide when creating new components or refactoring existing ones.

For questions or suggestions, please contact the development team.

---

**Last Updated:** November 3, 2025

