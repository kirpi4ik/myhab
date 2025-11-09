import {gql} from '@apollo/client/core';

export const ZONES_GET_ALL = gql`
  query {
    zoneList {
      id
      name
      description
      parent {
        id
        name
      }
      zones {
        id
        name
      }
      peripherals {
        id
        name
        description
        category {
          id
          name
        }
        connectedTo {
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
      name
      description
      zones {
        id
        name
        description
        peripherals {
          id
          name
          description
          category {
            name
          }
          connectedTo {
            id
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
        name
        description
        category {
          name
        }
        connectedTo {
          id
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
      parent {
        id
        name
        description
      }
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
      id
      name
      description
      zones {
        name
        description
      }
      peripherals {
        id
        name
        description
        category {
          name
        }
        connectedTo {
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
    zoneUpdateCustom(id: $id, zone: $zone) {
      id
      name
      description
      parent {
        id
        name
      }
      zones {
        id
        name
      }
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