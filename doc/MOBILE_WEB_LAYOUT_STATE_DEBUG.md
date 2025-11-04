# MobileWebLayout State Initialization Debug

## Issue

All SVG elements that should have class `bulb-on` are showing `bulb-off` instead, indicating that peripheral state is not being initialized correctly.

## Root Cause Analysis

### Expected Behavior (from old implementation)

In `MobileWebLayout_OLD.vue` lines 296-347:

```javascript
if (svgElement.type === 'asset') {
  let srvAsset = this.srvPeripherals[svgElement['id']];
  switch (svgElement['category']) {
    case 'LIGHT': {
      if (srvAsset && srvAsset.state) {
        actionElementClass = 'bulb-on';
      } else {
        actionElementClass = 'bulb-off';
      }
      break;
    }
    // ... other cases
  }
}
```

Key points:
1. The category comes from the SVG element ID (e.g., `asset-LIGHT-123`)
2. The peripheral is looked up by the parsed ID
3. If peripheral exists AND `state === true`, apply `'bulb-on'`
4. Otherwise, default to `'bulb-off'`

### State Initialization (from usePeripheralState.js)

```javascript
const initializePeripheralState = (peripheral) => {
  if (!peripheral.connectedTo || peripheral.connectedTo.length === 0) return;

  const port = peripheral.connectedTo[0];
  if (!port) return;

  peripheral.portValue = port.value;
  peripheral.state = peripheral.portValue === 'ON';  // Boolean conversion
  peripheral.portId = port.id;
  peripheral.portUid = port.uid;
  peripheral.deviceStatus = port.device?.status;
};
```

The state is set as: `peripheral.state = peripheral.portValue === 'ON'`

This means:
- If `port.value === 'ON'`, then `peripheral.state === true` → should show `'bulb-on'`
- If `port.value === 'OFF'`, then `peripheral.state === false` → should show `'bulb-off'`
- If `port.value` is anything else, `peripheral.state === false` → should show `'bulb-off'`

## Potential Issues

### 1. Peripheral Not Found

**Symptom**: All lights show as `'bulb-off'`

**Possible Causes**:
- SVG element ID doesn't match peripheral ID in the database
- Peripheral ID parsing is incorrect
- Peripherals map is empty or not populated

**Debug Steps**:
1. Check if `peripherals` object has any keys
2. Check what IDs are in the SVG elements
3. Check what IDs are returned from the GraphQL query
4. Verify the ID parsing logic

### 2. State Not Set

**Symptom**: Peripheral is found but `state` is always `false`

**Possible Causes**:
- `port.value` is not `'ON'` (might be lowercase `'on'`, or `'1'`, etc.)
- `connectedTo` array is empty
- Port data is not included in the GraphQL response

**Debug Steps**:
1. Check the GraphQL response for `connectedTo[0].value`
2. Verify the value is exactly `'ON'` (uppercase)
3. Check if `initializePeripheralState` is being called for all peripherals

### 3. Timing Issue

**Symptom**: State is correct in data but SVG shows wrong class

**Possible Causes**:
- SVG is transformed before peripherals are fully loaded
- Race condition between data loading and SVG rendering

**Debug Steps**:
1. Verify `v-if="!loading"` is working correctly
2. Check if `loading` is set to `false` after peripherals are loaded
3. Add console logs to track the timing of data loading vs SVG transformation

## Debug Logging Added

Added comprehensive logging to `useSvgInteraction.js` (lines 102-112):

```javascript
// Debug logging
if (process.env.DEV && parsed.category === 'LIGHT') {
  console.log('SVG Element:', element.id);
  console.log('Parsed ID:', parsed.id);
  console.log('Peripheral found:', !!peripheral);
  if (peripheral) {
    console.log('Peripheral state:', peripheral.state);
    console.log('Peripheral portValue:', peripheral.portValue);
    console.log('Peripheral category:', peripheral.category);
  }
}
```

This will log:
- The full SVG element ID
- The parsed peripheral ID
- Whether the peripheral was found in the map
- The peripheral's state, portValue, and category (if found)

## Testing Checklist

To diagnose the issue, check the browser console for:

- [ ] Are there any LIGHT elements in the SVG?
- [ ] What are the SVG element IDs? (format: `asset-LIGHT-{id}`)
- [ ] Are peripherals being loaded? (check `peripherals.value` in Vue DevTools)
- [ ] Do the peripheral IDs match the SVG element IDs?
- [ ] What is the `portValue` for each peripheral? (should be `'ON'` or `'OFF'`)
- [ ] What is the `state` for each peripheral? (should be boolean)
- [ ] Is the GraphQL query returning `connectedTo[0].value`?

## Expected Console Output

For a light that should be ON:
```
SVG Element: asset-LIGHT-123
Parsed ID: 123
Peripheral found: true
Peripheral state: true
Peripheral portValue: ON
Peripheral category: { id: 1, uid: "...", title: "Light", name: "LIGHT" }
```

For a light that should be OFF:
```
SVG Element: asset-LIGHT-456
Parsed ID: 456
Peripheral found: true
Peripheral state: false
Peripheral portValue: OFF
Peripheral category: { id: 1, uid: "...", title: "Light", name: "LIGHT" }
```

For a peripheral not found:
```
SVG Element: asset-LIGHT-789
Parsed ID: 789
Peripheral found: false
```

## Next Steps

1. **Run the application** and check the browser console for debug logs
2. **Identify the issue** based on the console output:
   - If no LIGHT elements are logged → SVG doesn't have LIGHT assets
   - If peripheral not found → ID mismatch issue
   - If peripheral found but state is always false → Check `portValue` and state initialization
3. **Fix the root cause** based on findings

## Related Files

- `client/web-vue3/src/composables/useSvgInteraction.js` - SVG element initialization
- `client/web-vue3/src/composables/usePeripheralState.js` - Peripheral state management
- `client/web-vue3/src/pages/MobileWebLayout.vue` - Main component
- `client/web-vue3/src/graphql/queries/peripherals.js` - GraphQL query
- `client/web-vue3/public/svg/screen-1.svg` - SVG file with asset IDs

