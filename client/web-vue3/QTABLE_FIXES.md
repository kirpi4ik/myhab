# QTable Initialization Fixes

## Issue
QTable components were throwing `Cannot read properties of undefined (reading 'length')` errors when trying to render before data was loaded.

## Root Cause
Several list components were initializing the `rows` ref with `ref()` (undefined) instead of `ref([])` (empty array). When QTable tried to access `rows.length` during the initial render, it failed because `undefined` has no `length` property.

## Files Fixed

1. **PeripheralList.vue** - Line 75
2. **ZoneList.vue** - Line 68
3. **UserList.vue** - Line 76
4. **PeripheralCategories/CategoryList.vue** - Line 68
5. **DeviceList.vue** - Line 75
6. **DeviceCategories/CategoryList.vue** - Line 68

## Change Made

### Before:
```javascript
const rows = ref();
```

### After:
```javascript
const rows = ref([]);
```

## Why This Fixes It

- QTable expects `rows` to be an array (even if empty) to calculate pagination and render properly
- Initializing with `ref([])` ensures:
  - `rows.value` is an array from the start
  - `rows.value.length` returns `0` instead of throwing an error
  - QTable can safely render an empty state while waiting for data
- Data loading is asynchronous, so the component mounts before data arrives

## Best Practice

Always initialize array refs with empty arrays:
```javascript
✅ const rows = ref([]);        // Correct
❌ const rows = ref();           // Incorrect - causes errors
❌ const rows = ref(null);       // Incorrect - causes errors
❌ const rows = ref(undefined);  // Incorrect - causes errors
```

## Verification

Run the application and navigate to any of the list pages:
- Peripheral List
- Zone List  
- User List
- Device List
- Cable List
- Category Lists

All should now render without errors, showing an empty table while loading, then populating with data when it arrives.

---

*Fixed: October 19, 2025*
*Part of Quasar CLI v3 → v4 migration bug fixes*

