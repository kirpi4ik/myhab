import {gql} from '@apollo/client/core';

export const PERIPHERAL_CREATE = gql`
  mutation ($devicePeripheral: DevicePeripheralUpdate!) {
    peripheralCreate(devicePeripheral: $devicePeripheral) {
      id
    }
  }
`;
export const PERIPHERAL_UPDATE = gql`
    mutation ($id: Long!, $devicePeripheral: DevicePeripheralUpdate!) {
        updatePeripheral(id: $id, peripheral: $devicePeripheral) {
            success
            error
        }
    }
`;
export const PERIPHERAL_CATEGORY_CREATE = gql`
  mutation ($peripheralCategory: PeripheralCategoryCreate) {
    peripheralCategoryCreate(peripheralCategory: $peripheralCategory ) {
      id
    }
  }
`;
export const PERIPHERAL_DELETE = gql`
  mutation ($id: Long!) {
    devicePeripheralDelete(id: $id) {
      success
    }
  }
`;
export const PERIPHERAL_LIST_ALL = gql`
  query {
    devicePeripheralList {
      id
      name
      model
      description
      category {
        id
        name
      }
      connectedTo {
        id
      }
      tsCreated
      tsUpdated
    }
  }
`;

export const PERIPHERAL_CATEGORIES = gql`
  query {
    peripheralCategoryList {
      id
      name
      title
    }
  }
`;
export const PERIPHERAL_CATEGORY_BY_ID = gql`
  query peripheralCategory($id: Long!) {
    peripheralCategory(id: $id){
      id
      name
    }
  }
`;

export const PERIPHERAL_CATEGORY_GET_DETAILS = gql`
  query peripheralCategory($id: Long!) {
    peripheralCategory(id: $id) {
      id
      name
      title
      entityType
      peripherals {
        id
        name
      }
      cables {
        id
        code
      }
    }
  }
`;
export const PERIPHERAL_CATEGORY_UPDATE = gql`
  mutation ($id: Long!, $peripheralCategory: PeripheralCategoryUpdate!) {
    peripheralCategoryUpdate(id: $id, peripheralCategory: $peripheralCategory) {
      id
      name
      title
    }
  }
`;

export const PERIPHERAL_CATEGORY_DELETE = gql`
  mutation ($id: Long!) {
    peripheralCategoryDelete(id: $id) {
      success
      error
    }
  }
`;
export const PERIPHERAL_GET_BY_ID = gql`
  query devicePeripheralById($id: Long!) {
    devicePeripheral(id: $id) {
      id
      name
      description
      model
      maxAmp
      category {
        id
        name
      }
      connectedTo {
        id
        name
        value
        internalRef
        device {
          code
          status
        }
      }
      configurations {
        id
        key
        value
      }
      zones {
        id
        name
        description
      }
    }
    deviceList {
      id
      code
      name
      ports {
        id
        name
        internalRef
      }
    }
  }
`;

export const PERIPHERAL_LIST_WUI = gql`
  query devicePeripheralList {
    devicePeripheralList {
      id
      name
      description
      category {
        id
        name
        title
      }
      connectedTo {
        id
        internalRef
        name
        description
        value
        device {
          status
        }
      }
    }
  }
`;
export const PERIPHERAl_EVENT_LOGS = gql`
  query eventsByP2($p2: String!, $count: Int!, $offset: Int!) {
    eventsByP2(p2: $p2, count: $count, offset: $offset) {
      id
      tsCreated
      entityType
      p4
      p6
      p2
      p3
      p1
      p0
    }
  }
`;
