import {gql} from '@apollo/client/core';

export const PORT_LIST_HINTS = gql`
  query {
    devicePortList {
      id
      name
      description
      internalRef
      device {
        code
        name
      }
    }
  }
`;

export const PORT_LIST_ALL = gql`
  query {
    devicePortList {
      id
      name
      internalRef
      description
      type
      state
      value
      device {
        id
        code
        name
      }
    }
    deviceList {
      id
      code
      name
      description
    }
  }
`;

export const PORT_GET_BY_ID = gql`
	query ($id: Long!) {
		devicePort(id: $id) {
      id
      name
      internalRef
      description
      type
      state
      value
      tsCreated
      tsUpdated
      device {
        id
        code
        name
        description
      }
      peripherals {
        id
        name
        model
        description
        category {
          id
          name
        }
      }
      cables {
        id
        code
        codeNew
        codeOld
        description
      }
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
      name
      internalRef
      description
      type
      state
      value
      device {
        id
        code
        name
        description
      }
      cables {
        id
        code
        codeNew
        codeOld
        description
      }
    }
    deviceList {
      id
      name
    }
    portTypes
    portStates
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
		}
	}
`;

export const PORT_VALUES_RECENT = gql`
  query ($portId: Long!, $limit: Int) {
    recentPortValues(portId: $portId, limit: $limit) {
      id
      value
      tsCreated
    }
  }
`;