import {gql} from '@apollo/client/core';

export const PORT_LIST_ALL = gql`
  query {
    devicePortList {
      id
      uid
      internalRef
      name
      description
      device {
        id
        code
        name
      }
      type
      state
      value
    }
    deviceList{
      id
      name
      code
    }
  }
`;

export const PORT_GET_BY_ID = gql`
	query ($id: Long!) {
		devicePort(id: $id) {
			id
			uid
			internalRef
			name
			description
			device {
				id
        code
        name
        description
			}
      cables {
        id
        code
        description
      }
			type
			state
			value
		}
		portTypes
		portStates
	}
`;
export const PORT_DELETE_BY_ID = gql`
  mutation ($id: Long!) {
    devicePortDelete(id: $id) {
      error
      success
    }
  }
`;
export const PORT_EDIT_GET_BY_ID = gql`
  query ($id: Long!) {
    devicePort(id: $id) {
      id
      uid
      internalRef
      name
      description
      device {
        id
        code
        name
        description
      }
      cables {
        id
        code
        description
      }
      type
      state
      value
    }
    deviceList{
      id
      name
    }
    portTypes
    portStates
  }
`;
export const PORT_DETAILS_TO_CREATE = gql`
	query ($id: Long!) {
		device(id: $id) {
			id
			name
		}
		devicePeripheralList {
			id
			name
			description
		}
		cableList {
			id
			code
		}
		portTypes
		portStates
	}
`;
export const PORT_VALUE_UPDATE = gql`
	mutation ($id: Long!, $portValue: PortValueUpdate) {
		portValueUpdate(id: $id, portValue: $portValue) {
			id
			uid
		}
	}
`;
export const PORT_UPDATE = gql`
	mutation ($id: Long!, $port: DevicePortUpdate!) {
		updatePort(id: $id, port: $port) {
			id
		}
	}
`;
export const PORT_CREATE = gql`
	mutation ($devicePort: DevicePortCreate) {
		devicePortCreate(devicePort: $devicePort) {
			id
			uid
		}
	}
`;
