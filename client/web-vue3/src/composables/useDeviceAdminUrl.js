import { ref, computed, watch } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { APP_CONFIG_GET } from '@/graphql/queries';

/**
 * Resolve a device's controller admin-UI URL on the frontend.
 *
 * <p>Reads the per-model URL pattern from the GIT-backed ConfigProvider
 * (graphql query `config(key: "device.adminurl.pattern.<MODEL_NAME>")`) and
 * substitutes <code>{ip}/{port}/{username}/{password}</code> from the device's
 * own data (networkAddress + default DeviceAccount).</p>
 *
 * <p>Patterns are fetched lazily per model the first time they're needed and
 * memoised in a module-level cache so subsequent devices of the same model
 * reuse the same query result without a network round-trip.</p>
 *
 * @param {Ref|Object} device  Reactive ref or plain object with the device
 *                             (must include model, networkAddress?, authAccounts?).
 *                             Pass a ref so the URL recomputes when the device
 *                             reloads after an IP update / account change.
 * @returns {Object} { adminUrl: ComputedRef<string|null>, refresh: () => void }
 *
 * @example
 *   const { adminUrl } = useDeviceAdminUrl(viewItem);  // viewItem is a ref
 *   // template: <a v-if="adminUrl" :href="adminUrl" target="_blank">{{ adminUrl }}</a>
 */

// Module-level cache: model -> Promise<pattern|null>. Keeps pattern requests
// deduplicated across all useDeviceAdminUrl() instances in the page.
const patternCache = new Map();

export function useDeviceAdminUrl(device) {
  const { client } = useApolloClient();
  const pattern = ref(null);
  let currentModel = null;

  /** Unwrap whatever the caller passed (ref or plain object). */
  const unwrap = () => (device && 'value' in device ? device.value : device);

  /**
   * Fetch the pattern for a given model name. Memoised on `patternCache` so
   * multiple devices of the same model only trigger one network request, and
   * subsequent visits hit the cache.
   */
  const fetchPattern = (modelName) => {
    if (!modelName) return Promise.resolve(null);
    if (patternCache.has(modelName)) {
      return patternCache.get(modelName);
    }
    const key = `device.adminurl.pattern.${modelName}`;
    const promise = client
      .query({
        query: APP_CONFIG_GET,
        variables: { key },
        fetchPolicy: 'cache-first',
      })
      .then((r) => r?.data?.config || null)
      .catch(() => null);
    patternCache.set(modelName, promise);
    return promise;
  };

  /** Re-resolve the pattern for the current device's model. */
  const resolve = () => {
    const d = unwrap();
    const modelName = d?.model || null;
    if (modelName === currentModel) return;
    currentModel = modelName;
    pattern.value = null;
    if (!modelName) return;
    fetchPattern(modelName).then((p) => {
      // Only commit if the model is still current (avoid races on rapid switches).
      if (currentModel === modelName) pattern.value = p;
    });
  };

  // React to model changes (e.g. when DeviceView refetches after IP update).
  if (device && 'value' in device) {
    watch(() => unwrap()?.model, resolve, { immediate: true });
  } else {
    resolve();
  }

  /**
   * Public escape hatch: force a re-fetch (e.g. after the user just edited a
   * pattern in Application Configuration). Clears the cache entry for the
   * current model so the next reactive read pulls fresh.
   */
  const refresh = () => {
    if (currentModel) {
      patternCache.delete(currentModel);
      const m = currentModel;
      currentModel = null;
      resolve();
      // resolve() bails when modelName === currentModel; force re-entry
      if (!pattern.value && m) {
        currentModel = m;
        fetchPattern(m).then((p) => {
          if (currentModel === m) pattern.value = p;
        });
      }
    }
  };

  const adminUrl = computed(() => {
    const d = unwrap();
    const p = pattern.value;
    if (!p) return null;
    const ip = d?.networkAddress?.ip;
    if (!ip) return null;
    const accounts = d?.authAccounts || [];
    const account = accounts.find((a) => a?.isDefault) || accounts[0] || null;
    return p
      .replace('{ip}', ip)
      .replace('{port}', d?.networkAddress?.port || '')
      .replace('{username}', account?.username || '')
      .replace('{password}', account?.password || '');
  });

  return { adminUrl, refresh };
}
