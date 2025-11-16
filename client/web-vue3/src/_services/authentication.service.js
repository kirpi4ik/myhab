import {BehaviorSubject} from 'rxjs';

import {handleResponse, requestOptions, Utils} from '@/_helpers';

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

export const authzService = {
	login,
	logout,
	currentUser: currentUserSubject.asObservable(),
	get currentUserValue() {
		return currentUserSubject.value;
	},
	hasAnyRole,
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
			return user;
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
