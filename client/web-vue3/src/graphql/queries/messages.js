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
