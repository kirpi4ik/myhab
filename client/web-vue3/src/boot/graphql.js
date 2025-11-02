import {boot} from 'quasar/wrappers';

import {ApolloClients} from '@vue/apollo-composable';
import {ApolloClient, HttpLink, InMemoryCache} from '@apollo/client/core';
import {setContext} from '@apollo/client/link/context';
import {onError} from '@apollo/client/link/error';
import {authzService} from '@/_services';
import {Utils} from '@/_helpers';
import { Notify } from 'quasar'

// Load Apollo dev tools only in development mode
if (process.env.DEV) {
  import("@apollo/client/dev").then(({ loadErrorMessages, loadDevMessages }) => {
    loadDevMessages();
    loadErrorMessages();
  });
}

// Notice the use of the link-error tool from Apollo
const httpLink = new HttpLink({
  uri: Utils.host() + '/graphql',
});
const withAuthToken = setContext(() => {
  // get the authentication token from local storage if it exists
  // return the headers to the context so httpLink can read them
  return {
    headers: {
      authorization: `Bearer ${authzService.currentUserValue != null ? authzService.currentUserValue.access_token : ''}`,
    },
  };
});

// we use a usefull error handling tool provided by Apollo in order to execute some code when errors occur.
let backendUnavailableCount = 0;
const MAX_BACKEND_ERRORS = 5; // Redirect after 5 consecutive failures
let lastErrorTime = 0;
const ERROR_RESET_TIMEOUT = 10000; // Reset counter if 10 seconds pass between errors

const onErrorLink = onError(({graphQLErrors, networkError, operation, forward}) => {
  // Reset counter if errors are not consecutive (more than 10 seconds apart)
  const now = Date.now();
  if (now - lastErrorTime > ERROR_RESET_TIMEOUT) {
    backendUnavailableCount = 0;
  }
  lastErrorTime = now;
  
  // We log every GraphQL errors
  if (graphQLErrors) {
    // GraphQL errors mean the backend IS responding, so reset counter
    backendUnavailableCount = 0;
    
    graphQLErrors.map(({message, locations, path}) =>
      // console.log(`[GraphQL error]: Message: ${message}, Location: ${locations}, Path: ${path}`),
      Notify.create({
        color: 'negative',
        message: `[GraphQL error]: Message: ${message}, Location: ${JSON.stringify(locations)}, Path: ${path}`,
        icon:'announcement',
        timeout: 10000,
      }),
    );
  }
  
  // When a network error occurs
  if (networkError) {
    let error = {path: '/pages/500', code: 'ERROR_UNKNOW'};
    
    // Handle authentication errors first
    if (networkError.statusCode === 401) {
      backendUnavailableCount = 0; // Reset counter, this is auth issue not backend down
      authzService.logout();
      error.path = '/login';
      error.code = 'ERROR_NOT_AUTHENTICATED';
      location.replace(error.path);
      return;
    }
    
    // Check if this is a cache error (not backend unavailability)
    const isCacheError = networkError.message?.includes('cache') ||
                         networkError.message?.includes('Cache') ||
                         networkError.message?.includes('disk_cache');
    
    if (isCacheError) {
      // Cache errors don't count as backend unavailability
      if (process.env.DEV) {
        console.log('[Apollo]: Cache error (ignoring):', networkError.message);
      }
      backendUnavailableCount = 0;
      return forward(operation);
    }
    
    // Check for actual backend unavailability (connection refused, timeout, etc.)
    const isBackendDown = (networkError.message === 'Failed to fetch' || 
                          networkError.message?.toLowerCase().includes('network') ||
                          networkError.message?.toLowerCase().includes('connection') ||
                          networkError.statusCode === 503 ||
                          networkError.statusCode === 504 ||
                          networkError.statusCode === 0) && // Status 0 often means network failure
                          !isCacheError; // Make sure it's not a cache error
    
    if (isBackendDown) {
      backendUnavailableCount++;
      error.code = 'ERROR_SERVER_DOWN';
      
      if (process.env.DEV) {
        console.log(`[Apollo]: Backend unavailable (${backendUnavailableCount}/${MAX_BACKEND_ERRORS}):`, networkError.message);
      }
      
      // If we've had multiple consecutive failures, redirect to maintenance page
      if (backendUnavailableCount >= MAX_BACKEND_ERRORS) {
        const currentPath = window.location.pathname;
        // Don't redirect if already on maintenance page
        if (!currentPath.includes('/maintenance')) {
          console.error('[Apollo]: Backend server is unavailable after multiple attempts. Redirecting to maintenance page.');
          window.location.href = '/maintenance?title=Backend Unavailable&message=Unable to connect to the backend server. Please check if the server is running on port 8181.&retry=true';
        }
        return;
      }
      
      // Show notification for first few errors
      Notify.create({
        color: 'warning',
        message: `Connection issue detected. Retrying... (${backendUnavailableCount}/${MAX_BACKEND_ERRORS})`,
        icon: 'warning',
        timeout: 3000,
      });
    } else {
      // Reset counter on different type of error
      backendUnavailableCount = 0;
    }
    
    if (process.env.DEV) {
      console.log(`[Network error]: ${networkError}`);
    }
  } else {
    // Reset counter on successful response (no network error)
    if (backendUnavailableCount > 0) {
      console.log('[Apollo]: Backend connection restored!');
      Notify.create({
        color: 'positive',
        message: 'Connection restored!',
        icon: 'check_circle',
        timeout: 2000,
      });
    }
    backendUnavailableCount = 0;
  }
  
  if (operation.variables) {
    const omitTypename = (key, value) => (key === '__typename' ? undefined : value);
    operation.variables = JSON.parse(JSON.stringify(operation.variables), omitTypename);
  }
  return forward(operation).map(data => {
    // Also reset counter on successful data
    if (backendUnavailableCount > 0) {
      backendUnavailableCount = 0;
    }
    return data;
  });
});

const apolloClient = new ApolloClient({
  link: onErrorLink.concat(withAuthToken).concat(httpLink),
  cache: new InMemoryCache({
    addTypename: false,
    // Customize type policies to prevent normalization warnings
    typePolicies: {
      // Prevent Apollo from warning about data loss for simplified nested objects
      Query: {
        fields: {
          // Merge strategy for queries - allow any data
          cable: {
            merge: true,
          },
          devicePort: {
            merge: true,
          },
          devicePeripheral: {
            merge: true,
          },
          zoneById: {
            merge: true,
          }
        }
      }
    },
    // Don't warn about missing fields or data loss
    possibleTypes: {},
    dataIdFromObject: (object) => {
      // Custom cache key generation to avoid normalization issues
      // Return null to disable normalization for objects without proper IDs
      if (object.id) {
        return `${object.__typename}:${object.id}`;
      }
      return null;
    }
  }),
  connectToDevTools: process.env.DEV,
  // Suppress warnings about data loss during normalization
  defaultOptions: {
    watchQuery: {
      errorPolicy: 'all',
      fetchPolicy: 'network-only', // Always fetch fresh data
    },
    query: {
      errorPolicy: 'all',
      fetchPolicy: 'network-only', // Always fetch fresh data
    },
    mutate: {
      errorPolicy: 'all',
    },
  },
});

export default boot(({app}) => {
  // Provide Apollo client using Composition API (Vue 3 + @vue/apollo-composable)
  app.provide(ApolloClients, {
    default: apolloClient,
  });
});

// Export the Apollo client for use outside of components
export {apolloClient};
