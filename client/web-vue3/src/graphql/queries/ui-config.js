import { gql } from '@apollo/client/core';

/**
 * Boot-time fetch of UI-facing app configuration. Returns every Configuration
 * row whose entityType is CONFIG, entityId is 0, and key begins with `ui.`.
 * The result hydrates the `useAppConfigStore` Pinia store; widgets then read
 * device IDs, zone IDs etc. synchronously from the store.
 */
export const UI_CONFIG_LIST = gql`
	query uiConfigList {
		uiConfigList {
			key
			value
		}
	}
`;
