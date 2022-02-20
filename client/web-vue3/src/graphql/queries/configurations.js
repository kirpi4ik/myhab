import { gql } from '@apollo/client/core';

export const CONFIGURATION_SET_VALUE = gql`
	mutation ($key: String!, $entityId: Long!, $entityType: EntityType!, $value: String!) {
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
export const CONFIGURATION_LIST = gql`
	query configurationListByEntity($entityType: EntityType!, $entityId: Long!) {
		configurationListByEntity(entityType: $entityType, entityId: $entityId) {
			id
			key
			value
			entityType
			entityId
		}
	}
`;
export const CONFIGURATION_ADDLIST_CONFIG_VALUE = gql`
	mutation ($key: String!, $entityId: Long!, $entityType: EntityType!, $value: String!) {
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
	mutation ($id: Long!) {
		removeConfig(id: $id) {
			success
		}
	}
`;
export const CONFIGURATION_GET_VALUE = gql`
	query configPropertyByKey($key: String!, $entityId: Long!, $entityType: EntityType!) {
		configPropertyByKey(key: $key, entityId: $entityId, entityType: $entityType) {
			id
			key
			value
		}
	}
`;
export const CONFIGURATION_GET_LIST_VALUE = gql`
	query configListByKey($key: String!, $entityId: Long!, $entityType: EntityType!) {
		configListByKey(key: $key, entityId: $entityId, entityType: $entityType) {
			id
			key
			value
		}
	}
`;
export const CACHE_GET_VALUE = gql`
	query getCache($cacheName: String!, $cacheKey: String!) {
		cache(cacheName: $cacheName, cacheKey: $cacheKey) {
			cachedValue
		}
	}
`;
export const CACHE_GET_ALL_VALUES = gql`
  query getCacheAll($cacheName: String!){
    cacheAll(cacheName: $cacheName) {
      cacheKey
      cacheName
      cachedValue
    }
  }
`;2
export const CONFIGURATION_DELETE = gql`
	mutation ($id: Long!) {
		configurationDelete(id: $id) {
			success
			error
		}
	}
`;
export const CONFIG_GLOBAL_GET_STRING_VAL = gql`
	query ($key: String!) {
		config(key: $key)
	}
`;
