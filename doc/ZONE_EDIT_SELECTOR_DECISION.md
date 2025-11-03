# ZoneEdit Sub-zones Selector - Decision

**Date:** November 3, 2025  
**Component:** `ZoneEdit.vue`  
**Question:** Should we replace the sub-zones selector with `LocationSelector`?

---

## Decision: ❌ **No, Keep Custom Implementation**

---

## Reasoning

### 1. Different Use Cases

**LocationSelector:**
- Designed for selecting zones where an entity **exists/is located**
- Used in: `PeripheralEdit.vue`, `DeviceEdit.vue`
- Purpose: Multi-location assignment (an item can be in multiple zones)
- No hierarchical relationship logic

**Sub-zones Selector (ZoneEdit):**
- Designed for selecting **child zones** in a hierarchical structure
- Purpose: Parent-child relationship management
- Requires custom filtering to prevent cyclic references
- Hierarchical relationship logic required

### 2. Custom Filtering Requirements

The sub-zones selector in `ZoneEdit.vue` has **special filtering logic**:

```javascript
const availableSubZones = computed(() => {
  if (!zone.value.id) return zoneList.value;
  
  const selectedZoneIds = (zone.value.zones || []).map(z => z.id);
  
  return zoneList.value.filter(z => {
    // 1. Exclude the current zone itself (prevent self-reference)
    if (z.id === zone.value.id) return false;
    
    // 2. Exclude the parent zone (prevent direct cyclic reference)
    if (zone.value.parent && z.id === zone.value.parent.id) return false;
    
    // 3. Prevent duplicate selections
    if (selectedZoneIds.includes(z.id)) return false;
    
    return true;
  });
});
```

**This logic prevents:**
- ❌ Zone A being its own sub-zone (self-reference)
- ❌ Zone A having its parent as a sub-zone (cyclic reference)
- ❌ Duplicate sub-zone selections

**LocationSelector does NOT support this** - it loads all zones without custom filtering.

### 3. Loss of Functionality

If we replaced it with `LocationSelector`, we would:
- ❌ Lose cyclic reference prevention
- ❌ Lose self-reference prevention
- ❌ Allow invalid hierarchical structures
- ❌ Risk data integrity issues

---

## What We Did Instead

### Improvement: Enhanced the Existing Selector

We made the sub-zones selector **more consistent** with `LocationSelector` style:

**Changes:**
1. ✅ Added `use-input` for search/filter capability
2. ✅ Added `clearable` for easy clearing
3. ✅ Removed custom clear button (now using built-in `clearable`)
4. ✅ Updated hint to explain exclusion logic

**Before:**
```vue
<q-select 
  v-model="zone.zones"
  :options="availableSubZones"
  label="Sub-zones" 
  hint="Select child zones (optional)"
  multiple
  use-chips
>
  <template v-slot:append v-if="zone.zones && zone.zones.length">
    <q-icon 
      name="mdi-close-circle" 
      @click.stop.prevent="zone.zones = []" 
    />
  </template>
</q-select>
```

**After:**
```vue
<q-select 
  v-model="zone.zones"
  :options="availableSubZones"
  label="Sub-zones" 
  hint="Select child zones (optional, excludes current zone and parent)"
  multiple
  use-chips
  use-input
  clearable
>
</q-select>
```

**Benefits:**
- ✅ Maintains custom filtering logic
- ✅ Adds search capability
- ✅ Cleaner UI (uses built-in clearable)
- ✅ Better hint text explaining behavior
- ✅ Consistent with LocationSelector style

---

## When to Use Each Component

### Use `LocationSelector`:
- ✅ Selecting zones where an entity is located
- ✅ Multi-location assignment (no hierarchy)
- ✅ Simple zone selection without custom logic
- ✅ Examples: `PeripheralEdit.vue`, `DeviceEdit.vue`

### Use Custom Selector:
- ✅ Hierarchical relationships (parent-child)
- ✅ Need to prevent cyclic references
- ✅ Need custom filtering logic
- ✅ Examples: `ZoneEdit.vue` (sub-zones, parent zone)

---

## Summary

**Question:** Replace sub-zones selector with `LocationSelector`?  
**Answer:** ❌ No

**Reason:** Different use cases, custom filtering required

**Action Taken:** Enhanced existing selector to be more consistent with `LocationSelector` style while maintaining critical filtering logic.

---

**Decision Made By:** AI Assistant (Claude Sonnet 4.5)  
**Date:** November 3, 2025  
**Status:** ✅ Implemented Enhancement

