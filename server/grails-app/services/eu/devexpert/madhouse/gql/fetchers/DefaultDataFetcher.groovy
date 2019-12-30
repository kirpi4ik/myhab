package eu.devexpert.madhouse.gql.fetchers

import graphql.schema.DataFetcher
import groovy.transform.InheritConstructors
import org.grails.gorm.graphql.fetcher.DefaultGormDataFetcher

@InheritConstructors
abstract class DefaultDataFetcher<T> implements DataFetcher<T> {

    def service

    DefaultDataFetcher(service) {
        this.service = service
    }
}
