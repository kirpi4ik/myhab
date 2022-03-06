import { BehaviorSubject } from 'rxjs';

import { handleResponse, requestOptions, Utils } from '@/_helpers';

const currentUserSubject = new BehaviorSubject(JSON.parse(localStorage.getItem('currentUser')));

export const authzService = {
	login,
	logout,
	currentUser: currentUserSubject.asObservable(),
	get currentUserValue() {
		return currentUserSubject.value;
	},
	hasAnyRole,
};

function login(username, password) {
	return fetch(`${Utils.host()}/api/login`, requestOptions.post({ username, password }))
		.then(handleResponse)
		.then(user => {
			// store user details and jwt token in local storage to keep user logged in between page refreshes
			localStorage.setItem('currentUser', JSON.stringify(user));
			currentUserSubject.next(user);
			return user;
		});
}

function logout() {
	// remove user from local storage to log user out
	localStorage.removeItem('currentUser');
	currentUserSubject.next(null);
	location.replace('/login');
}

function hasAnyRole(roles) {
	return (
		currentUserSubject.value.permissions.filter(function (userRole) {
			return roles.includes(userRole);
		}).length > 0
	);
}
