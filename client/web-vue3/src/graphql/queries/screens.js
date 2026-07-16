import { gql } from '@apollo/client/core';

/**
 * Dashboard screens (mobile /wui): metadata + layout JSON. Background image
 * bytes are served via REST (/api/screens/:id/background), not GraphQL.
 */

const SCREEN_FIELDS = `
	id
	name
	ordinal
	enabled
	layoutJson
	backgroundContentType
	backgroundWidth
	backgroundHeight
	tsUpdated
`;

export const DASHBOARD_SCREENS = gql`
	query dashboardScreens($enabledOnly: Boolean) {
		dashboardScreens(enabledOnly: $enabledOnly) {
			${SCREEN_FIELDS}
		}
	}
`;

export const SCREEN_CREATE = gql`
	mutation dashboardScreenCreate($dashboardScreen: DashboardScreenCreate!) {
		dashboardScreenCreate(dashboardScreen: $dashboardScreen) {
			${SCREEN_FIELDS}
		}
	}
`;

export const SCREEN_UPDATE = gql`
	mutation dashboardScreenUpdate($id: Long!, $dashboardScreen: DashboardScreenUpdate!) {
		dashboardScreenUpdate(id: $id, dashboardScreen: $dashboardScreen) {
			${SCREEN_FIELDS}
		}
	}
`;

export const SCREEN_DELETE = gql`
	mutation dashboardScreenDelete($id: Long!) {
		dashboardScreenDelete(id: $id) {
			success
			error
		}
	}
`;

export const SCREEN_SAVE_LAYOUT = gql`
	mutation dashboardScreenSaveLayout($id: Long!, $layoutJson: String!) {
		dashboardScreenSaveLayout(id: $id, layoutJson: $layoutJson) {
			${SCREEN_FIELDS}
		}
	}
`;

export const SCREEN_REORDER = gql`
	mutation dashboardScreenReorder($ids: [Long!]) {
		dashboardScreenReorder(ids: $ids) {
			id
			ordinal
		}
	}
`;
