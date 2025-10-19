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
const onErrorLink = onError(({graphQLErrors, networkError, operation, forward}) => {
  // We log every GraphQL errors
  if (graphQLErrors) {
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
  // When a 401 error occur, we log-out the user.
  if (networkError) {
    let error = {path: '/pages/500', code: 'ERROR_UNKNOW'};
    if (networkError.statusCode === 401) {
      authzService.logout();
      error.path = '/nx/login';
      error.code = 'ERROR_NOT_AUTHENTICATED';
      location.replace(error.path);
    }
    if (networkError.message === 'Failed to fetch') {
      error.code = 'ERROR_SERVER_DOWN';
    }
    console.log(`[Network error]: ${networkError}`);
    // router.push({ path: error.path, query: { error: error.code } });
  }
  if (operation.variables) {
    const omitTypename = (key, value) => (key === '__typename' ? undefined : value);
    operation.variables = JSON.parse(JSON.stringify(operation.variables), omitTypename);
  }
  return forward(operation).map(data => {
    return data;
  });
});

const apolloClient = new ApolloClient({
  link: onErrorLink.concat(withAuthToken).concat(httpLink),
  cache: new InMemoryCache({addTypename: false}),
  connectToDevTools: process.env.DEV,
});

export default boot(({app}) => {
  // Provide Apollo client using Composition API (Vue 3 + @vue/apollo-composable)
  app.provide(ApolloClients, {
    default: apolloClient,
  });
});

// Export the Apollo client for use outside of components
export {apolloClient};
