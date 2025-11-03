# "New" Components Migration Guide

**Date:** November 3, 2025  
**Status:** 3/10 Complete - 7 Remaining

---

## Completed Migrations ✅

1. ✅ DeviceNew.vue
2. ✅ CategoryNew.vue (Device)
3. ✅ CategoryNew.vue (Peripheral)

## Remaining Migrations ⏳

4. ⏳ ZoneNew.vue
5. ⏳ CableNew.vue
6. ⏳ PeripheralNew.vue
7. ⏳ PortNew.vue
8. ⏳ UserNew.vue
9. ⏳ ScenarioNew.vue
10. ⏳ JobNew.vue

---

## Migration Pattern

All "New" components follow the exact same migration pattern. Here's the step-by-step guide:

### Step 1: Update Template

**Before:**
```vue
<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave">
      <q-card>
        <!-- form fields -->
        <q-card-actions>
          <q-btn color="primary" type="submit" :loading="saving">Save</q-btn>
          <q-btn color="grey" @click="$router.go(-1)">Cancel</q-btn>
        </q-card-actions>
      </q-card>
    </form>
  </q-page>
</template>
```

**After:**
```vue
<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" v-if="entity">  <!-- ⚠️ Add v-if -->
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-plus-circle" color="primary" size="sm" class="q-mr-sm"/>
            Create New Entity
          </div>
        </q-card-section>

        <q-separator/>

        <!-- form fields with better styling -->
        
        <q-separator/>

        <!-- ⚠️ Replace manual buttons with component -->
        <EntityFormActions
          :saving="saving"
          :show-view="false"
          save-label="Create Entity"
        />
      </q-card>
    </form>
  </q-page>
</template>
```

### Step 2: Update Script Imports

**Before:**
```javascript
import {defineComponent, ref} from 'vue';
import {useApolloClient} from "@vue/apollo-composable";
import {useRouter} from "vue-router";
import {useQuasar} from 'quasar';
import {ENTITY_CREATE} from '@/graphql/queries';
```

**After:**
```javascript
import {defineComponent} from 'vue';
import {useEntityCRUD} from '@/composables';
import EntityFormActions from '@/components/EntityFormActions.vue';
import {ENTITY_CREATE} from '@/graphql/queries';
```

### Step 3: Add Component Registration

```javascript
export default defineComponent({
  name: 'EntityNew',
  components: {
    EntityFormActions  // ⚠️ Add this
  },
  setup() {
```

### Step 4: Replace Manual State with Composable

**Before:**
```javascript
const $q = useQuasar();
const {client} = useApolloClient();
const entity = ref({
  field1: '',
  field2: '',
  // ...
});
const router = useRouter();
const saving = ref(false);
```

**After:**
```javascript
const {
  entity,
  saving,
  createEntity,
  validateRequired
} = useEntityCRUD({
  entityName: 'Entity',  // Display name
  entityPath: '/admin/entities',  // Base path for routing
  createMutation: ENTITY_CREATE,  // GraphQL mutation
  createMutationKey: 'entityCreate',  // Response key (e.g., deviceCreate, zoneCreate)
  createVariableName: 'entity',  // Variable name in mutation (e.g., device, zone)
  excludeFields: ['__typename'],  // ⚠️ IMPORTANT: Only exclude __typename
  initialData: {  // ⚠️ IMPORTANT: Provide initial data
    field1: '',
    field2: '',
    // ... all fields with default values
  },
  transformBeforeSave: (data) => {  // Optional: clean nested objects
    const transformed = {...data};
    // Clean nested objects - only send IDs if they exist
    if (transformed.nestedObject) {
      if (transformed.nestedObject.id) {
        transformed.nestedObject = { id: transformed.nestedObject.id };
      } else {
        delete transformed.nestedObject;
      }
    }
    return transformed;
  }
});
```

### Step 5: Replace Manual onSave

**Before:**
```javascript
const onSave = () => {
  if (!entity.value.field1 || !entity.value.field2) {
    $q.notify({
      color: 'negative',
      message: 'Please fill in all required fields',
      icon: 'mdi-alert-circle',
      position: 'top'
    });
    return;
  }

  saving.value = true;

  client.mutate({
    mutation: ENTITY_CREATE,
    variables: {entity: entity.value},
    fetchPolicy: 'no-cache',
  }).then(response => {
    saving.value = false;
    $q.notify({
      color: 'positive',
      message: 'Entity created successfully',
      icon: 'mdi-check-circle',
      position: 'top'
    });
    router.push({path: `/admin/entities/${response.data.entityCreate.id}/edit`});
  }).catch(error => {
    saving.value = false;
    $q.notify({
      color: 'negative',
      message: error.message || 'Failed to create entity',
      icon: 'mdi-alert-circle',
      position: 'top'
    });
    console.error('Error creating entity:', error);
  });
};
```

**After:**
```javascript
const onSave = async () => {
  // Prevent duplicate submissions
  if (saving.value) return;
  
  // Validate required fields
  if (!validateRequired(entity.value, ['field1', 'field2'])) {
    return;
  }

  await createEntity();
};
```

### Step 6: Update Return Statement

**Before:**
```javascript
return {
  entity,
  onSave,
  saving,
  // ... other refs
};
```

**After:**
```javascript
return {
  entity,
  saving,
  onSave,
  // ... only what template needs
};
```

---

## Component-Specific Notes

### ZoneNew.vue
- Has parent zone selection
- May have sub-zones selection
- Use `transformBeforeSave` to clean parent: `{ id: parent.id }`

### CableNew.vue
- Has rack selection
- Has category selection
- Use `transformBeforeSave` to clean nested objects

### PeripheralNew.vue
- Has zone selection (use `LocationSelector` component if available)
- Has category selection
- Has device selection
- Use `transformBeforeSave` to clean all nested objects

### PortNew.vue
- Has device selection
- Has port type selection
- Has port state selection
- Use `transformBeforeSave` to clean device object

### UserNew.vue
- Has password fields
- Has roles selection (separate mutation)
- May need custom logic for roles after user creation
- Keep password validation logic

### ScenarioNew.vue
- Has CodeEditor component
- Has port selection
- Keep CodeEditor and custom completions
- Use `transformBeforeSave` to clean ports array

### JobNew.vue
- Has scenario selection
- Has cron triggers (array)
- Has tags (with create-on-the-fly)
- Has job state selection
- Use `transformBeforeSave` to clean scenario, triggers, and tags

---

## Common Pitfalls

### ❌ Pitfall 1: Missing v-if
```vue
<!-- WRONG -->
<form @submit.prevent.stop="onSave">

<!-- CORRECT -->
<form @submit.prevent.stop="onSave" v-if="entity">
```

### ❌ Pitfall 2: Wrong excludeFields
```javascript
// WRONG - excludes too much
excludeFields: ['__typename', 'id', 'uid', 'tsCreated', 'tsUpdated']

// CORRECT - only exclude __typename for create
excludeFields: ['__typename']
```

### ❌ Pitfall 3: Missing initialData
```javascript
// WRONG - entity will be null
const { entity } = useEntityCRUD({
  createMutation: ENTITY_CREATE,
});

// CORRECT - entity initialized immediately
const { entity } = useEntityCRUD({
  createMutation: ENTITY_CREATE,
  initialData: { field1: '', field2: '' }
});
```

### ❌ Pitfall 4: Wrong mutation keys
```javascript
// WRONG - mismatch with actual GraphQL response
createMutationKey: 'createDevice',  // But GraphQL returns 'deviceCreate'

// CORRECT - matches GraphQL response
createMutationKey: 'deviceCreate',
```

---

## Testing Checklist

For each migrated component, verify:

- [ ] Page loads without errors
- [ ] Form fields are visible and editable
- [ ] Validation works (required fields)
- [ ] Create button shows loading state
- [ ] Success notification appears
- [ ] Redirects to correct page after creation
- [ ] Error handling works (try invalid data)
- [ ] Cancel button works
- [ ] Nested objects are cleaned properly (check network payload)

---

## Quick Reference: Mutation Keys

| Component | createMutationKey | createVariableName |
|-----------|-------------------|-------------------|
| DeviceNew | `deviceCreate` | `device` |
| CategoryNew (Device) | `deviceCategoryCreate` | `deviceCategory` |
| CategoryNew (Peripheral) | `peripheralCategoryCreate` | `peripheralCategory` |
| ZoneNew | `zoneCreate` | `zone` |
| CableNew | `cableCreate` | `cable` |
| PeripheralNew | `peripheralCreate` | `peripheral` |
| PortNew | `devicePortCreate` | `port` |
| UserNew | `userCreate` | `user` |
| ScenarioNew | `scenarioCreate` | `scenario` |
| JobNew | `jobCreate` | `job` |

---

## Example: Complete Migration

Here's a complete example for **ZoneNew.vue**:

```vue
<template>
  <q-page padding>
    <form @submit.prevent.stop="onSave" v-if="zone">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-h5 q-mb-md">
            <q-icon name="mdi-plus-circle" color="primary" size="sm" class="q-mr-sm"/>
            Create New Zone
          </div>
        </q-card-section>

        <q-separator/>

        <q-card-section class="q-gutter-md">
          <q-input 
            v-model="zone.name" 
            label="Name" 
            hint="Zone name"
            clearable 
            filled
            dense
            :rules="[val => !!val || 'Name is required']"
          />
          
          <q-input 
            v-model="zone.description" 
            label="Description" 
            clearable 
            filled
            dense
          />

          <q-select
            v-model="zone.parent"
            :options="parentZoneList"
            option-label="name"
            label="Parent Zone"
            clearable
            filled
            dense
          />
        </q-card-section>

        <q-separator/>

        <EntityFormActions
          :saving="saving"
          :show-view="false"
          save-label="Create Zone"
        />
      </q-card>
    </form>
  </q-page>
</template>

<script>
import {defineComponent, onMounted, ref} from 'vue';
import {useApolloClient} from "@vue/apollo-composable";
import {useEntityCRUD} from '@/composables';
import EntityFormActions from '@/components/EntityFormActions.vue';
import {ZONE_CREATE, ZONES_GET_ALL} from '@/graphql/queries';

export default defineComponent({
  name: 'ZoneNew',
  components: {
    EntityFormActions
  },
  setup() {
    const {client} = useApolloClient();
    const parentZoneList = ref([]);

    const {
      entity: zone,
      saving,
      createEntity,
      validateRequired
    } = useEntityCRUD({
      entityName: 'Zone',
      entityPath: '/admin/zones',
      createMutation: ZONE_CREATE,
      createMutationKey: 'zoneCreate',
      createVariableName: 'zone',
      excludeFields: ['__typename'],
      initialData: {
        name: '',
        description: '',
        parent: null
      },
      transformBeforeSave: (data) => {
        const transformed = {...data};
        // Clean parent - only send ID if selected
        if (transformed.parent) {
          if (transformed.parent.id) {
            transformed.parent = { id: transformed.parent.id };
          } else {
            delete transformed.parent;
          }
        }
        return transformed;
      }
    });

    const fetchParentZones = async () => {
      try {
        const response = await client.query({
          query: ZONES_GET_ALL,
          fetchPolicy: 'network-only',
        });
        parentZoneList.value = response.data.zoneList || [];
      } catch (error) {
        console.error('Error fetching zones:', error);
      }
    };

    const onSave = async () => {
      if (saving.value) return;
      if (!validateRequired(zone.value, ['name'])) return;
      await createEntity();
    };

    onMounted(() => {
      fetchParentZones();
    });

    return {
      zone,
      saving,
      onSave,
      parentZoneList
    };
  }
});
</script>
```

---

## Summary

**Pattern is consistent across all components:**
1. Add `v-if="entity"` to form
2. Replace buttons with `<EntityFormActions>`
3. Use `useEntityCRUD` with `initialData`
4. Set `excludeFields: ['__typename']`
5. Simplify `onSave` to use `createEntity()`
6. Add `transformBeforeSave` if needed for nested objects

**Time estimate per component:** 10-15 minutes

---

**Last Updated:** November 3, 2025  
**Completed:** 3/10 (30%)  
**Remaining:** 7 components

