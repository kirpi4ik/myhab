import gql from "graphql-tag";

export const CABLE_CREATE = gql`
    mutation($cable: CableCreate) {
        cableCreate(cable: $cable) {
            uid
        }
    }
`;
export const CABLE_VALUE_UPDATE = gql`
    mutation ($id:Long!, $cableUpdate:CableUpdate) {
        cableUpdate(id:$id, cable: $cableUpdate){
            id,
            uid
        }
    }
`;
export const CABLE_DELETE = gql`
    mutation($id: Long!) {
        cableDelete(id: $id) {
            success
        }
    }
`;
export const CABLE_LIST_ALL = gql`
    {
        cableList{
            id
            uid
            code
            codeNew
            codeOld
            description
            patchPanel{
                id
                code
            }
            category{
                name
            }
        }
    }
`;
export const CABLE_GET_BY_ID_CHILDS = gql`
    query cableByUid($uid:String!){
        cableByUid(uid: $uid) {
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
                code
                size
                rack {
                    id
                    name
                    description
                }
                code
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
                title
                name
            }
            peripherals {
                id
                version
                name
                code
                model
                description
                maxAmp
                codeOld
            }
            connectedTo {
                id
                name
                code
                model
                description
            }
            zones {
                id
                name
            }
        }
    }
`;