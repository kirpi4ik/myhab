import {gql} from '@apollo/client/core';

export const ZONES_GET_ALL = gql`
  query {
    zoneList {
      id
      uid
      name
      description
      peripherals {
        id
        uid
        name
        description
        category {
          uid
        }
        connectedTo {
          uid
          value
        }
      }
    }
  }
`;
export const ZONE_GET_BY_ID = gql`
  query zoneById($id: String!) {
    zoneById(id: $id) {
      id
      uid
      name
      description
      zones {
        id
        uid
        name
        description
        peripherals {
          id
          uid
          name
          description
          category {
            name
          }
          connectedTo {
            id
            uid
            value
            device {
              status
            }
            configurations {
              id
              key
              value
            }
          }
          configurations {
            id
            key
            value
          }
        }
      }
      peripherals {
        id
        uid
        name
        description
        category {
          name
        }
        connectedTo {
          id
          uid
          value
          device {
            status
          }
        }
        configurations {
          id
          key
          value
        }
      }
    }
  }
`;
export const ZONE_GET_BY_ID_MINIMAL = gql`
  query zoneById($id: String!) {
    zoneById(id: $id) {
      id
      name
      description
      categories
      zones {
        id
        name
        description
      }
      devices {
        id
        name
        description
      }
      peripherals {
        id
        name
        description
      }
    }
  }
`;
export const ZONES_GET_ROOT = gql`
  {
    zonesRoot {
      uid
      name
      description
      zones {
        uid
        name
        description
      }
      peripherals {
        id
        uid
        name
        description
        category {
          name
        }
        connectedTo {
          uid
          value
          configurations {
            id
            key
            value
          }
        }
        configurations {
          id
          key
          value
        }
      }
    }
  }
`;

export const ZONE_VALUE_UPDATE = gql`
  mutation ($id: Long!, $zone: ZoneUpdate!) {
    zoneUpdate(id: $id, zone: $zone) {
      id
      name
      description
    }
  }
`;

export const ZONE_CREATE = gql`
  mutation ($zone: ZoneCreate!) {
    zoneCreate(zone: $zone) {
      id
      name
      description
    }
  }
`;

export const ZONE_DELETE = gql`
  mutation ($id: Long!) {
    zoneDelete(id: $id) {
      success
      error
    }
  }
`;