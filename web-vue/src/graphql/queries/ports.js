import {gql} from "@apollo/client/core"

export const PORT_GET_BY_ID = gql`
    query($id:Long!){
        devicePort(id:$id){
            id
            uid
            internalRef
            name
            description
            device{
                id
            }
            type
            state
            value
            cables{
                id
                code
                description
            }
            peripherals{
                id
                name
            }
        }
        devicePeripheralList{
            id
            name
            description
        }
        cableList{
            id
            code
        }
        portTypes
        portStates
    }
`;
export const PORT_DETAILS_TO_CREATE = gql`
    query($id:Long!){
        device(id:$id){
            id
            name           
        }
        devicePeripheralList{
            id
            name
            description
        }
        cableList{
            id
            code
        }
        portTypes
        portStates
    }
`;
export const PORT_VALUE_UPDATE = gql`
    mutation ($id:Long!, $portValue:PortValueUpdate) {
        portValueUpdate(id:$id, portValue: $portValue){
            id,
            uid
        }
    }
`;
export const PORT_UPDATE = gql`
    mutation ($id: Long!, $port: DevicePortUpdate!) {
        updatePort(id:$id, port: $port){
            id
        }
    }
`;
export const PORT_CREATE = gql`
    mutation ($devicePort:DevicePortCreate) {
        devicePortCreate(devicePort: $devicePort){
            id,
            uid
        }
    }
`;
