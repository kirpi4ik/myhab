# MobileWebLayout.vue Optimization Plan

**Date:** November 3, 2025  
**Component:** `MobileWebLayout.vue`  
**Current State:** Options API, legacy patterns  
**Target:** Composition API, modern patterns, better performance

---

## Current Issues Identified

### 1. **Options API (Legacy)**
- Uses Options API instead of modern Composition API
- Harder to maintain and test
- Less type-safe
- Not consistent with other migrated components

### 2. **Direct Apollo Client Import**
- Uses `apolloClient` directly instead of `useApolloClient()`
- Not following Vue 3 best practices
- Harder to mock in tests

### 3. **Legacy Service Imports**
- Uses `@/_services` path (old structure)
- `authzService.currentUserValue` - should use Pinia store
- `lightService`, `heatService` - should be composables

### 4. **Manual Event Listeners**
- Uses `document.addEventListener` in mounted
- Should use Vue event handling or composables
- No cleanup in unmounted

### 5. **Complex State Management**
- Multiple reactive data properties
- Complex nested objects (`srvPeripherals`, `portToPeripheralMap`, `assetMap`)
- Could benefit from computed properties and better organization

### 6. **WebSocket Store Usage**
- Uses old store pattern
- Should use composable pattern

### 7. **Hardcoded Text**
- Romanian text hardcoded ("Atentie! Doriti sa deschideti ?")
- Should use i18n

### 8. **No Error Handling**
- GraphQL queries have no error handling
- No loading states
- No user feedback on errors

### 9. **Performance Issues**
- No debouncing on click handlers
- SVG manipulation could be optimized
- No memoization of computed values

### 10. **Code Organization**
- Large methods (`init`, `transform`, `svgElInit`)
- Could be split into smaller, testable functions
- Mixed concerns (UI, data fetching, business logic)

---

## Optimization Strategy

### Phase 1: Convert to Composition API ✅
- Migrate from Options API to Composition API
- Use `<script setup>` syntax
- Extract logic into composables

### Phase 2: Modernize Dependencies ✅
- Replace direct Apollo import with `useApolloClient()`
- Replace service imports with composables
- Use Pinia store properly

### Phase 3: Improve State Management ✅
- Use `ref` and `computed` properly
- Extract complex logic into composables
- Better reactive data organization

### Phase 4: Add Error Handling ✅
- Add try-catch blocks
- Show user notifications
- Add loading states

### Phase 5: Performance Optimization ✅
- Add debouncing where needed
- Memoize expensive computations
- Optimize SVG manipulation

### Phase 6: Code Organization ✅
- Extract methods into separate composables
- Split large functions
- Better separation of concerns

---

## Proposed Structure

### New Composables to Create:

1. **`useSvgInteraction.js`**
   - Handle SVG click events
   - Parse asset IDs
   - Manage SVG transformations

2. **`usePeripheralState.js`**
   - Manage peripheral state
   - Handle port-to-peripheral mapping
   - Update UI based on WebSocket events

3. **`usePeripheralControl.js`**
   - Light control
   - Heat control
   - Door lock control

### Component Structure:
```vue
<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { useRouter } from 'vue-router';
import { useWebSocketStore } from '@/store/websocket.store';
import { useNotifications } from '@/composables';
import { useSvgInteraction } from '@/composables/useSvgInteraction';
import { usePeripheralState } from '@/composables/usePeripheralState';
import { usePeripheralControl } from '@/composables/usePeripheralControl';

// Component logic here
</script>
```

---

## Implementation Plan

### Step 1: Create Composables
- [ ] Create `useSvgInteraction.js`
- [ ] Create `usePeripheralState.js`
- [ ] Create `usePeripheralControl.js`

### Step 2: Migrate Component
- [ ] Convert to `<script setup>`
- [ ] Replace Options API with Composition API
- [ ] Use new composables

### Step 3: Modernize Dependencies
- [ ] Replace direct Apollo import
- [ ] Replace service imports
- [ ] Use proper store access

### Step 4: Add Features
- [ ] Error handling
- [ ] Loading states
- [ ] User notifications
- [ ] i18n support

### Step 5: Optimize Performance
- [ ] Add debouncing
- [ ] Memoize computations
- [ ] Optimize SVG manipulation

### Step 6: Testing
- [ ] Add unit tests
- [ ] Test WebSocket integration
- [ ] Test SVG interactions

---

## Estimated Impact

### Code Quality
- **Before:** Options API, mixed concerns, hard to test
- **After:** Composition API, separated concerns, testable

### Maintainability
- **Before:** ~400 lines in one file, complex logic
- **After:** ~200 lines + 3 composables, clear separation

### Performance
- **Before:** No optimization, potential memory leaks
- **After:** Debounced events, proper cleanup, optimized

### Type Safety
- **Before:** No types, hard to catch errors
- **After:** Better types with Composition API

---

## Risk Assessment

### Low Risk
- ✅ Composable extraction
- ✅ Composition API migration
- ✅ Error handling addition

### Medium Risk
- ⚠️ WebSocket integration changes
- ⚠️ SVG manipulation changes
- ⚠️ State management refactoring

### High Risk
- ⚠️ Breaking existing SVG interactions
- ⚠️ Changing peripheral control logic
- ⚠️ WebSocket message handling

---

## Recommendation

**Approach:** Incremental migration with thorough testing

1. **Start with composables** - Extract logic without changing component
2. **Test thoroughly** - Ensure SVG interactions still work
3. **Migrate gradually** - Convert to Composition API piece by piece
4. **Add features** - Error handling, loading states, etc.
5. **Optimize** - Performance improvements last

**Timeline:** 2-3 hours for full optimization

**Priority:** Medium - Component works but needs modernization

---

## Next Steps

1. Create the three composables
2. Test composables independently
3. Migrate component to use composables
4. Add error handling and loading states
5. Optimize performance
6. Add tests

---

**Analysis By:** AI Assistant (Claude Sonnet 4.5)  
**Date:** November 3, 2025  
**Status:** Ready for implementation

