import gql from "graphql-tag";

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
            mustSendToServer
            mode
            model
            runScenario
            action
            runAction
            value
            miscValue
            hystDeviationValue
            cables{
                id
                code
                description
            }
            peripherals{
                id
                code
                name
            }
        }
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
    mutation ($id:Long!, $devicePort:DevicePortUpdate) {
        devicePortUpdate(id:$id, devicePort: $devicePort){
            id,
            uid
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
