import gql from "graphql-tag";

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
export const ZONE_GET_BY_UID = gql`
    query zoneByUid($uid: String!) {
        zoneByUid(uid: $uid) {
            uid
            name
            zones {
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
export const USER_VALUE_UPDATE = gql`
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
export const DEVICE_CREATE = gql`
    mutation($device: DeviceCreate) {
        deviceCreate(device: $device) {
            uid
        }
    }
`;
export const DEVICE_DELETE = gql`
    mutation($id: Long!) {
        deviceDelete(id: $id) {
            success
        }
    }
`;
export const DEVICE_LIST_ALL = gql`
    {
        deviceList{
            id
            uid
            code
            name
            description
        }
    }
`;
export const DEVICE_GET_BY_ID_CHILDS = gql`
    query findDeviceByUid($uid:String!){
        deviceByUid(uid: $uid) {
            id
            code
            model
            uid
            name
            description
            networkAddress{
                ip
                port
            }
            rack{
                id
                name
                description
            }
            ports{
                id
                internalRef
                name
                description
                type
                state
            }

        }
    }
`;
export const PERIPHERAL_CREATE = gql`
    mutation($devicePeripheral: DevicePeripheralCreate) {
        devicePeripheralCreate(devicePeripheral: $devicePeripheral) {
            uid
        }
    }
`;
export const PERIPHERAL_VALUE_UPDATE = gql`
    mutation ($id:Long!, $devicePeripheralUpdate:DevicePeripheralUpdate) {
        devicePeripheralUpdate(id:$id, devicePeripheral: $devicePeripheralUpdate){
            id,
            uid
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
export const PERIPHERAL_GET_BY_ID_CHILDS = gql`
    query devicePeripheralByUid($uid:String!){
        devicePeripheralByUid(uid: $uid) {
            id
            uid
            name
            description
            code

        }
    }
`;
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
            description
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
export const CONFIGURATION_SET_VALUE = gql`
    mutation ($key:String!, $entityId:Long!, $entityType:EntityType!, $value:String!) {
        savePropertyValue(key: $key, entityId: $entityId, entityType: $entityType, value: $value) {
            id
            version
            name
            description
            value
            entityType
            key
            entityId
        }
    }
`;
export const CONFIGURATION_ADDLIST_CONFIG_VALUE = gql`
    mutation ($key:String!, $entityId:Long!, $entityType:EntityType!, $value:String!) {
        addListItemProperty(key: $key, entityId: $entityId, entityType: $entityType, value: $value) {
            id
            version
            name
            description
            value
            entityType
            key
            entityId
        }
    }
`;
export const CONFIGURATION_REMOVE_CONFIG = gql`
    mutation ($id:Long!) {
        removeConfig(id: $id) {
            success
        }
    }
`;
export const CONFIGURATION_GET_VALUE = gql`
    query configPropertyByKey($key:String!, $entityId:Long!, $entityType:EntityType!){
        configPropertyByKey(key: $key, entityId: $entityId, entityType: $entityType) {
            id
            key
            value
        }
    }
`;
export const CONFIGURATION_GET_LIST_VALUE = gql`
    query configListByKey($key:String!, $entityId:Long!, $entityType:EntityType!){
        configListByKey(key: $key, entityId: $entityId, entityType: $entityType) {
            id
            key
            value
        }
    }
`;
export const CACHE_GET_VALUE = gql`
    query getCache($cacheName: String!, $cacheKey: String!){
        cache(cacheName: $cacheName, cacheKey: $cacheKey) {
            cachedValue
        }
    }
`;
export const CONFIGURATION_DELETE = gql`
    mutation ($id:Long!){
        configurationDelete(id: $id) {
            success
            error
        }
    }

`;