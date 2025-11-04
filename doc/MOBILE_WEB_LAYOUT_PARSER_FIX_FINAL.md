# MobileWebLayout SVG ID Parser - Final Fix

## Issue

The initial parser fix didn't work because the old regex pattern `/([A-Za-z_]+)-(([0-9]+)(-([0-9])))*/g` was not correctly parsing the SVG element ID format.

### Console Output Showing the Problem

```
Parsing ID: asset-light-id-2524-2
Matches: (3) [Array(6), Array(6), Array(6)]
Parsed: {type: 'asset', info: 'light', category: 'LIGHT', id: null, assetOrder: null}
Looking for peripheral ID: null
Peripheral found: false
Available peripheral IDs: (5) ['2229', '2496', '2513', '2523', '2524']
```

The parser was returning `id: null` instead of `id: 2524`.

## Root Cause

The old regex pattern matches word-number pairs globally:
- `/([A-Za-z_]+)-(([0-9]+)(-([0-9])))*/g`

For the ID `asset-light-id-2524-2`, this regex matches:
1. `asset` (word only, no number following)
2. `light` (word only, no number following)
3. `id` (word only, no number following)

The pattern does NOT match `2524-2` because there's no word preceding it - it's just numbers!

The `*` quantifier makes the number part optional, so words without numbers still match. But the number part `2524-2` doesn't match at all because it doesn't start with a word.

## Solution

Replaced the complex global regex with simple, specific regex patterns for each format:

### Asset Format: `asset-<category>-id-<id>-<index>`

```javascript
const assetMatch = id.match(/^asset-([a-z]+)-id-(\d+)-(\d+)$/i);
if (assetMatch) {
  return {
    type: 'asset',
    info: assetMatch[1].toLowerCase(),      // 'light'
    category: assetMatch[1].toUpperCase(),  // 'LIGHT'
    id: parseInt(assetMatch[2], 10),        // 2524 (as number)
    assetOrder: assetMatch[3],              // '2'
  };
}
```

**Regex breakdown:**
- `^asset-` - Must start with "asset-"
- `([a-z]+)` - Capture category name (letters only)
- `-id-` - Literal "-id-" separator
- `(\d+)` - Capture peripheral ID (digits)
- `-` - Literal "-" separator
- `(\d+)` - Capture index (digits)
- `$` - Must end here

### Nav Format: `nav-<direction>-<index>`

```javascript
const navMatch = id.match(/^nav-([a-z]+)-(\d+)$/i);
if (navMatch) {
  return {
    type: 'nav',
    info: navMatch[1].toLowerCase(),  // 'back', 'forward', 'home'
    category: null,
    id: null,
    assetOrder: navMatch[2],
  };
}
```

### Fallback for Other Formats

Kept the old regex as a fallback for any unexpected formats.

## Complete Updated Function

```javascript
const parseAssetId = (id) => {
  // Use a more specific regex for asset IDs
  // Format: asset-<category>-id-<id>-<index>
  const assetMatch = id.match(/^asset-([a-z]+)-id-(\d+)-(\d+)$/i);
  if (assetMatch) {
    return {
      type: 'asset',
      info: assetMatch[1].toLowerCase(),
      category: assetMatch[1].toUpperCase(),
      id: parseInt(assetMatch[2], 10),
      assetOrder: assetMatch[3],
    };
  }
  
  // Format: nav-<direction>-<index>
  const navMatch = id.match(/^nav-([a-z]+)-(\d+)$/i);
  if (navMatch) {
    return {
      type: 'nav',
      info: navMatch[1].toLowerCase(),
      category: null,
      id: null,
      assetOrder: navMatch[2],
    };
  }
  
  // Fallback to old regex for any other format
  const matches = [...id.matchAll(/([A-Za-z_]+)-(([0-9]+)(-([0-9])))*/g)];
  return {
    type: matches[0]?.[1] || null,
    info: matches[1]?.[1] || null,
    category: matches[1]?.[1]?.toUpperCase() || null,
    id: matches[2]?.[3] ? parseInt(matches[2]?.[3], 10) : null,
    assetOrder: matches[2]?.[5] || null,
  };
};
```

## Test Cases

### Asset IDs

| Input | Output |
|-------|--------|
| `asset-light-id-2524-1` | `{ type: 'asset', info: 'light', category: 'LIGHT', id: 2524, assetOrder: '1' }` |
| `asset-light-id-2524-2` | `{ type: 'asset', info: 'light', category: 'LIGHT', id: 2524, assetOrder: '2' }` |
| `asset-light-id-2229-1` | `{ type: 'asset', info: 'light', category: 'LIGHT', id: 2229, assetOrder: '1' }` |
| `asset-heat-id-257551-1` | `{ type: 'asset', info: 'heat', category: 'HEAT', id: 257551, assetOrder: '1' }` |
| `asset-temp-id-6872-1` | `{ type: 'asset', info: 'temp', category: 'TEMP', id: 6872, assetOrder: '1' }` |
| `asset-temp-id-3736300-1` | `{ type: 'asset', info: 'temp', category: 'TEMP', id: 3736300, assetOrder: '1' }` |

### Nav IDs

| Input | Output |
|-------|--------|
| `nav-home-1` | `{ type: 'nav', info: 'home', category: null, id: null, assetOrder: '1' }` |
| `nav-back-1` | `{ type: 'nav', info: 'back', category: null, id: null, assetOrder: '1' }` |
| `nav-forward-1` | `{ type: 'nav', info: 'forward', category: null, id: null, assetOrder: '1' }` |

## Expected Console Output After Fix

```
Element ID: asset-light-id-2524-2
Parsed: {type: 'asset', info: 'light', category: 'LIGHT', id: 2524, assetOrder: '2'}
Looking for peripheral ID: 2524
Peripheral found: true
Peripheral: { id: 2524, state: true, portValue: 'ON', category: 'LIGHT' }
```

## Benefits of New Approach

1. **Simpler and more readable**: Each format has its own clear regex pattern
2. **More reliable**: Exact matching with `^` and `$` anchors
3. **Better performance**: Single match instead of global matching
4. **Easier to debug**: Clear separation between asset and nav formats
5. **Type safety**: Explicit `parseInt()` for ID conversion
6. **Fallback support**: Old regex still available for unexpected formats

## Files Modified

- **`client/web-vue3/src/composables/useSvgInteraction.js`**
  - Completely rewrote `parseAssetId()` function
  - Added specific regex patterns for asset and nav formats
  - Kept old regex as fallback

## Verification

To verify the fix:

1. Reload the application
2. Check browser console for debug logs
3. Confirm peripheral IDs are correctly parsed (not null)
4. Confirm peripherals are found in the map
5. Verify visual state matches peripheral state:
   - Lights with `state: true` show bright/yellow (`bulb-on`)
   - Lights with `state: false` show dim/blue (`bulb-off`)

## Related Documents

- **MOBILE_WEB_LAYOUT_PARSER_FIX.md** - Initial (failed) attempt
- **MOBILE_WEB_LAYOUT_INITIALIZATION_FIX.md** - Timing and loading fixes
- **MOBILE_WEB_LAYOUT_STATE_DEBUG.md** - Debug approach

## Conclusion

The parser now correctly extracts peripheral IDs from SVG element IDs using a simple, direct regex approach. All peripherals should now display their correct state in the UI.

