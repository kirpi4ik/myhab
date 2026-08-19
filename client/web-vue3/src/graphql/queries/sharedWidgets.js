import { gql } from '@apollo/client/core';

export const SHARED_WIDGETS = gql`
	query sharedWidgets($state: String) {
		sharedWidgets(state: $state) {
			id
			token
			widgetType
			peripheralId
			peripheralName
			shareStartDate
			shareExpireDate
			actionsAllowed
			actionsUsed
			state
			description
			stateDescription
			createdByUsername
			hasPin
			tsCreated
			tsUpdated
		}
	}
`;

export const SHARED_WIDGET_AUDIT = gql`
	query sharedWidgetAudit($sharedWidgetId: ID!, $count: Int, $offset: Int) {
		sharedWidgetAudit(sharedWidgetId: $sharedWidgetId, count: $count, offset: $offset) {
			id
			action
			result
			resultDescription
			remoteAddress
			userAgent
			tsCreated
		}
	}
`;

export const SHARED_WIDGET_CREATE = gql`
	mutation sharedWidgetCreate($input: SharedWidgetInput!) {
		sharedWidgetCreate(input: $input) {
			success
			error
			token
			shareUrl
		}
	}
`;

export const SHARED_WIDGET_UPDATE_STATE = gql`
	mutation sharedWidgetUpdateState($id: ID!, $state: String!, $stateDescription: String) {
		sharedWidgetUpdateState(id: $id, state: $state, stateDescription: $stateDescription) {
			success
			error
		}
	}
`;
