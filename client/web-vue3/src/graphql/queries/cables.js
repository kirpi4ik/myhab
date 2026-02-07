import {gql} from '@apollo/client/core';

export const CABLE_CREATE = gql`
  mutation ($cable: CableCreate) {
    cableCreate(cable: $cable) {
      id
    }
  }
`;
export const CABLE_VALUE_UPDATE = gql`
  mutation ($id: Long!, $cable: CableUpdate!) {
    updateCable(id: $id, cable: $cable) {
      id
    }
  }
`;
export const CABLE_DELETE = gql`
  mutation ($id: Long!) {
    cableDelete(id: $id) {
      success
    }
  }
`;
export const RACK_LIST_ALL = gql`
  {
    rackList {
      id
      name
      description
    }
  }
`;
export const CABLE_LIST_ALL = gql`
  {
    cableList {
      id
      code
      codeNew
      codeOld
      description
      patchPanelPort
      rack {
        name
      }
      patchPanel {
        id
        name
        size
      }
      category {
        name
      }
      tsCreated
      tsUpdated
    }
    cableCategoryList{
      id
      name
    }
  }
`;
export const CABLE_BY_ID = gql`
  query cableById($id: Long!) {
    cable(id: $id) {
      id
      code
      codeNew
      codeOld
      description
      nrWires
      patchPanelPort
      rackRowNr
      orderInRow
      maxAmp
      tsCreated
      tsUpdated
      patchPanel {
        id
        name
        size
        description
        rack {
          id
          name
          description
        }
      }
      rack {
        id
        name
        description
      }
      category {
        id
        name
      }
      peripherals {
        id
        name
        model
        description
      }
      connectedTo {
        id
        name
        internalRef
      }
      zones {
        id
        name
      }
    }
  }
`;

export const CABLE_EDIT_GET_DETAILS = gql`
  query cableById($id: Long!) {
    cable(id: $id) {
      id
      code
      codeNew
      codeOld
      description
      nrWires
      patchPanelPort
      rackRowNr
      orderInRow
      maxAmp
      version
      tsCreated
      tsUpdated
      entityType
      rack {
        id
        name
      }
      patchPanel {
        id
        name
        size
      }
      category {
        id
        name
      }
      peripherals {
        id
        name
      }
      connectedTo {
        id
        name
        internalRef
        device {
          code
        }
      }
      zones {
        id
        name
      }
    }
    rackList {
      id
      name
      description
      patchPanels {
        id
        name
      }
    }
    cableCategoryList {
      id
      name
    }
    zoneList {
      id
      name
      description
    }
    patchPanelList {
      id
      name
      size
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
    devicePeripheralList {
      id
      name
    }
    devicePortList {
      id
      internalRef
      name
      description
      device {
        code
        description
      }
    }
  }
`;
