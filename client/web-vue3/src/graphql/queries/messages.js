import { gql } from '@apollo/client/core';

export const MY_MESSAGES = gql`
	query myMessages($level: String, $state: String) {
		myMessages(level: $level, state: $state) {
			id
			subject
			fromSender
			message
			level
			state
			tsCreated
			tsUpdated
			dedupKey
		}
	}
`;

export const MY_UNREAD_COUNT = gql`
	query {
		myUnreadCount
	}
`;

export const MESSAGE_UPDATE_STATE = gql`
	mutation messageUpdateState($id: ID!, $state: String!) {
		messageUpdateState(id: $id, state: $state) {
			success
			error
		}
	}
`;

export const MESSAGE_BATCH_UPDATE_STATE = gql`
	mutation messageBatchUpdateState($ids: [ID!]!, $state: String!) {
		messageBatchUpdateState(ids: $ids, state: $state) {
			success
			error
		}
	}
`;

export const PUSH_PUBLIC_KEY = gql`
	query {
		pushPublicKey
	}
`;

export const PUSH_SUBSCRIBE = gql`
	mutation pushSubscribe($endpoint: String!, $p256dh: String!, $auth: String!, $userAgent: String) {
		pushSubscribe(endpoint: $endpoint, p256dh: $p256dh, auth: $auth, userAgent: $userAgent) {
			success
			error
		}
	}
`;

export const PUSH_UNSUBSCRIBE = gql`
	mutation pushUnsubscribe($endpoint: String!) {
		pushUnsubscribe(endpoint: $endpoint) {
			success
			error
		}
	}
`;

export const MY_NOTIFICATION_RULES = gql`
	query {
		myNotificationRules {
			id
			matchType
			pattern
			targetState
			tsCreated
		}
	}
`;

export const NOTIFICATION_RULE_CREATE = gql`
	mutation notificationRuleCreate($matchType: String!, $pattern: String!, $targetState: String!) {
		notificationRuleCreate(matchType: $matchType, pattern: $pattern, targetState: $targetState) {
			success
			error
			appliedCount
		}
	}
`;

export const NOTIFICATION_RULE_DELETE = gql`
	mutation notificationRuleDelete($id: ID!) {
		notificationRuleDelete(id: $id) {
			success
			error
		}
	}
`;
