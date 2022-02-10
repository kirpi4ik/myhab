import { PUSH_EVENT } from '@/graphql/queries';
import { authenticationService } from '@/_services';
import apolloProvider from '../../graphql';

export const heatService = {
	toggle,
};

function toggle(peripheral) {
	let event = {
		p0: 'evt_heat',
		p1: 'PERIPHERAL',
		p2: peripheral.id,
		p3: 'mweb',
		p4: peripheral.state === true ? 'off' : 'on',
		p6: authenticationService.currentUserValue.login,
	};
	apolloProvider.defaultClient
		.mutate({
			mutation: PUSH_EVENT,
			variables: { input: event },
		})
		.then(response => {});
}
