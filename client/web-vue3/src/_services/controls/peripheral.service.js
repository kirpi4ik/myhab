export const peripheralService = {
  peripheralInit,
};

function peripheralInit(assetExpirationMap, peripheral) {
  if (peripheral != null) {
    if (peripheral.connectedTo && peripheral.connectedTo.length > 0) {
      let port = peripheral.connectedTo[0];
      if (port != null && port.device != null) {
        peripheral['value'] = peripheral.connectedTo[0].value;
        peripheral['state'] = peripheral.connectedTo[0].value === 'OFF';
        peripheral['deviceState'] = peripheral.connectedTo[0].device.status;
      } else {
        peripheral['deviceState'] = 'OFFLINE';
      }
    }

    let portId = (peripheral.connectedTo && peripheral.connectedTo.length) ? peripheral.connectedTo[0].id : null
    let asset = {
      data: peripheral,
      id: peripheral['id'],
      value: peripheral['value'],
      state: peripheral['state'],
      deviceState: peripheral['deviceState'],
      expiration: assetExpirationMap.value[portId],
    };
    return asset
  }
}
