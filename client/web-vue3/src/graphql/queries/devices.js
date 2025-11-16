import {gql} from '@apollo/client/core';

export const DEVICE_CREATE = gql`
  mutation ($device: DeviceCreate) {
    deviceCreate(device: $device) {
      id
    }
  }
`;
export const DEVICE_UPDATE_CUSTOM = gql`
  mutation ($id: Long!, $device: DeviceUpdate!) {
    deviceUpdateCustom(id: $id, device: $device) {
      id
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
export const DEVICE_CATEGORY_UPDATE = gql`
  mutation ($id: Long!, $deviceCategory: DeviceCategoryUpdate) {
    deviceCategoryUpdate(id: $id, deviceCategory: $deviceCategory) {
      id
      name
    }
  }
`;
export const DEVICE_CATEGORY_DELETE = gql`
  mutation ($id: Long!) {
    deviceCategoryDelete(id: $id) {
      success
      error
    }
  }
`;
export const DEVICE_LIST_ALL = gql`
  query {
    deviceList {
      id
      code
      model
      name
      description
      type {
        name
      }
      rack {
        id
        name
        description
      }
      ports {
        id
        name
        internalRef
      }
      tsCreated
      tsUpdated
    }
  }
`;

export const DEVICE_LIST_ALL_WITH_PORTS = gql`
  query {
    deviceList {
      id
      code
      model
      name
      description
      ports {
        id
        name
        internalRef
      }
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
      tsCreated
      tsUpdated
    }
  }
`;
export const DEVICE_CATEGORY_BY_ID = gql`
  query categoryById($id: Long!) {
    deviceCategory(id: $id){
      id
      name
      tsCreated
      tsUpdated
    }
  }
`;
export const DEVICE_GET_BY_ID_WITH_PORT_VALUES = gql`
  query device($id: Long!) {
    device(id: $id) {
      id
      code
      model
      name
      description
      status
      ports {
        id
        name
        internalRef
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
      zones {
        id
        name
        description
      }
      ports {
        id
        name
        internalRef
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
