package eu.devexpert.madhouse.graphql.fetchers

import graphql.schema.DataFetcher
import groovy.transform.InheritConstructors

@InheritConstructors
abstract class DefaultDataFetcher<T> implements DataFetcher<T> {

    def service

    DefaultDataFetcher(service) {
        this.service = service
    }
}
