import {BehaviorSubject} from 'rxjs';

import { handleResponse, requestOptions, Utils } from '@/_helpers';
import { ME } from '@/graphql/queries';

// `apolloClient` is loaded lazily (dynamic import inside ensureCurrentUserIds) to avoid
// a module-evaluation cycle: boot/graphql.js → @/_services → authentication.service.js → boot/graphql.js.
// At call time (post-login / on route change) all modules have fully evaluated.

/**
 * Safe JSON parse with fallback
 * @param {string} value - JSON string to parse
 * @returns {Object|null} Parsed object or null
 */
const safeJsonParse = (value) => {
	try {
		return value ? JSON.parse(value) : null;
	} catch (error) {
		console.error('Error parsing stored user data:', error);
		return null;
	}
};

const currentUserSubject = new BehaviorSubject(safeJsonParse(localStorage.getItem('currentUser')));

/**
 * Fetch current user id/username via the `me` GraphQL query and merge into currentUser.
 * Call after login or on app load so HeaderLayout (and avatar) can use currentUser.id.
 */
function ensureCurrentUserIds() {
	const current = currentUserSubject.value;
	const token = current?.access_token || current?.token;
	if (!token) {
		return Promise.resolve(current);
	}
	// Refetch when ids are missing, or when `language` / `timezone` were never
	// loaded (undefined). A user who explicitly has no preference stores `null`,
	// which is "loaded" and does not trigger another fetch.
	const needsFetch = current?.id == null || current?.username == null || current?.language === undefined || current?.timezone === undefined;
	if (!needsFetch) {
		return Promise.resolve(current);
	}
	return import('@/boot/graphql')
		.then(({ apolloClient }) => apolloClient.query({ query: ME, fetchPolicy: 'no-cache' }))
		.then((response) => response?.data?.me || null)
		.then((data) => {
			if (data?.id != null || data?.username != null) {
				const updated = { ...current, ...data, language: data.language ?? null, timezone: data.timezone ?? null };
				localStorage.setItem('currentUser', JSON.stringify(updated));
				currentUserSubject.next(updated);
				applyUserLocaleSafe(updated.language);
				return updated;
			}
			return current;
		})
		.catch(() => current);
}

/**
 * Apply the user's preferred locale (browser fallback when null). Loaded
 * lazily to mirror the boot/graphql import style and avoid any module cycle.
 */
function applyUserLocaleSafe(language) {
	import('@/_services/locale.service')
		.then(({ applyUserLocale }) => applyUserLocale(language))
		.catch(() => { /* locale application is best-effort */ });
}

/**
 * Merge a partial update into the current user (localStorage + subject).
 * Used e.g. when the user changes their preferred language in Settings.
 */
function updateCurrentUser(patch) {
	const merged = { ...(currentUserSubject.value || {}), ...patch };
	localStorage.setItem('currentUser', JSON.stringify(merged));
	currentUserSubject.next(merged);
	return merged;
}

export const authzService = {
	login,
	logout,
	currentUser: currentUserSubject.asObservable(),
	get currentUserValue() {
		return currentUserSubject.value;
	},
	hasAnyRole,
	ensureCurrentUserIds,
	updateCurrentUser,
};

/**
 * Authenticate user with username and password
 * @param {string} username - User's username
 * @param {string} password - User's password
 * @returns {Promise<Object>} User object with auth token
 */
function login(username, password) {
	if (!username || !password) {
		return Promise.reject(new Error('Username and password are required'));
	}

	return fetch(`${Utils.host()}/api/login`, requestOptions.post({ username, password }))
		.then(handleResponse)
		.then(user => {
			// Store user details and jwt token in local storage to keep user logged in between page refreshes
			localStorage.setItem('currentUser', JSON.stringify(user));
			currentUserSubject.next(user);
			// Ensure id/username are set for avatar and profile (via `me` GraphQL query)
			return ensureCurrentUserIds().then(() => currentUserSubject.value || user);
		})
		.catch(error => {
			console.error('Login failed:', error);
			throw error;
		});
}

/**
 * Logout current user and redirect to login page
 */
function logout() {
	// Remove user from local storage to log user out
	localStorage.removeItem('currentUser');
	currentUserSubject.next(null);
	location.replace('/login');
}

/**
 * Check if current user has any of the specified roles
 * @param {string[]} roles - Array of role names to check
 * @returns {boolean} True if user has at least one of the specified roles
 */
function hasAnyRole(roles) {
	if (!roles || !Array.isArray(roles) || roles.length === 0) {
		return false;
	}

	const permissions = currentUserSubject.value?.permissions;
	if (!permissions || !Array.isArray(permissions)) {
		return false;
	}

	return permissions.some(userRole => roles.includes(userRole));
}
