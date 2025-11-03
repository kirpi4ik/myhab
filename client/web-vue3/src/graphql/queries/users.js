import {gql} from '@apollo/client/core';

export const USER_VALUE_UPDATE = gql`
	mutation ($id: Long!, $user: UserUpdate) {
		userUpdate(id: $id, user: $user) {
			id
			uid
		}
	}
`;
export const USERS_GET_ALL = gql`
	{
		userList {
			id
			uid
			username
			enabled
			accountExpired
			accountLocked
			passwordExpired
			email
			firstName
			lastName
		}
	}
`;
export const USER_GET_BY_ID = gql`
	query findUserById($id: String!) {
		userById(id: $id) {
			id
			uid
			name
			username
			enabled
			accountExpired
			accountLocked
			passwordExpired
			email
			firstName
			lastName
		}
	}
`;
export const USER_GET_BY_ID_WITH_ROLES = gql`
	query findUserById($id: String!) {
		userById(id: $id) {
			id
			uid
			name
			username
			enabled
			accountExpired
			accountLocked
			passwordExpired
			email
			firstName
			lastName
			phoneNr
		}
		userRolesForUser(userId: $id) {
			userId
			roleId
		}
		roleList {
			id
			authority
		}
	}
`;
export const USER_CREATE = gql`
	mutation ($user: UserCreate) {
		userCreate(user: $user) {
			id
			uid
		}
	}
`;
export const USER_DELETE = gql`
	mutation ($id: Long!) {
		userDeleteCascade(id: $id) {
			success
		}
	}
`;
export const ROLES_GET_FOR_USER = gql`
	query rolesForUser($id: String!) {
		roleList {
			id
			authority
		}
		userRolesForUser(userId: $id) {
			userId
			roleId
		}
	}
`;
export const ROLES_SAVE = gql`
	mutation save($input: SaveUserRoles) {
		userRolesSave(input: $input) {
			success
		}
	}
`;
