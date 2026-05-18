import { boot } from 'quasar/wrappers';
import { apolloClient } from 'boot/graphql';
import { UI_CONFIG_LIST } from 'src/graphql/queries/ui-config';
import { useAppConfigStore } from 'src/store/app-config.store';

/**
 * Hydrate the app-config Pinia store before the router mounts the first page.
 * Components that previously read `process.env.NAVIMOW_DEVICE_ID` (etc.) now
 * call `appConfig.getNumber('ui.device.navimow.id')` and can do so
 * synchronously because this boot file blocks on the network round trip.
 *
 * Failure modes:
 *   - Network/backend down → log + leave the store empty; components fall
 *     back to their default values (null) and render an empty state. The
 *     app still loads; widgets that need an ID will just show a placeholder.
 *   - Unauthenticated → the query is exposed without auth restrictions, but
 *     if the request fails with 401 we still don't block startup.
 */
export default boot(async ({ store }) => {
	const appConfig = useAppConfigStore(store);
	try {
		const { data } = await apolloClient.query({
			query: UI_CONFIG_LIST,
			fetchPolicy: 'network-only',
		});
		appConfig.setAll(data?.uiConfigList || []);
	} catch (err) {
		// eslint-disable-next-line no-console
		console.warn('[app-config] uiConfigList fetch failed; widgets will use null defaults', err);
		appConfig.setAll([]);
	}
});
