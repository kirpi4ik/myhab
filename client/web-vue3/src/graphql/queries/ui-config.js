import { gql } from '@apollo/client/core';

/**
 * Boot-time fetch of UI-facing app configuration. Returns the `ui.*` subset
 * of the git-backed ConfigProvider — device IDs, zone IDs, Grafana URLs,
 * UI date formats, etc. Excludes the secret-bearing keys (mqtt.password,
 * telegram.token, …) that `appConfigList` would expose, so regular
 * authenticated users can hydrate the dashboard without seeing credentials.
 *
 * Edits go through the existing `appConfigUpdate` mutation (admin only,
 * commits to the same git repo). The backend then publishes
 * `evt_app_cfg_value_changed` over WebSocket so connected SPAs refresh
 * their in-memory copy live (see App.vue).
 */
export const UI_CONFIG_LIST = gql`
	query uiConfigList {
		uiConfigList {
			key
			value
		}
	}
`;
