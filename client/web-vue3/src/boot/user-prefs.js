import { boot } from 'quasar/wrappers';
import { authzService } from '@/_services';
import { useUserPrefsStore } from 'src/store/user-prefs.store';

/**
 * Hydrate the per-user preferences store whenever the authenticated user
 * changes. Runs after `graphql` boot, subscribes to the authzService
 * observable, and:
 *   - on login → fetch the user's Configuration rows and populate the store
 *   - on logout → clear the store so a different user starts fresh
 *
 * The initial hydrate also covers the "refresh while logged in" case:
 * `currentUserValue` is populated synchronously from localStorage at module
 * load, so the subscribe callback fires immediately with the existing user.
 */
export default boot(async ({ store }) => {
	const prefs = useUserPrefsStore(store);

	const hydrateFor = async (user) => {
		if (!user || user.id == null) {
			// User id might not be populated yet right after login — try to fill it
			// via the `me` GraphQL query. If still missing, treat as logged-out.
			const refreshed = user ? await authzService.ensureCurrentUserIds() : null;
			if (!refreshed || refreshed.id == null) {
				prefs.clear();
				return;
			}
			user = refreshed;
		}
		// Avoid redundant refetch if the same user is being announced again
		// (BehaviorSubject fires on subscribe with the current value).
		if (prefs.userId === Number(user.id) && prefs.loaded) {
			return;
		}
		await prefs.hydrate(user.id);
	};

	authzService.currentUser.subscribe((user) => {
		// Fire-and-forget — boot should not block the app on this. The first
		// hydrate is best-effort; widgets render with "all visible" defaults
		// until it completes.
		hydrateFor(user).catch((err) => {
			// eslint-disable-next-line no-console
			console.warn('[user-prefs] hydrate-on-auth-change failed', err);
		});
	});
});
