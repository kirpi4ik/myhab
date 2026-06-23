import {useRouter} from 'vue-router';

/**
 * Maps a backend EntityType (see org.myhab.domain.EntityType) to the SPA admin
 * detail route. This is the single place that turns a scanned QR token
 * (entity type + id) into an in-app navigation against the *current* deployment
 * — the base URL is never baked into the QR.
 *
 * Only entity types that have a detail page are listed; others resolve to null.
 */
export const ENTITY_ROUTES = {
  CABLE: id => `/admin/cables/${id}/view`,
  DEVICE: id => `/admin/devices/${id}/view`,
  PERIPHERAL: id => `/admin/peripherals/${id}/view`,
  ZONE: id => `/admin/zones/${id}/view`,
  PORT: id => `/admin/ports/${id}/view`,
  SCENARIO: id => `/admin/scenarios/${id}/view`,
  JOB: id => `/admin/jobs/${id}/view`,
  USER: id => `/admin/users/${id}/view`,
};

/**
 * Parse a scanned QR payload of the form `myhab://{ENTITY_TYPE}/{id}` (any
 * trailing text after the id is ignored). Returns {entityType, id} or null.
 */
export function parseEntityToken(raw) {
  if (!raw) return null;
  const match = /^myhab:\/\/([A-Za-z_]+)\/(\d+)/.exec(raw.trim());
  if (!match) return null;
  return {entityType: match[1].toUpperCase(), id: match[2]};
}

export function useEntityRouter() {
  const router = useRouter();

  /**
   * Build the SPA path for an entity, or null if the type is not routable.
   */
  const entityPath = (entityType, id) => {
    const builder = ENTITY_ROUTES[(entityType || '').toUpperCase()];
    return builder ? builder(id) : null;
  };

  /**
   * Navigate to an entity's detail page. Returns true on success, false if the
   * entity type has no known route.
   */
  const navigateToEntity = (entityType, id) => {
    const path = entityPath(entityType, id);
    if (!path) return false;
    router.push(path);
    return true;
  };

  return {entityPath, navigateToEntity, parseEntityToken};
}
