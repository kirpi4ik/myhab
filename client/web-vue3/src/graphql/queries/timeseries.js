import {gql} from '@apollo/client/core';

export const TIMESERIES_GET_LATEST_BY_KEY = gql`
  query ($key: String!) {
    statLatestByKey(key: $key) {
      id
      value
      key
      deltaDiff
    }
  }
`;
export const TIMESERIES_GET_LATEST_BY_KEYS = gql`
  query ($keys: [String!]) {
    statLatestByKeys(keys: $keys) {
      id
      key
      value
      deltaDiff
    }
  }
`;
