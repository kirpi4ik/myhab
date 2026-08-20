import { useAppConfigStore } from 'src/store/app-config.store';

/**
 * Answers "can this widget work?" from the `requiredConfig` it declares in
 * useDashboardWidgets.
 *
 * Shared so the dashboard, the settings screen and the placeholder card all agree —
 * a widget that Settings lets you enable must be one the dashboard can actually render.
 *
 * Reads the app-config store, so it is reactive: setting a key via the picker (or an
 * admin editing it elsewhere, which App.vue pushes into the store over the WebSocket)
 * flips a widget from unconfigured to configured without a reload.
 */
export function useWidgetConfigStatus() {
	const appConfig = useAppConfigStore();

	/** The declared settings this widget is missing. Empty means it is good to go. */
	const missingConfig = (widget) => {
		const required = widget?.requiredConfig || [];
		return required.filter((entry) => {
			const value = appConfig.get(entry.key);
			return value == null || value === '';
		});
	};

	const isConfigured = (widget) => missingConfig(widget).length === 0;

	return { missingConfig, isConfigured };
}
