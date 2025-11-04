# MobileWebLayout Optimization - Auth Store Fix

**Date:** November 3, 2025  
**Issue:** Module not found error for `@/store/auth.store`  
**Status:** ✅ Fixed

---

## Problem

```
ERROR in ./src/composables/usePeripheralControl.js 7:0-50
Module not found: Error: Can't resolve '@/store/auth.store'
```

---

## Root Cause

The project uses a **legacy authentication service** (`authzService`) instead of a Pinia store for authentication.

**Current Auth Implementation:**
- Located at: `client/web-vue3/src/_services/authentication.service.js`
- Uses RxJS `BehaviorSubject` for state management
- Exports `authzService` with `currentUserValue` property

---

## Solution

### Changed in `usePeripheralControl.js`:

**Before:**
```javascript
import { useAuthStore } from '@/store/auth.store';

export function usePeripheralControl() {
  const authStore = useAuthStore();
  
  // Usage:
  p6: authStore.currentUser?.login || 'unknown',
}
```

**After:**
```javascript
import { authzService } from '@/_services';

export function usePeripheralControl() {
  // No need to instantiate, it's a singleton
  
  // Usage:
  p6: authzService.currentUserValue?.login || 'unknown',
}
```

---

## Changes Made

### File: `client/web-vue3/src/composables/usePeripheralControl.js`

1. **Import changed:**
   ```javascript
   - import { useAuthStore } from '@/store/auth.store';
   + import { authzService } from '@/_services';
   ```

2. **Removed unnecessary instantiation:**
   ```javascript
   - const authStore = useAuthStore();
   ```

3. **Updated all references (3 occurrences):**
   ```javascript
   - p6: authStore.currentUser?.login || 'unknown',
   + p6: authzService.currentUserValue?.login || 'unknown',
   ```

---

## Verification

✅ Import path corrected  
✅ All references updated  
✅ No more module not found error  
✅ Compatible with existing auth implementation  

---

## Note on Auth Implementation

The project currently uses a **legacy authentication pattern**:

```javascript
// client/web-vue3/src/_services/authentication.service.js
import { BehaviorSubject } from 'rxjs';

const currentUserSubject = new BehaviorSubject(
  JSON.parse(localStorage.getItem('currentUser'))
);

export const authzService = {
  login,
  logout,
  currentUser: currentUserSubject.asObservable(),
  get currentUserValue() {
    return currentUserSubject.value;
  },
  hasAnyRole,
};
```

**Future Enhancement (Optional):**
Consider migrating to a Pinia store for consistency:

```javascript
// client/web-vue3/src/store/auth.store.js
import { defineStore } from 'pinia';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    currentUser: JSON.parse(localStorage.getItem('currentUser'))
  }),
  getters: {
    isAuthenticated: (state) => !!state.currentUser,
    userLogin: (state) => state.currentUser?.login
  },
  actions: {
    async login(username, password) {
      // ... login logic
    },
    logout() {
      // ... logout logic
    }
  }
});
```

But this is **not required** for the current optimization to work.

---

## Status

✅ **Fixed and Ready**

The `usePeripheralControl.js` composable now correctly uses the existing `authzService` for authentication, matching the pattern used in the original `MobileWebLayout.vue` component.

---

**Fixed By:** AI Assistant (Claude Sonnet 4.5)  
**Date:** November 3, 2025  
**Status:** ✅ Complete

