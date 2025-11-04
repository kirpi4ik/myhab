# MobileWebLayout CSS Scoped Fix

## Issue

SVG elements had the correct CSS class assigned (e.g., `bulb-on`), but the styles were not visible in the browser. Elements that should be yellow (lights ON) were not displaying the correct color.

## Root Cause

### Vue Scoped Styles

The new implementation used `<style scoped>`:

```vue
<style scoped>
.bulb-on {
  fill: #d6d40f;
  fill-opacity: 0.7;
}
</style>
```

When Vue processes scoped styles, it:
1. Adds a unique data attribute to all elements in the component's template (e.g., `data-v-7ba5bd90`)
2. Transforms CSS selectors to include this attribute (e.g., `.bulb-on[data-v-7ba5bd90]`)

### The Problem with Dynamic SVG Elements

The SVG elements are **dynamically created and modified** by the `transformSvg` function in `useSvgInteraction.js`:

```javascript
const initializeSvgElement = (svg, element, peripherals, svgPages, currentPageId) => {
  // ...
  element.setAttribute('class', cssClass);  // Sets class like 'bulb-on'
  // ...
};
```

These dynamically modified SVG elements:
- Are loaded from external SVG files (`screen-1.svg`, `screen-2.svg`)
- Don't have the Vue scoped data attribute
- Won't match the scoped CSS selectors
- Therefore, the styles don't apply!

### Comparison with Old Implementation

The old implementation correctly used **unscoped styles**:

```vue
<!-- MobileWebLayout_OLD.vue line 412 -->
<style>
.bulb-on {
  fill: #d6d40f;
  fill-opacity: 0.7;
}
</style>
```

## Solution

Remove the `scoped` attribute from the `<style>` tag:

```vue
<!-- Before -->
<style scoped>
.hidden {
  display: none;
}
/* ... */
</style>

<!-- After -->
<style>
.hidden {
  display: none;
}
/* ... */
</style>
```

**File Modified**: `client/web-vue3/src/pages/MobileWebLayout.vue` (line 279)

## Why This Is Safe

### 1. Specific Selectors

The CSS uses specific selectors that are unlikely to conflict:
- `.bulb-on`, `.bulb-off` - Specific to this component's SVG assets
- `.heat-on`, `.heat-off` - Specific to this component's SVG assets
- `.motion-on`, `.motion-off` - Specific to this component's SVG assets
- `circle.heat-on`, `path.heat-on` - Element-specific selectors
- `circle#nav-home-1` - ID selector for specific element

### 2. Component Isolation

The `MobileWebLayout` component is used in a specific route and doesn't share the page with other components that might have conflicting styles.

### 3. Naming Convention

The class names follow a clear naming convention that indicates they're for asset states:
- `bulb-*` for lights
- `heat-*` for heating
- `motion-*` for motion sensors
- `lock` for door locks
- `device-offline` for offline devices

### 4. SVG-Specific Styles

Most styles use SVG-specific properties (`fill`, `stroke`, `fill-opacity`, `stroke-width`) that won't affect regular HTML elements.

## Verification

After the fix, the browser should correctly display:

### Light ON (bulb-on)
```css
.bulb-on {
  fill: #d6d40f;        /* Yellow color */
  fill-opacity: 0.7;
}
```
**Expected**: Yellow/bright appearance

### Light OFF (bulb-off)
```css
.bulb-off {
  fill: #4a90d6;        /* Blue color */
  fill-opacity: 0.3;
  stroke: #2b6095;
  stroke-width: 0.5;
}
```
**Expected**: Blue/dim appearance

### Heat ON (heat-on)
```css
circle.heat-on {
  fill: rgb(244, 194, 168);  /* Warm orange/pink */
  fill-opacity: 0.61;
  stroke: rgb(116, 29, 29);
}

path.heat-on {
  fill: rgb(88, 77, 77);
  stroke: rgb(226, 9, 9);    /* Red stroke */
  stroke-opacity: 0.34;
}
```
**Expected**: Warm/red appearance

### Heat OFF (heat-off)
```css
circle.heat-off {
  fill: rgb(168, 193, 244);  /* Cool blue */
  fill-opacity: 0.61;
  stroke: rgb(116, 29, 29);
}

path.heat-off {
  fill: rgb(88, 77, 77);
  stroke: rgb(9, 96, 226);   /* Blue stroke */
  stroke-opacity: 0.34;
}
```
**Expected**: Cool/blue appearance

## Testing Checklist

- [ ] Open the application in browser
- [ ] Navigate to mobile web layout
- [ ] Verify lights with state ON show yellow color
- [ ] Verify lights with state OFF show blue color
- [ ] Verify heat elements with state ON show warm/red color
- [ ] Verify heat elements with state OFF show cool/blue color
- [ ] Click on a light to toggle it
- [ ] Verify the color changes immediately (optimistic update)
- [ ] Verify WebSocket updates also change colors correctly
- [ ] Check browser DevTools to confirm styles are applied

## Browser DevTools Verification

### Before Fix (Scoped)
```html
<!-- Element in DOM -->
<circle class="bulb-on" ... />

<!-- Computed styles show NO matching rules -->
Styles: (no matching rules)
```

### After Fix (Unscoped)
```html
<!-- Element in DOM -->
<circle class="bulb-on" ... />

<!-- Computed styles show matching rule -->
Styles:
  .bulb-on {
    fill: #d6d40f;
    fill-opacity: 0.7;
  }
```

## Related Issues

This is a common issue when working with:
- Dynamically created elements
- Elements loaded from external sources
- SVG elements manipulated via JavaScript
- Third-party libraries that inject DOM elements

## Best Practices

When working with Vue scoped styles:

1. **Use scoped styles for template elements** - Elements defined in the component's template
2. **Use unscoped styles for dynamic elements** - Elements created/modified by JavaScript
3. **Use deep selectors for child components** - `:deep(.class)` or `::v-deep .class`
4. **Use global styles for truly global needs** - In a separate global stylesheet

## Conclusion

Removing the `scoped` attribute allows the CSS styles to apply to the dynamically modified SVG elements, fixing the visual appearance of assets in the mobile web layout. The styles remain safe due to specific selectors and component isolation.

