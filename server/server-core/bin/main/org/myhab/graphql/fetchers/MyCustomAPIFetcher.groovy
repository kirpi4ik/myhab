package org.myhab.graphql.fetchers

import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import groovy.transform.CompileStatic

import javax.inject.Singleton

@Singleton
@CompileStatic
class MyCustomAPIFetcher implements DataFetcher<String> {

    @Override
    String get(DataFetchingEnvironment env) {
        String methodparam = env.getArgument("methodparam")
        return "MyCustomAPI called with ${methodparam}"
    }
}