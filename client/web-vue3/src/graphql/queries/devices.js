import {gql} from '@apollo/client/core';

export const DEVICE_CREATE = gql`
  mutation ($device: DeviceCreate) {
    deviceCreate(device: $device) {
      id
      uid
    }
  }
`;
export const DEVICE_UPDATE = gql`
  mutation ($id: Long!, $device: DeviceUpdate) {
    deviceUpdate(id: $id, device: $device) {
      id
      uid
      code
      name
    }
  }
`;
export const DEVICE_DELETE = gql`
  mutation ($id: Long!) {
    deviceDelete(id: $id) {
      success
    }
  }
`;
export const DEVICE_CATEGORY_CREATE = gql`
  mutation ($deviceCategory: DeviceCategoryCreate) {
    deviceCategoryCreate(deviceCategory: $deviceCategory ) {
      id
    }
  }
`;
export const DEVICE_LIST_ALL = gql`
  {
    deviceList {
      id
      uid
      code
      name
      description
    }
  }
`;
export const DEVICE_GET_DETAILS_FOR_EDIT = gql`
  query device($id: Long!) {
    deviceCategoryList {
      id
      name
    }
    rackList {
      id
      name
      description
    }
    device(id: $id) {
      id
      code
      model
      uid
      name
      description
      type {
        name
      }
      networkAddress {
        ip
        port
        gateway
      }
      rack {
        id
        name
        description
      }
      authAccounts {
        id
        username
        password
        isDefault
      }
    }
  }
`;
export const DEVICE_GET_BY_ID_MINIMAL = gql`
  query device($id: Long!) {
    device(id: $id) {
      id
      code
      model
      uid
      name
      description
    }
  }
`;
export const DEVICE_MODEL_LIST = gql`
  {
    deviceModelList
  }
`;
export const DEVICE_CATEGORIES_LIST = gql`
  {
    deviceCategoryList{
      id
      name
    }
  }
`;
export const DEVICE_CATEGORY_BY_ID = gql`
  query categoryById($id: Long!) {
    deviceCategory(id: $id){
      id
      name
    }
  }
`;
export const DEVICE_GET_BY_ID_WITH_PORT_VALUES = gql`
  query device($id: Long!) {
    device(id: $id) {
      id
      code
      model
      uid
      name
      description
      ports {
        id
        internalRef
        name
        description
        type
        state
        value
      }
    }
  }
`;
export const DEVICE_GET_BY_ID_CHILDS = gql`
  query device($id: Long!) {
    deviceCategoryList {
      id
      name
    }
    rackList {
      id
      name
      description
    }
    device(id: $id) {
      id
      code
      model
      uid
      name
      description
      type {
        name
      }
      networkAddress {
        ip
        port
      }
      rack {
        id
        name
        description
      }
      ports {
        id
        internalRef
        name
        description
        type
        state
      }
      authAccounts {
        id
        username
        password
        isDefault
      }
    }
  }
`;
export const DEVICE_META_GET = gql`
  {
    deviceCategoryList {
      id
      name
    }
    rackList {
      id
      name
      description
    }
  }
`;
export const DEVICE_ACCOUNT_CREATE = gql`
  mutation ($deviceAccount: DeviceAccountCreate) {
    deviceAccountCreate(deviceAccount: $deviceAccount) {
      id
      uid
      username
    }
  }
`;
export const DEVICE_ACCOUNT_UPDATE = gql`
  mutation ($id: Long!, $deviceAccount: DeviceAccountUpdate) {
    deviceAccountUpdate(id: $id, deviceAccount: $deviceAccount) {
      id
      uid
    }
  }
`;
export const DEVICE_ACCOUNT_DELETE = gql`
  mutation ($id: Long!) {
    deviceAccountDelete(id: $id) {
      success
      error
    }
  }
`;
