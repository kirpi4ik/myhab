import {gql} from "@apollo/client/core"

export const ZONES_GET_ALL = gql`
    query {
        zoneList{
            uid
            name
            description
            peripherals{
                id
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
export const ZONE_GET_BY_UID = gql`
    query zoneByUid($uid: String!) {
        zoneByUid(uid: $uid) {
            id
            uid
            name
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
                        uid
                    }
                    connectedTo{
                        id
                        uid
                        value
                        device{
                            status
                        }
                    }
                }
            }
            peripherals {
                id
                uid
                name
                description
                category {
                    uid
                }
                connectedTo{
                    id
                    uid
                    value
                    device{
                        status
                    }
                }
                configurations{
                    id
                    key
                    value
                }
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
                    uid
                }
                connectedTo{
                    uid
                    value
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