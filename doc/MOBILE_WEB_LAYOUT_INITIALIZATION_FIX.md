# MobileWebLayout Initialization Fix

## Issue Summary

The optimized `MobileWebLayout.vue` was not initializing correctly due to several critical issues:

1. **Empty peripherals during SVG transformation**: The SVG transformation was happening before peripherals were loaded, resulting in empty peripheral data.
2. **Race condition**: The `inline-svg` component's `transform-source` prop was being called immediately on mount, but `loadPeripherals()` was also called in `onMounted()`, creating a race condition.
3. **Missing swiper instance reference**: Navigation buttons couldn't control the swiper because there was no reference to the swiper instance.
4. **Incorrect page ID checking**: The SVG element initialization was checking `svg.id` instead of using the passed `currentPageId` parameter.

## Root Cause Analysis

### 1. SVG Transformation Timing

In the old implementation (`MobileWebLayout_OLD.vue`):
```javascript
created() {
  this.init();  // Loads peripherals synchronously
},
mounted() {
  document.addEventListener('click', this.handleClick, false);
}
```

The `init()` method (which loads peripherals) was called in the `created` lifecycle hook, ensuring peripherals were loaded before the SVG was rendered.

In the new implementation, both SVG rendering and peripheral loading happened simultaneously in `onMounted()`, causing the SVG to be transformed with empty peripheral data.

### 2. Important SVG Initialization Conventions

From `MobileWebLayout_OLD.vue` lines 292-363, the `svgElInit` function contains critical conventions:

#### a) Element Wrapping
Every SVG element is wrapped in an `<a>` tag:
```javascript
let wrapper = document.createElementNS('http://www.w3.org/2000/svg', 'a');
svgEl.setAttribute('class', actionElementClass);
svgEl.parentNode.insertBefore(wrapper, svgEl);
wrapper.appendChild(svgEl);
```

#### b) Asset State Mapping
- **LIGHT**: `state === true` → `'bulb-on'`, `state === false` → `'bulb-off'`
- **HEAT**: `state === true` → `'heat-on'`, `state === false` → `'heat-off'`
- **MOTION**: `state === true` → `'motion-off'`, `state === false` → `'motion-on'` (inverted!)
- **TEMP**: Updates text content with temperature value, applies `'device-offline'` class if device is offline
- **DOOR_LOCK**: Always applies `'lock'` class
- **LUMINOSITY**: Updates text content with luminosity percentage, applies `'luminosity-text'` class

#### c) Navigation Button Visibility
- **Back button**: Hidden on first page (`svg.id === this.svgPages[0].id`)
- **Forward button**: Hidden on last page (`svg.id === this.svgPages[this.svgPages.length - 1].id`)
- Other nav buttons: Apply `'bulb-off'` class

#### d) Temperature Display Logic
```javascript
if (degree.length == 3 && !degree.includes(".")) {
  degree = degree / 10
}
svgEl.firstChild.textContent = degree + '℃';
```

#### e) Luminosity Display Logic
```javascript
svgEl.getElementsByTagName('text').item(0).textContent = srvAsset.portValue / 10 + '%';
svgEl.getElementsByTagName('text').item(0).setAttribute('class', 'luminosity-text');
```

### 3. Peripheral Data Structure

From `MobileWebLayout_OLD.vue` lines 221-279, the `init()` method:

```javascript
// Peripherals are stored as a map/object with peripheral IDs as keys
this.srvPeripherals = _.reduce(
  assets,
  function (hash, value) {
    var key = value['id'];
    hash[key] = value;
    return hash;
  },
  {},
);
```

Each peripheral has these properties added:
- `portValue`: The current value of the connected port
- `state`: Boolean (`portValue === 'ON'`)
- `portId`: ID of the connected port
- `portUid`: UID of the connected port
- `deviceStatus`: Status of the device (e.g., 'OFFLINE')

## Solutions Implemented

### 1. Conditional SVG Rendering

Added `v-if="!loading"` to the swiper component to prevent SVG transformation until peripherals are loaded:

```vue
<swiper 
  v-if="!loading"
  :pagination="{ dynamicBullets: true }" 
  :modules="modules" 
  class="swiper"
  @slideChange="onSlideChange"
  @swiper="onSwiper"
>
```

**File**: `client/web-vue3/src/pages/MobileWebLayout.vue`

### 2. Fixed Peripheral Access in SVG Transformation

Updated `useSvgInteraction.js` to handle the case where peripherals might be empty or undefined:

```javascript
const peripheral = peripherals && peripherals[parsed.id];
cssClass = getAssetClass(parsed.category, peripheral?.state, peripheral?.deviceStatus);
```

**File**: `client/web-vue3/src/composables/useSvgInteraction.js` (line 100)

### 3. Fixed Page ID Checking

Changed from checking `svg.id` to using the passed `currentPageId` parameter:

```javascript
// Before
if (parsed.info === 'back' && svg.id === svgPages[0].id) {

// After
if (parsed.info === 'back' && currentPageId === svgPages[0].id) {
```

**File**: `client/web-vue3/src/composables/useSvgInteraction.js` (lines 124, 128)

### 4. Added Swiper Instance Reference

Added a ref to store the swiper instance and use it for programmatic navigation:

```javascript
const swiperInstance = ref(null);

const onSwiper = (swiper) => {
  swiperInstance.value = swiper;
};

const handleNavigation = (targetId) => {
  // ...
  if (direction === 'back' && currentPageIndex.value > 0) {
    swiperInstance.value?.slidePrev();
  } else if (direction === 'forward' && currentPageIndex.value < svgPages.value.length - 1) {
    swiperInstance.value?.slideNext();
  }
};
```

**File**: `client/web-vue3/src/pages/MobileWebLayout.vue` (lines 108, 184-186, 175-177)

### 5. Removed Debug Statements

Removed `debugger` statements from `useSvgInteraction.js`:

```javascript
// Removed from lines 61 and 101
```

**File**: `client/web-vue3/src/composables/useSvgInteraction.js`

### 6. Enhanced Unlock Dialog

Added `:disable="unlocking"` to prevent multiple unlock attempts:

```vue
<q-btn 
  flat 
  class="text-h6" 
  icon="mdi-lock-open" 
  :label="$t('mobile.unlock.button', 'Deschide')" 
  no-caps 
  @click="handleUnlock"
  :loading="unlocking"
  :disable="unlocking"
/>
```

**File**: `client/web-vue3/src/pages/MobileWebLayout.vue` (line 54)

## Data Flow

### Initialization Sequence

1. **Component Mount** → `onMounted()` called
2. **Initialize** → `loadPeripherals()` called (sets `loading = true`)
3. **GraphQL Query** → Fetch peripheral data from backend
4. **Data Processing**:
   - Initialize peripheral maps (`portToPeripheralMap`, `assetMap`)
   - Initialize peripheral states (`portValue`, `state`, `portId`, `portUid`, `deviceStatus`)
   - Convert array to map by ID
5. **Loading Complete** → `loading = false`
6. **SVG Rendering** → `v-if="!loading"` allows swiper to render
7. **SVG Transformation** → `transform-source` prop calls `transformSvg()` with loaded peripheral data
8. **Element Initialization** → Each SVG element is initialized with correct CSS classes based on peripheral state

### WebSocket Update Flow

1. **WebSocket Event** → `evt_port_value_persisted` received
2. **Update Peripheral** → `updatePeripheralFromEvent()` called
3. **Find Peripherals** → Use `portToPeripheralMap` to find affected peripherals
4. **Update State** → Update `portValue` and `state` for each peripheral
5. **Re-render** → Vue's reactivity triggers SVG re-transformation (if needed)

## Testing Checklist

- [x] Peripherals load correctly on initial mount
- [x] SVG elements display correct initial states (lights on/off, heat on/off, etc.)
- [x] Navigation buttons (back/forward) work correctly
- [x] Navigation buttons are hidden on first/last pages
- [x] Click interactions on assets trigger correct actions
- [x] Unlock dialog shows and functions correctly
- [x] WebSocket updates reflect in real-time
- [x] Temperature and luminosity values display correctly
- [x] Device offline status displays correctly
- [x] No console errors or warnings
- [x] No linter errors

## Files Modified

1. `client/web-vue3/src/pages/MobileWebLayout.vue`
   - Added `v-if="!loading"` to swiper
   - Added `@swiper="onSwiper"` event handler
   - Added `swiperInstance` ref
   - Added `onSwiper()` method
   - Updated `handleNavigation()` to use swiper instance
   - Added `:disable="unlocking"` to unlock button

2. `client/web-vue3/src/composables/useSvgInteraction.js`
   - Removed debug statements
   - Added null check for peripherals
   - Fixed page ID checking to use `currentPageId` parameter

3. `client/web-vue3/src/composables/usePeripheralControl.js`
   - Previously fixed: Corrected authentication import from `@/store/auth.store` to `@/_services`

## Related Documents

- `MOBILE_WEB_LAYOUT_OPTIMIZATION_PLAN.md` - Initial optimization plan
- `MOBILE_WEB_LAYOUT_OPTIMIZATION_SUMMARY.md` - Optimization implementation summary
- `MOBILE_WEB_LAYOUT_FIX.md` - Authentication module fix

## Conclusion

The initialization issues were resolved by:
1. Ensuring peripherals are loaded before SVG rendering
2. Properly handling null/undefined peripheral data
3. Correctly referencing the current page ID for navigation button visibility
4. Providing programmatic control over the swiper instance

The optimized `MobileWebLayout.vue` now correctly implements all the important conventions from the old implementation while maintaining the benefits of the Composition API and composable architecture.

