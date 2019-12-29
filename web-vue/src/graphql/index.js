import Vue from 'vue';
import VueApollo from 'vue-apollo';
import ApolloClient from 'apollo-client';
import {HttpLink} from 'apollo-link-http';
import {setContext} from 'apollo-link-context';
import {onError} from 'apollo-link-error';
import {InMemoryCache} from 'apollo-cache-inmemory';
import {authenticationService} from '@/_services';
import {router} from '@/_helpers';
// Notice the use of the link-error tool from Apollo

Vue.use(VueApollo);

const httpLink = new HttpLink({
    uri: process.env.VUE_APP_SERVER_URL + '/graphql',
});
const withAuthToken = setContext((_, {headers}) => {
    // get the authentication token from local storage if it exists
    // return the headers to the context so httpLink can read them
    return {
        headers: {
            authorization: `Bearer ${authenticationService.currentUserValue.access_token}`,
        }
    }
});

// we use a usefull error handling tool provided by Apollo in order to execute some code when errors occur.
const onErrorLink = onError(({graphQLErrors, networkError}) => {
    // We log every GraphQL errors
    if (graphQLErrors) {
        graphQLErrors.map(({message, locations, path}) =>
            console.log(
                `[GraphQL error]: Message: ${message}, Location: ${locations}, Path: ${path}`,
            ),
        );
    }
    // When a 401 error occur, we log-out the user.
    if (networkError) {
        let error = {path: '/pages/500', code: 'ERROR_UNKNOW'};
        if (networkError.statusCode === 401) {
            authenticationService.logout();
            error.path = '/pages/login';
            error.code = 'ERROR_NOT_AUTHENTICATED';
        }
        if (networkError.message === 'Failed to fetch') {
            error.code = "ERROR_SERVER_DOWN";
        }
        console.log(`[Network error]: ${networkError}`);
        router.push({path: error.path, query: {error: error.code}});
    }
});

const apolloClient = new ApolloClient({
    link: onErrorLink.concat(withAuthToken).concat(httpLink),
    cache: new InMemoryCache(),
    watchQuery: {
        fetchPolicy: 'cache-and-network',
        errorPolicy: 'ignore',
    },
    query: {
        fetchPolicy: 'network-only',
        errorPolicy: 'all',
    },
    mutate: {
        errorPolicy: 'all'
    }
});

export default new VueApollo({
    defaultClient: apolloClient,
});