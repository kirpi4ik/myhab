package eu.devexpert.madhouse.gql.fetchers

import grails.gorm.transactions.Transactional
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Transactional
class Mutation {

    public DataFetcher save() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                println "SAVED"
                return "saved"
            }
        }
    }
}
