import { ref, computed, watch } from 'vue';
import { useApolloClient } from '@vue/apollo-composable';
import { APP_CONFIG_GET } from '@/graphql/queries';

/**
 * Resolve a device's controller admin-UI URL on the frontend.
 *
 * <p>Reads the per-model URL pattern from the GIT-backed ConfigProvider
 * (graphql query `config(key: "device.adminurl.pattern.<MODEL_NAME>")`) and
 * substitutes <code>{ip}/{port}/{username}/{password}</code> from the device's
 * own data (networkAddress + default DeviceAccount). Without a configured
 * pattern, a built-in default for the model is used.</p>
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

// Module-level cache: config key -> Promise<pattern|null>. Keeps pattern
// requests deduplicated across all instances in the page.
const patternCache = new Map();

// Built-in patterns used when the config repo has none for the model;
// `{:port}` expands to `:<port>` unless the port is empty or 80.
const DEFAULT_DEVICE_PATTERNS = {
  MEGAD_2561_RTC: 'http://{ip}/{password}/',
};
const DEFAULT_PORT_PATTERNS = {
  MEGAD_2561_RTC: 'http://{ip}/{password}/?pt={portRef}',
};
const GENERIC_DEVICE_PATTERN = 'http://{ip}{:port}/';

// MegaD factory password, also assumed by MegaDriverService.
const DEFAULT_PASSWORDS = {
  MEGAD_2561_RTC: 'sec',
};

/** Unwrap whatever the caller passed (ref or plain object). */
const unwrapRef = (r) => (r && 'value' in r ? r.value : r);

function substitute(pattern, device, extra = {}) {
  const ip = device?.networkAddress?.ip;
  if (!pattern || !ip) return null;
  const port = device?.networkAddress?.port;
  const accounts = device?.authAccounts || [];
  const account = accounts.find((a) => a?.isDefault) || accounts[0] || null;
  const password = account?.password || DEFAULT_PASSWORDS[device?.model] || '';
  let url = pattern
    .replace('{ip}', ip)
    .replace('{:port}', port && String(port) !== '80' ? `:${port}` : '')
    .replace('{port}', port || '')
    .replace('{username}', account?.username || '')
    .replace('{password}', password);
  Object.entries(extra).forEach(([k, v]) => {
    url = url.replace(`{${k}}`, v ?? '');
  });
  return url;
}

/**
 * Shared machinery: watch the device's model, fetch the configured pattern
 * for `<keyPrefix>.<model>`, fall back to `defaults[model]` (or `generic`).
 */
function useModelPattern(getDevice, keyPrefix, defaults, generic) {
  const { client } = useApolloClient();
  const pattern = ref(null);
  let currentModel = null;

  const fetchPattern = (modelName) => {
    const key = `${keyPrefix}.${modelName}`;
    if (patternCache.has(key)) {
      return patternCache.get(key);
    }
    const promise = client
      .query({
        query: APP_CONFIG_GET,
        variables: { key },
        fetchPolicy: 'cache-first',
      })
      .then((r) => r?.data?.config || null)
      .catch(() => null);
    patternCache.set(key, promise);
    return promise;
  };

  const load = (modelName) => {
    fetchPattern(modelName).then((p) => {
      // Only commit if the model is still current (avoid races on rapid switches).
      if (currentModel === modelName) pattern.value = p || defaults[modelName] || generic;
    });
  };

  const resolve = () => {
    const modelName = getDevice()?.model || null;
    if (modelName === currentModel) return;
    currentModel = modelName;
    pattern.value = null;
    if (modelName) load(modelName);
  };

  watch(() => getDevice()?.model, resolve, { immediate: true });

  /**
   * Force a re-fetch (e.g. after the user just edited a pattern in
   * Application Configuration).
   */
  const refresh = () => {
    if (!currentModel) return;
    patternCache.delete(`${keyPrefix}.${currentModel}`);
    load(currentModel);
  };

  return { pattern, refresh };
}

export function useDeviceAdminUrl(device) {
  const getDevice = () => unwrapRef(device);
  const { pattern, refresh } = useModelPattern(
    getDevice, 'device.adminurl.pattern', DEFAULT_DEVICE_PATTERNS, GENERIC_DEVICE_PATTERN,
  );
  const adminUrl = computed(() => substitute(pattern.value, getDevice()));
  return { adminUrl, refresh };
}

/**
 * Controller admin-UI URL for a single port (`device.adminurl.port.pattern.<MODEL>`,
 * extra placeholder <code>{portRef}</code> = port internalRef). Null for models
 * with neither a configured nor a built-in port pattern.
 *
 * @param {Ref|Object} port  Port with internalRef and device { model, networkAddress, authAccounts }.
 */
export function useDevicePortAdminUrl(port) {
  const getDevice = () => unwrapRef(port)?.device;
  const { pattern, refresh } = useModelPattern(
    getDevice, 'device.adminurl.port.pattern', DEFAULT_PORT_PATTERNS, null,
  );
  const adminUrl = computed(() => {
    const p = unwrapRef(port);
    if (!p?.internalRef) return null;
    return substitute(pattern.value, p.device, { portRef: encodeURIComponent(p.internalRef) });
  });
  return { adminUrl, refresh };
}
