import gql from "graphql-tag";

export const PERIPHERAL_CREATE = gql`
    mutation($devicePeripheral: DevicePeripheralCreate) {
        devicePeripheralCreate(devicePeripheral: $devicePeripheral) {
            id
            uid
        }
    }
`;
export const PERIPHERAL_VALUE_UPDATE = gql`
    mutation ($id:Long!, $devicePeripheralUpdate:DevicePeripheralUpdate!) {
        updatePeripheral(id:$id, peripheral: $devicePeripheralUpdate){
            success
        }
    }
`;
export const PERIPHERAL_DELETE = gql`
    mutation($id: Long!) {
        devicePeripheralDelete(id: $id) {
            success
        }
    }
`;
export const PERIPHERAL_LIST_ALL = gql`
    {
        devicePeripheralList{
            id
            uid
            code
            name
            description
        }
    }
`;
export const PERIPHERAL_META_GET = gql`
    {
        peripheralCategoryList{
            id
            uid
            title
            name
        }
        zoneList{
            id
            uid
            name
            description
        }
        devicePortList{
            id
            uid
            internalRef
            name
            description
        }
    }
`;
export const PERIPHERAL_GET_BY_ID_CHILDS = gql`
    query devicePeripheralById($id:Long!){
        devicePeripheral(id: $id) {
            id
            uid
            name
            description
            code
            codeOld
            model
            maxAmp
            category{
                id
                uid
                title
                name
            }
            connectedTo{
                id
                uid
                internalRef
                name
                description
                device{
                    name
                    code
                }
            }
            zones{
                id
                uid
                name
                description
            }
        }
        peripheralCategoryList{
            id
            uid
            title
            name
        }
        zoneList{
            id
            uid
            name
            description
        }
        devicePortList{
            id
            uid
            internalRef
            name
            description
            device{
                name
                code
            }
        }
    }
`;

export const PERIPHERAl_EVENT_LOGS = gql`
    query eventsByP2($p2:String!, $count: Int!, $offset:Int!){
        eventsByP2(p2:$p2, count: $count, offset:$offset) {
            id
            uid
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

