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
export const UPDATE_USER_VALUE = gql`
    mutation ($id:Long!, $user:UserUpdate) {
        userUpdate(id:$id, user: $user){
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
export const USERS_GET_ALL = gql`
    {
        userList{
            id
            uid
            username
            enabled
            accountExpired
            accountLocked
            passwordExpired
            email
            firstName
            lastName
        }
    }
`;
export const USER_GET_BY_ID = gql`
    query findUserByUid($uid:String!){
        userByUid(uid: $uid) {
            id
            uid
            name
            username
            enabled
            accountExpired
            accountLocked
            passwordExpired
            email
            firstName
            lastName
        }
    }
`;
export const USER_GET_BY_ID_WITH_ROLES = gql`
    query findUserByUid($uid:String!){
        userByUid(uid: $uid) {
            id
            uid
            name
            username
            enabled
            accountExpired
            accountLocked
            passwordExpired
            email
            firstName
            lastName
        }
        userRolesForUser(userUid: $uid) {
            userId
            roleId
        }
        roleList {
            id
            authority
        }
        
    }
`;
export const USER_CREATE = gql`
    mutation($user: UserCreate) {
      userCreate(user: $user) {
        uid
      }
    }
`;
export const USER_DELETE = gql`
    mutation($id: Long!) {
        userDeleteCascade(id: $id) {
        success
      }
    }
`;
export const ROLES_GET_FOR_USER = gql`
    query rolesForUser($uid: String!) {
        roleList {
            id
            authority
        }
        userRolesForUser(userUid: $uid) {
            userId
            roleId
        }
    }

`;
export const ROLES_SAVE = gql`
    mutation save($input:SaveUserRoles){
        userRolesSave(input:$input){
            success
        }
    }
`;