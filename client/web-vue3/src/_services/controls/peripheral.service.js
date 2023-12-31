import {authzService} from "@/_services";
import {apolloProvider} from "boot/graphql";
import {PUSH_EVENT} from "@/graphql/queries";

export const peripheralService = {
	peripheralInit,toggle
};

function peripheralInit(cacheMap, peripheral) {
	if (peripheral != null) {
		if (peripheral.connectedTo && peripheral.connectedTo.length > 0) {
			let port = peripheral.connectedTo[0];
			if (port != null && port.device != null) {
				peripheral['value'] = peripheral.connectedTo[0].value;
				peripheral['state'] = peripheral.connectedTo[0].value === 'ON';
				peripheral['deviceState'] = peripheral.connectedTo[0].device.status;
			} else {
				peripheral['deviceState'] = 'OFFLINE';
			}
		}

		let portId = peripheral.connectedTo && peripheral.connectedTo.length ? peripheral.connectedTo[0].id : null;
		let expiration;
		if (cacheMap) {
			expiration =
				cacheMap.value[portId] && cacheMap.value[portId]['expiring'] ? cacheMap.value[portId]['expiring']['cachedValue'] : null;
		} else {
			if (!peripheral['expiration'] || peripheral['expiration'] == 'null') {
				expiration = null;
			} else {
				expiration = peripheral['expiration'];
			}
		}
		return {
			data: peripheral,
			id: peripheral['id'],
			value: peripheral['value'],
			state: peripheral['state'],
			deviceState: peripheral['deviceState'],
			expiration: expiration,
		};
	}
}

function toggle(peripheral) {
  let event = {
    p0: 'evt_light',
    p1: 'PERIPHERAL',
    p2: peripheral.id,
    p3: 'mweb',
    p4: peripheral.state === true ? 'off' : 'on',
    p6: authzService.currentUserValue.login,
  };
  apolloProvider.defaultClient
    .mutate({
      mutation: PUSH_EVENT,
      variables: { input: event },
    })
    .then(response => {});
}
