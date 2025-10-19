import {PUSH_EVENT} from '@/graphql/queries';
import {authzService} from '@/_services';
import {apolloClient} from '@/boot/graphql';

export const lightService = {
	toggle,
};

function toggle(peripheral) {
	let event = {
		p0: 'evt_light',
		p1: 'PERIPHERAL',
		p2: peripheral.id,
		p3: 'mweb',
		p4: peripheral.state === true ? 'off' : 'on',
		p6: authzService.currentUserValue.login,
	};
	apolloClient
		.mutate({
			mutation: PUSH_EVENT,
			variables: { input: event },
		})
		.then(response => {});
}
