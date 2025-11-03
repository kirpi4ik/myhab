# Quick Reference Guide - MyHAB Vue3

## Common Imports

```javascript
// Composables
import { useNotifications, useEntityCRUD, useEntityList } from '@/composables';

// Components
import EntityInfoPanel from '@/components/EntityInfoPanel.vue';
import EntityFormActions from '@/components/EntityFormActions.vue';
import PortConnectCard from '@/components/cards/PortConnectCard.vue';

// Helpers
import { prepareForMutation } from '@/_helpers/apollo-utils';

// Vue & Quasar
import { defineComponent, ref, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useQuasar } from 'quasar';
import { useApolloClient } from '@vue/apollo-composable';

// Utilities
import _ from 'lodash';
import { format } from 'date-fns';
```

---

## Quick Snippets

### Notifications
```javascript
const { notifySuccess, notifyError } = useNotifications();

notifySuccess('Operation successful');
notifyError('Operation failed', error);
```

### CRUD Operations
```javascript
const { entity, loading, saving, fetchEntity, updateEntity } = useEntityCRUD({
  entityName: 'Cable',
  entityPath: '/admin/cables',
  getQuery: CABLE_GET_BY_ID,
  updateMutation: CABLE_UPDATE
});
```

### List Operations
```javascript
const { items: filteredItems, loading, filter, deleteItem } = useEntityList({
  entityName: 'Cable',
  entityPath: '/admin/cables',
  listQuery: CABLE_LIST_ALL,
  deleteMutation: CABLE_DELETE
});
```

### Form Input with Icon
```vue
<q-input
  v-model="entity.code"
  label="Code"
  hint="Unique identifier"
  clearable
  filled
  dense
  :rules="[val => !!val || 'Required']"
>
  <template v-slot:prepend>
    <q-icon name="mdi-barcode"/>
  </template>
</q-input>
```

### Info Panel
```vue
<EntityInfoPanel
  :entity="cable"
  :extra-info="[
    { icon: 'mdi-ethernet', label: 'Ports', value: cable.ports?.length }
  ]"
/>
```

### Form Actions
```vue
<EntityFormActions
  :saving="saving"
  :show-view="true"
  :view-route="`/admin/cables/${id}/view`"
/>
```

### Data Cleaning
```javascript
const cleanData = prepareForMutation(entity.value, [
  '__typename', 'id', 'uid', 'tsCreated', 'tsUpdated'
]);

if (cleanData.rack) cleanData.rack = { id: cleanData.rack.id };
```

---

## Common Icons

| Purpose | Icon |
|---------|------|
| Edit | `mdi-pencil` |
| Delete | `mdi-delete` |
| View | `mdi-eye` |
| Add | `mdi-plus` |
| Save | `mdi-content-save` |
| Cancel | `mdi-cancel` |
| Search | `mdi-magnify` |
| Filter | `mdi-filter` |
| Export | `mdi-download` |
| Refresh | `mdi-refresh` |
| Settings | `mdi-cog` |
| Info | `mdi-information` |
| Warning | `mdi-alert` |
| Error | `mdi-alert-circle` |
| Success | `mdi-check-circle` |
| Loading | `mdi-loading mdi-spin` |

---

## File Locations

| Type | Location |
|------|----------|
| Pages | `src/pages/infra/[entity]/` |
| Components | `src/components/` |
| Composables | `src/composables/` |
| GraphQL Queries | `src/graphql/queries/` |
| Helpers | `src/_helpers/` |
| Services | `src/_services/` |
| Layouts | `src/layouts/` |
| Router | `src/router/` |
| Store | `src/store/` |

---

## Naming Conventions

### Files
- Components: `PascalCase.vue` (e.g., `CableEdit.vue`)
- Composables: `camelCase.js` (e.g., `useEntityCRUD.js`)
- Queries: `kebab-case.js` (e.g., `cables.js`)

### GraphQL
- Queries: `ENTITY_ACTION` (e.g., `CABLE_LIST_ALL`)
- Mutations: `ENTITY_ACTION` (e.g., `CABLE_UPDATE`)

### Routes
- Pattern: `/admin/[entity-plural]/[:id]/[action]`
- Examples: `/admin/cables`, `/admin/cables/123/edit`

---

## Common Patterns

### Edit Page Structure
1. Header with title and entity name
2. Info panel (ID, UID, timestamps)
3. Sectioned form fields
4. Form actions (Save, Cancel, View)
5. Loading spinner

### List Page Structure
1. Header with title and search
2. Add button
3. Table with columns
4. Action buttons per row
5. Delete confirmation dialog

### View Page Structure
1. Header with title and edit button
2. Info panel
3. Detail sections
4. Related entities tables
5. Loading spinner

---

## Quick Commands

```bash
# Development
yarn dev

# Build
yarn build

# Lint
yarn lint

# Test
yarn test
```

---

## Documentation Links

- **Full Development Guide:** `client/web-vue3/DEVELOPMENT_GUIDE.md`
- **Frontend Audit Report:** `doc/FRONTEND_AUDIT_REPORT.md`
- **Optimization Summary:** `doc/FRONTEND_OPTIMIZATION_SUMMARY.md`
- **PortConnectCard Docs:** `client/web-vue3/src/components/cards/README_PortConnectCard.md`

---

**Last Updated:** November 3, 2025

