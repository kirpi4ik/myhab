import Vue from 'vue';
import VueApollo from 'vue-apollo';
import {ApolloClient, HttpLink, InMemoryCache} from '@apollo/client/core';
import {setContext} from '@apollo/client/link/context';
import {onError} from '@apollo/client/link/error';
import {authenticationService} from '@/_services';
import {router, Utils} from '@/_helpers';

// Notice the use of the link-error tool from Apollo

Vue.use(VueApollo);

const httpLink = new HttpLink({
    uri: Utils.host() + '/graphql',
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
const onErrorLink = onError(({graphQLErrors, networkError, operation, forward}) => {
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
    if (operation.variables) {
        const omitTypename = (key, value) => (key === '__typename' ? undefined : value);
        operation.variables = JSON.parse(JSON.stringify(operation.variables), omitTypename);
    }
    return forward(operation).map((data) => {
        return data;
    });
});

const apolloClient = new ApolloClient({
    link: onErrorLink.concat(withAuthToken).concat(httpLink),
    cache: new InMemoryCache({
        addTypename: false
    }),
    watchQuery: {
        fetchPolicy: 'cache-and-network',
        errorPolicy: 'ignore',
    },
    query: {
        //fetchPolicy: "no-cache",
        fetchPolicy: 'cache-and-network',
        errorPolicy: 'all',
    },
    mutate: {
        errorPolicy: 'all'
    }
});

export default new VueApollo({
    defaultClient: apolloClient,
});