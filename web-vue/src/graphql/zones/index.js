import gql from "graphql-tag";

export const GET_ALL_ZONES = gql`
    query {
        zoneList{
            uid
            name
            description
            parent{
                uid
            }
            peripherals{
                id
                uid
                name
                description
                category{
                    uid
                    name
                }
                connectedTo{
                    uid
                    value
                }
            }
        } }
`

export const UPDATE_PORT_VALUE = gql`
    mutation ($id:Long!, $portValue:PortValueUpdate) {
        portValueUpdate(id:$id, portValue: $portValue){
            id,
            uid
        }
    }
`