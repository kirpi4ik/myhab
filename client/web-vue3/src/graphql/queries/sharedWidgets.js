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
			stateDescription
			createdByUsername
			hasPin
			tsCreated
			tsUpdated
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
