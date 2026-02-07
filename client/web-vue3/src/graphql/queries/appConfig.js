import {gql} from '@apollo/client/core';

/**
 * Query to get all application configuration entries from GIT
 */
export const APP_CONFIG_LIST = gql`
  query appConfigList {
    appConfigList {
      key
      value
      type
    }
  }
`;

/**
 * Query to get a single configuration value by key
 */
export const APP_CONFIG_GET = gql`
  query config($key: String!) {
    config(key: $key)
  }
`;

/**
 * Mutation to update a configuration value and commit to GIT
 */
export const APP_CONFIG_UPDATE = gql`
  mutation appConfigUpdate($key: String!, $value: String!, $commitMessage: String) {
    appConfigUpdate(key: $key, value: $value, commitMessage: $commitMessage) {
      success
      error
    }
  }
`;

/**
 * Mutation to refresh configuration from GIT (pull latest changes)
 */
export const APP_CONFIG_REFRESH = gql`
  mutation appConfigRefresh {
    appConfigRefresh {
      success
      error
    }
  }
`;
