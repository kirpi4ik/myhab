import { defineStore } from 'pinia';
import { apolloClient } from 'boot/graphql';
import {
	CONFIGURATION_LIST,
	CONFIGURATION_SET_VALUE,
	CONFIGURATION_REMOVE_CONFIG_BY_KEY,
} from 'src/graphql/queries/configurations';

/**
 * Per-user dashboard preferences. Storage piggybacks on the existing
 * `Configuration` table (entityType=USER, entityId=<userId>), so no new
 * domain or schema is required — the same `savePropertyValue` /
 * `configurationListByEntity` GraphQL operations already used by the
 * ConfigurationView UI cover read & write.
 *
 * Today this stores only `ui.dashboard.widgets.hidden` (a comma-separated
 * list of widget IDs the user has hidden), but the shape is generic enough
 * for future per-user UI preferences without further refactoring.
 */

const KEY_DASHBOARD_HIDDEN = 'ui.dashboard.widgets.hidden';

function parseCsv(value) {
	if (value == null || value === '') return [];
	return String(value)
		.split(',')
		.map((s) => s.trim())
		.filter((s) => s.length > 0);
}

export const useUserPrefsStore = defineStore('userPrefs', {
	state: () => ({
		/** The user id this snapshot belongs to. Used to detect stale data on user change. */
		userId: null,
		/** Set<widgetId> the user has hidden on the dashboard. */
		hiddenWidgets: new Set(),
		/** Flips true after the first hydrate (success or empty). */
		loaded: false,
	}),
	getters: {
		isWidgetVisible: (state) => (widgetId) => !state.hiddenWidgets.has(widgetId),
	},
	actions: {
		/** Drop everything — call on logout. */
		clear() {
			this.userId = null;
			this.hiddenWidgets = new Set();
			this.loaded = false;
		},
		/** Fetch the user's Configuration rows and populate the in-memory state. */
		async hydrate(userId) {
			if (userId == null) {
				this.clear();
				return;
			}
			this.userId = Number(userId);
			try {
				const { data } = await apolloClient.query({
					query: CONFIGURATION_LIST,
					variables: { entityType: 'USER', entityId: this.userId },
					fetchPolicy: 'network-only',
				});
				const rows = data?.configurationListByEntity || [];
				const hiddenRow = rows.find((r) => r.key === KEY_DASHBOARD_HIDDEN);
				this.hiddenWidgets = new Set(parseCsv(hiddenRow?.value));
			} catch (err) {
				// eslint-disable-next-line no-console
				console.warn('[user-prefs] hydrate failed; defaulting to all widgets visible', err);
				this.hiddenWidgets = new Set();
			} finally {
				this.loaded = true;
			}
		},
		/**
		 * Toggle a widget's visibility and persist via the existing
		 * savePropertyValue mutation. Optimistic — updates local state first,
		 * rolls back on persistence failure.
		 */
		async setWidgetVisible(widgetId, visible) {
			if (this.userId == null) return;
			const prev = new Set(this.hiddenWidgets);
			const next = new Set(this.hiddenWidgets);
			if (visible) next.delete(widgetId);
			else next.add(widgetId);
			this.hiddenWidgets = next;
			try {
				const csv = Array.from(next).join(',');
				if (csv === '') {
					// Empty list → delete the row so we don't leave noise rows in the DB.
					await apolloClient.mutate({
						mutation: CONFIGURATION_REMOVE_CONFIG_BY_KEY,
						variables: {
							entityType: 'USER',
							entityId: this.userId,
							key: KEY_DASHBOARD_HIDDEN,
						},
					});
				} else {
					await apolloClient.mutate({
						mutation: CONFIGURATION_SET_VALUE,
						variables: {
							entityType: 'USER',
							entityId: this.userId,
							key: KEY_DASHBOARD_HIDDEN,
							value: csv,
						},
					});
				}
			} catch (err) {
				// eslint-disable-next-line no-console
				console.error('[user-prefs] failed to persist widget visibility; rolling back', err);
				this.hiddenWidgets = prev;
				throw err;
			}
		},
	},
});
