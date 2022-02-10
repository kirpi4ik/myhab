import { gql } from '@apollo/client/core';

export * from './ports';
export * from './peripherals';
export * from './zones';
export * from './cables';
export * from './devices';
export * from './configurations';
export * from './users';

export const NAV_BREADCRUMB = gql`
	query navigation($zoneId: String) {
		navigation {
			breadcrumb(zoneId: $zoneId) {
				name
				zoneId
			}
		}
	}
`;

export const PUSH_EVENT = gql`
	mutation pushEvent($input: EventDatInput) {
		pushEvent(input: $input) {
			p0
		}
	}
`;
