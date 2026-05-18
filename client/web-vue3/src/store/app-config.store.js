import { defineStore } from 'pinia';

/**
 * App-level UI configuration store. Hydrated once at boot from the backend
 * `uiConfigList` GraphQL query (see boot/app-config.js) and read synchronously
 * by widgets that used to consume `process.env.X` values like device IDs and
 * zone IDs. Values are persisted in the `Configuration` table on the backend
 * (entityType=CONFIG, entityId=0, key starting with `ui.`), so changing them
 * via the ConfigurationView UI no longer requires a frontend rebuild.
 */
export const useAppConfigStore = defineStore('appConfig', {
	state: () => ({
		/** key → value map. All values are strings (or null). */
		entries: {},
		/** flips true after the boot-time fetch completes (even if it returned []). */
		loaded: false,
	}),
	getters: {
		/** Raw string value. Returns the fallback when the key is missing. */
		get: (state) => (key, fallback = null) => {
			const v = state.entries[key];
			return v != null ? v : fallback;
		},
		/** Number-coerced value. Returns null/fallback for missing or unparseable values. */
		getNumber: (state) => (key, fallback = null) => {
			const v = state.entries[key];
			if (v == null || v === '') return fallback;
			const n = Number(v);
			return Number.isFinite(n) ? n : fallback;
		},
	},
	actions: {
		/** Bulk-replace from a list of {key, value} rows returned by uiConfigList. */
		setAll(rows) {
			const next = {};
			(rows || []).forEach((r) => {
				if (r && r.key) next[r.key] = r.value;
			});
			this.entries = next;
			this.loaded = true;
		},
		/** Optimistic single-key update — used after a successful save in the settings UI. */
		updateOne(key, value) {
			this.entries = { ...this.entries, [key]: value };
		},
	},
});
