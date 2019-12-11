package eu.devexpert.madhouse.gql.fetchers

import grails.events.Event
import grails.events.EventPublisher
import grails.gorm.transactions.Transactional
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Transactional
class Mutation implements EventPublisher {
    public DataFetcher pushEvent() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def pushedEvent = environment.getArgument("input")
                publish("${pushedEvent['p0']}", pushedEvent)
                publish("log_event", pushedEvent)
                return pushedEvent
            }
        }
    }
}
