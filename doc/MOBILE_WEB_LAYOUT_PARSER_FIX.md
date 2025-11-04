# MobileWebLayout SVG ID Parser Fix

## Issue

All SVG elements that should have class `bulb-on` were showing `bulb-off` instead, indicating that peripheral state was not being applied correctly.

## Root Cause

The SVG element ID parser was not correctly extracting the peripheral ID from the SVG element IDs due to:

1. **Incorrect index in matches array**: The parser was looking at `matches[2]` for the ID, but with the `id-` prefix in the format, the actual ID is at `matches[3]`.

2. **Type mismatch**: The parser was returning the ID as a string, but the peripherals map uses numeric IDs as keys.

## SVG Element ID Format

The actual format used in the SVG files is:

```
asset-<category>-id-<id>-<index>
```

Examples:
- `asset-light-id-2524-1` → Light peripheral with ID 2524, instance 1
- `asset-light-id-2524-2` → Light peripheral with ID 2524, instance 2
- `asset-light-id-2229-1` → Light peripheral with ID 2229, instance 1
- `asset-heat-id-257551-1` → Heat peripheral with ID 257551, instance 1
- `asset-temp-id-6872-1` → Temperature sensor with ID 6872, instance 1
- `asset-temp-id-3736300-1` → Temperature sensor with ID 3736300, instance 1

### Regex Matching Breakdown

The regex `/([A-Za-z_]+)-(([0-9]+)(-([0-9])))*/g` matches word-number patterns globally.

For `asset-light-id-2524-1`:

| Match Index | Matched Text | Group 1 | Group 3 | Group 5 |
|-------------|--------------|---------|---------|---------|
| 0 | `asset` | `'asset'` | - | - |
| 1 | `light` | `'light'` | - | - |
| 2 | `id` | `'id'` | - | - |
| 3 | `2524-1` | - | `'2524'` | `'1'` |

Therefore:
- `matches[0][1]` = `'asset'` (type)
- `matches[1][1]` = `'light'` (category, lowercase)
- `matches[2][1]` = `'id'` (literal "id" prefix)
- `matches[3][3]` = `'2524'` (peripheral ID)
- `matches[3][5]` = `'1'` (asset order/index)

## Solution

### 1. Fixed Array Index

Changed from `matches[2][3]` to `matches[3][3]` to correctly extract the peripheral ID:

```javascript
// Before
id: matches[2]?.[3] || null,

// After
id: matches[3]?.[3] || null,
```

### 2. Added Type Conversion

Convert the parsed ID string to a number to match the peripherals map key type:

```javascript
const rawId = matches[3]?.[3] || null;

return {
  // ...
  id: rawId ? parseInt(rawId, 10) : null,  // Convert to number
  // ...
};
```

### 3. Enhanced Debug Logging

Added comprehensive debug logging to help diagnose parsing issues:

```javascript
// Log parsing details
if (process.env.DEV && id.startsWith('asset-')) {
  console.log('Parsing ID:', id);
  console.log('Matches:', matches);
}

// Log peripheral lookup details
if (process.env.DEV && parsed.category === 'LIGHT') {
  console.log('=== SVG Element Debug ===');
  console.log('Element ID:', element.id);
  console.log('Parsed:', parsed);
  console.log('Looking for peripheral ID:', parsed.id);
  console.log('Peripheral found:', !!peripheral);
  if (peripheral) {
    console.log('Peripheral:', {
      id: peripheral.id,
      state: peripheral.state,
      portValue: peripheral.portValue,
      category: peripheral.category?.name
    });
  } else {
    console.log('Available peripheral IDs:', Object.keys(peripherals || {}).slice(0, 5));
  }
  console.log('========================');
}
```

## Updated Parser Function

```javascript
/**
 * Parse asset ID from SVG element ID
 * Format: 
 *   - "asset-<category>-id-<id>-<index>" (e.g., "asset-light-id-2524-1")
 *   - "nav-<direction>-<index>" (e.g., "nav-back-1")
 * Examples: 
 *   - "asset-light-id-2524-1" → { type: 'asset', category: 'LIGHT', id: 2524, assetOrder: '1' }
 *   - "nav-back-1" → { type: 'nav', info: 'back' }
 */
const parseAssetId = (id) => {
  const matches = [...id.matchAll(/([A-Za-z_]+)-(([0-9]+)(-([0-9])))*/g)];
  
  // For debugging in development
  if (process.env.DEV && id.startsWith('asset-')) {
    console.log('Parsing ID:', id);
    console.log('Matches:', matches);
  }
  
  const rawId = matches[3]?.[3] || null;
  
  return {
    type: matches[0]?.[1] || null,           // 'asset' or 'nav'
    info: matches[1]?.[1] || null,           // category name (lowercase) or nav direction
    category: matches[1]?.[1]?.toUpperCase() || null,  // category name (uppercase)
    id: rawId ? parseInt(rawId, 10) : null,  // peripheral ID as number (after 'id-' prefix)
    assetOrder: matches[3]?.[5] || null,     // index number
  };
};
```

## Expected Behavior

After the fix:

1. **Correct ID Extraction**: `asset-light-id-2524-1` → `{ type: 'asset', category: 'LIGHT', id: 2524, assetOrder: '1' }`

2. **Successful Peripheral Lookup**: The numeric ID (2524) will correctly match the peripheral in the map

3. **Correct State Application**:
   - If `peripheral.state === true` → CSS class `'bulb-on'`
   - If `peripheral.state === false` → CSS class `'bulb-off'`

4. **Visual Feedback**: Lights that are ON will now display with the yellow/bright color (`bulb-on`), and lights that are OFF will display with the blue/dim color (`bulb-off`)

## Testing

### Console Output Examples

**Successful parsing and lookup:**
```
Parsing ID: asset-light-id-2524-1
Matches: [Array of match objects]
=== SVG Element Debug ===
Element ID: asset-light-id-2524-1
Parsed: { type: 'asset', info: 'light', category: 'LIGHT', id: 2524, assetOrder: '1' }
Looking for peripheral ID: 2524
Peripheral found: true
Peripheral: { id: 2524, state: true, portValue: 'ON', category: 'LIGHT' }
========================
```

**Failed lookup (ID not found):**
```
Parsing ID: asset-light-id-9999-1
Matches: [Array of match objects]
=== SVG Element Debug ===
Element ID: asset-light-id-9999-1
Parsed: { type: 'asset', info: 'light', category: 'LIGHT', id: 9999, assetOrder: '1' }
Looking for peripheral ID: 9999
Peripheral found: false
Available peripheral IDs: [2524, 2229, 257551, 6872, 3736300]
========================
```

## Files Modified

1. **`client/web-vue3/src/composables/useSvgInteraction.js`**
   - Updated `parseAssetId()` function to correctly extract ID from `matches[3][3]`
   - Added type conversion to parse ID as integer
   - Enhanced debug logging for development
   - Updated documentation with correct format examples

## Related Issues

- **MOBILE_WEB_LAYOUT_INITIALIZATION_FIX.md** - Fixed timing issues with peripheral loading
- **MOBILE_WEB_LAYOUT_STATE_DEBUG.md** - Debug approach for state initialization issues

## Verification

To verify the fix is working:

1. Open the application in development mode
2. Open browser console
3. Look for debug logs showing successful peripheral lookups
4. Verify that lights with `state: true` show bright/yellow color
5. Verify that lights with `state: false` show dim/blue color
6. Test clicking on lights to toggle them
7. Verify WebSocket updates reflect in real-time

## Conclusion

The parser fix resolves the state initialization issue by:
1. Correctly extracting the peripheral ID from the SVG element ID format
2. Converting the ID to a number to match the peripherals map key type
3. Providing comprehensive debug logging for troubleshooting

All SVG elements should now display the correct visual state based on their peripheral data.

