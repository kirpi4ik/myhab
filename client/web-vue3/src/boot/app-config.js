import { boot } from 'quasar/wrappers';
import { apolloClient } from 'boot/graphql';
import { authzService } from '@/_services';
import { UI_CONFIG_LIST } from 'src/graphql/queries/ui-config';
import { useAppConfigStore } from 'src/store/app-config.store';

/**
 * Hydrate the app-config Pinia store from the git-backed ConfigProvider.
 *
 * The query requires a valid JWT (the GraphQL endpoint is `isAuthenticated()`),
 * so we can't just fire it once at boot — on a fresh browser the user lands
 * on /login with no token, the query 401s, the store stays empty, and the
 * dashboard widgets render with null deviceIds. To handle that, we subscribe
 * to `authzService.currentUser` and hydrate (or re-hydrate) every time the
 * user transitions to logged-in. On logout we leave the previous values in
 * place — they're not user-specific and the cached map keeps widgets working
 * on the brief redirect to /login before unmount.
 *
 * Source of truth is `uiConfigList` — a UI-safe slice of ConfigProvider
 * (`specialDevices.*`, `specialZones.*`, `grafana.*`, `surveillance.*`,
 * `ui.*`). Components read synchronously after hydrate via
 * `appConfig.getNumber('specialDevices.navimow.deviceId')` etc. Live edits
 * from the /admin/appconfig UI propagate via the WebSocket listener in
 * App.vue (event: `evt_app_cfg_value_changed`).
 */
async function hydrate(appConfig) {
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
}

export default boot(({ store }) => {
	const appConfig = useAppConfigStore(store);

	// BehaviorSubject fires on subscribe with the current value, so the
	// initial hydrate happens automatically — no manual call needed.
	let lastUserId = null;
	authzService.currentUser.subscribe((user) => {
		// Treat "no user" as logged-out. Don't clear the store on logout;
		// re-hydrate only when a user logs IN or switches accounts.
		if (!user) {
			lastUserId = null;
			return;
		}
		// Avoid redundant refetches if the same user is re-announced
		// (BehaviorSubject also fires on subscribe).
		const currentId = user.id != null ? Number(user.id) : 'anon';
		if (currentId === lastUserId && appConfig.loaded) {
			return;
		}
		lastUserId = currentId;
		hydrate(appConfig).catch(() => { /* logged in hydrate() */ });
	});
});
