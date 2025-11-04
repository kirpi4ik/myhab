# MobileWebLayout Implementation Conventions and Fixes

## Overview

This document details the conventions used in the MobileWebLayout implementation and the fixes applied to ensure the new composable-based implementation follows the same conventions as the old implementation.

## Core Concepts and Conventions

### 1. SVG Structure

- **Background Images**: `screen-1.svg` and `screen-2.svg` contain base64-encoded background images
- **SVG Elements**: Over the background images, SVG elements are drawn to represent assets
- **Assets**: Each SVG element represents an "asset" - a synthetic peripheral instance
- **Multiple Assets per Peripheral**: One peripheral can have multiple assets (visual representations)

### 2. SVG Element ID Format

**Format**: `asset-<category>-id-<id>-<index>`

**Examples**:
- `asset-light-id-2524-1` → Peripheral ID 2524, Category LIGHT, Instance 1
- `asset-light-id-2524-2` → Peripheral ID 2524, Category LIGHT, Instance 2
- `asset-heat-id-257551-1` → Peripheral ID 257551, Category HEAT, Instance 1
- `asset-temp-id-6872-1` → Peripheral ID 6872, Category TEMP, Instance 1

**Purpose**: Correlates UI elements with backend peripherals

### 3. Peripheral State Convention

**Important**: Since a peripheral can have multiple ports connected and multiple assets:

> **By convention, asset state is represented by the first port value connected to this peripheral**

```javascript
// From usePeripheralState.js
const initializePeripheralState = (peripheral) => {
  const port = peripheral.connectedTo[0];  // First port!
  peripheral.portValue = port.value;
  peripheral.state = peripheral.portValue === 'ON';
  peripheral.portId = port.id;
  peripheral.portUid = port.uid;
  peripheral.deviceStatus = port.device?.status;
};
```

### 4. CSS Class Assignment During Initialization

Every SVG element is altered during initialization by adding a CSS class via `svgEl.setAttribute('class', ...)`:

| Category | State ON | State OFF | Special Cases |
|----------|----------|-----------|---------------|
| LIGHT | `bulb-on` | `bulb-off` | - |
| HEAT | `heat-on` | `heat-off` | - |
| MOTION | `motion-off` | `motion-on` | **Inverted logic!** |
| TEMP | - | - | `device-offline` if device offline |
| DOOR_LOCK | `lock` | `lock` | Always same class |
| LUMINOSITY | `luminosity-text` | `luminosity-text` | Text element |

**Purpose**: CSS classes control visualization for:
- Asset ON state (bright/active)
- Asset OFF state (dim/inactive)
- Asset clicked (focus effect)
- Asset value changes (real-time updates)

### 5. Interactive Assets

**LIGHT, HEAT, MOTION** assets can have two states: **ON/OFF**

These states are important:
- During initialization (to set correct CSS class)
- When clicking on an asset (to toggle state)

### 6. Click Interaction Flow

When a user clicks on an SVG element in the browser:

1. **Identify closest SVG element** (using `findClosestNode()`)
2. **Parse element ID** to identify the asset and peripheral
3. **Invoke `pushEvent`** with the appropriate event structure

### 7. Event Structure Convention

**Format**: `pushEvent` has the following structure:

```javascript
{
  p0: "evt_<category>",        // Event name based on category
  p1: "PERIPHERAL",            // Entity type (always PERIPHERAL for asset actions)
  p2: <peripheral_id>,         // Peripheral ID (number)
  p3: "mweb",                  // Source identifier (mobile web)
  p4: <new_state>,             // New state value (lowercase: 'on'/'off')
  p5: <extra_payload>,         // Optional extra data (JSON string or omitted)
  p6: <username>               // Current user login
}
```

**Examples**:

**Light Toggle**:
```javascript
{
  p0: 'evt_light',
  p1: 'PERIPHERAL',
  p2: 2524,
  p3: 'mweb',
  p4: 'on',  // lowercase!
  p6: 'admin'
}
```

**Heat Toggle**:
```javascript
{
  p0: 'evt_heat',
  p1: 'PERIPHERAL',
  p2: 257551,
  p3: 'mweb',
  p4: 'off',  // lowercase!
  p6: 'admin'
}
```

**Door Lock**:
```javascript
{
  p0: 'evt_intercom_door_lock',
  p1: 'PERIPHERAL',
  p2: 12345,
  p3: 'mweb',
  p4: 'open',
  p5: '{"unlockCode": "1234"}',
  p6: 'admin'
}
```

### 8. WebSocket Event Updates

When a WebSocket event is received with `eventName: 'evt_port_value_persisted'`:

1. Parse the `jsonPayload`
2. Use `portToPeripheralMap` to find affected peripherals
3. Update each peripheral's `portValue` and `state`
4. State is uppercase: `payload.p4 === 'ON'` (boolean result)

**Important**: 
- **Outgoing events** use lowercase: `'on'`, `'off'`
- **Incoming events** use uppercase: `'ON'`, `'OFF'`

## Issues Found and Fixed

### Issue 1: Wrong Event Structure

**Problem**: The new implementation was using:
```javascript
{
  p0: 'evt_port_value_change',  // Wrong event name
  p1: 'PORT',                    // Wrong entity type
  p2: port.id,                   // Wrong ID (port instead of peripheral)
  p3: 'mweb',
  p4: 'ON',                      // Wrong case (uppercase instead of lowercase)
  p5: '{}',                      // Unnecessary p5
  p6: 'admin'
}
```

**Fix**: Changed to correct format:
```javascript
{
  p0: 'evt_light',               // Correct event name
  p1: 'PERIPHERAL',              // Correct entity type
  p2: peripheral.id,             // Correct ID (peripheral)
  p3: 'mweb',
  p4: 'on',                      // Correct case (lowercase)
  p6: 'admin'                    // p5 omitted
}
```

**Files Modified**:
- `client/web-vue3/src/composables/usePeripheralControl.js`
  - `toggleLight()` function (lines 30-36)
  - `toggleHeat()` function (lines 65-71)

### Issue 2: Debugger Statement

**Problem**: Left a `debugger` statement in production code (line 26)

**Fix**: Removed the debugger statement

**File Modified**:
- `client/web-vue3/src/composables/usePeripheralControl.js` (line 26)

### Issue 3: Optimistic Update Case Mismatch

**Problem**: The optimistic update was setting `peripheral.portValue = newValue` where `newValue` is lowercase, but the WebSocket handler expects uppercase for comparison.

**Fix**: Convert to uppercase for the optimistic update:
```javascript
peripheral.portValue = newValue.toUpperCase();  // 'on' → 'ON'
```

**Files Modified**:
- `client/web-vue3/src/composables/usePeripheralControl.js`
  - `toggleLight()` function (line 46)
  - `toggleHeat()` function (line 81)

## Implementation Checklist

- [x] SVG element ID parsing follows format: `asset-<category>-id-<id>-<index>`
- [x] Peripheral state is determined by first connected port value
- [x] CSS classes are assigned during SVG initialization
- [x] LIGHT assets: `bulb-on` / `bulb-off`
- [x] HEAT assets: `heat-on` / `heat-off`
- [x] MOTION assets: `motion-off` / `motion-on` (inverted)
- [x] TEMP assets: Show temperature value, `device-offline` if offline
- [x] DOOR_LOCK assets: `lock` class, unlock dialog
- [x] Click interaction: Find closest node, parse ID, invoke event
- [x] Event structure: `p0: 'evt_<category>'`, `p1: 'PERIPHERAL'`, `p2: peripheral.id`
- [x] Event state values: lowercase (`'on'`, `'off'`)
- [x] WebSocket updates: Use `portToPeripheralMap`, update state with uppercase comparison
- [x] Optimistic updates: Convert to uppercase for `portValue`
- [x] No debugger statements in production code

## Code References

### Event Sending (Light)

```javascript
// client/web-vue3/src/composables/usePeripheralControl.js
const toggleLight = async (peripheral) => {
  const newValue = peripheral.state ? 'off' : 'on';
  
  const event = {
    p0: 'evt_light',
    p1: 'PERIPHERAL',
    p2: peripheral.id,
    p3: 'mweb',
    p4: newValue,
    p6: authzService.currentUserValue?.login || 'unknown',
  };
  
  await client.mutate({
    mutation: PUSH_EVENT,
    variables: { input: event },
  });
  
  // Optimistic update
  peripheral.state = !peripheral.state;
  peripheral.portValue = newValue.toUpperCase();
};
```

### WebSocket Event Handling

```javascript
// client/web-vue3/src/composables/usePeripheralState.js
const updatePeripheralFromEvent = (jsonPayload) => {
  const payload = JSON.parse(jsonPayload);
  const connectedPeripherals = portToPeripheralMap.value[payload.p2];
  
  connectedPeripherals.forEach((peripheralId) => {
    const peripheral = peripherals.value[peripheralId];
    if (peripheral) {
      peripheral.portValue = payload.p4;          // 'ON' or 'OFF' (uppercase)
      peripheral.state = payload.p4 === 'ON';     // Boolean
    }
  });
};
```

### SVG Element Initialization

```javascript
// client/web-vue3/src/composables/useSvgInteraction.js
const initializeSvgElement = (svg, element, peripherals, svgPages, currentPageId) => {
  const parsed = parseAssetId(element.id);
  const peripheral = peripherals && peripherals[parsed.id];
  
  let cssClass = getAssetClass(parsed.category, peripheral?.state, peripheral?.deviceStatus);
  
  element.setAttribute('class', cssClass);
  // ... wrapper creation and text updates
};
```

## Testing

To verify the implementation follows conventions:

1. **Initial Load**: Check that assets display correct state (ON/OFF)
2. **Click Interaction**: Click on a light/heat asset, verify event is sent with correct structure
3. **WebSocket Updates**: Trigger a port value change from backend, verify UI updates
4. **Console Logs**: Check for correct event structure in network tab
5. **Visual Feedback**: Verify CSS classes change correctly on state changes

## Related Documents

- `MOBILE_WEB_LAYOUT_INITIALIZATION_FIX.md` - Timing and loading fixes
- `MOBILE_WEB_LAYOUT_PARSER_FIX_FINAL.md` - SVG ID parsing fixes
- `MOBILE_WEB_LAYOUT_STATE_DEBUG.md` - State initialization debugging

## Conclusion

The new composable-based implementation now correctly follows all conventions from the old implementation:
- Correct event structure with lowercase state values
- Correct entity type (PERIPHERAL) and ID
- Correct event names based on category
- Proper optimistic updates with uppercase conversion
- Clean code without debug statements

