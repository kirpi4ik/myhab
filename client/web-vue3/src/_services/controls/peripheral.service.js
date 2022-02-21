import { PUSH_EVENT } from '@/graphql/queries';
import { authenticationService } from '@/_services';
import { apolloProvider } from '@/boot/graphql';

export const heatService = {
  peripheralInit,
};

const peripheralInit = peripheral => {
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
    let asset = {
      data: peripheral,
      id: peripheral['id'],
      value: peripheral['value'],
      state: peripheral['state'],
      deviceState: peripheral['deviceState'],
      expiration: assetExpirationMap.value[peripheral.connectedTo[0].id],
    };
    peripheralList.value.push(asset);
  }
};
