import { gql } from '@apollo/client/core';

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
			uid
			code
			codeNew
			codeOld
			description
			patchPanelPort
			patchPanel {
				id
				name
        size
			}
			category {
				name
			}
		}
	}
`;
export const CABLE_GET_BY_ID_CHILDS = gql`
	query cableById($id: String!) {
		cableById(id: $id) {
			id
			uid
			description
			code
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
			patchPanel {
				id
				name
				size
				rack {
					id
					name
					description
				}
				name
				description
			}
			patchPanelPort
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
			version
			uid
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
			}
			category {
				id
				name
			}
			code
			codeNew
			codeOld
			description
			patchPanelPort
			nrWires
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
			maxAmp
		}
		rackList {
			id
			name
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
		}
		patchPanelList {
			id
			name
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
export const CABLE_CREATE_GET_DETAILS = gql`
	query {
		rackList {
			id
			name
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
		}
		patchPanelList {
			id
			name
		}
		devicePeripheralList {
			id
			name
		}
	}
`;
