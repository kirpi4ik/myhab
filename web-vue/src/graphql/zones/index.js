import gql from "graphql-tag";

export const GET_ALL_ZONES = gql`
    query {
        zoneList{
            uid
            name
            description
            peripherals{
                uid
                name
                description
                category{
                    uid
                }
                connectedTo{
                    uid
                    value
                }
            }
        } }
`;
export const NAV_BREADCRUMB = gql`
    query navigation($zoneUid:String){
        navigation {
            breadcrumb(zoneUid:$zoneUid) {
                name
                zoneUid
            }
        }
    }
`;
export const GET_ZONE_BY_UID = gql`
    query zoneByUid($uid: String!) {
        zoneByUid(uid: $uid) {
            uid
            name
            zones {
                uid
                name
                description
                peripherals {
                    uid
                    name
                    description
                    category {
                        uid
                    }
                    connectedTo{
                        uid
                        value
                    }
                }
            }
            peripherals {
                uid
                name
                description
                category {
                    uid
                }
                connectedTo{
                    uid
                    value
                }
            }
        }
    }

`;
export const GET_ZONES_ROOT = gql`
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
                uid
                name
                description
                category {
                    uid
                }
                connectedTo{
                    uid
                    value
                }
            }
        }
    }
`;

export const UPDATE_PORT_VALUE = gql`
    mutation ($id:Long!, $portValue:PortValueUpdate) {
        portValueUpdate(id:$id, portValue: $portValue){
            id,
            uid
        }
    }
`;
export const PUSH_EVENT = gql`
    mutation pushEvent($input: EventDatInput){
        pushEvent(input:$input){
            p0
        }
    }
`;