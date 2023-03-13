export const peripheralService = {
	peripheralInit,
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
