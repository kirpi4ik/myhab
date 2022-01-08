package eu.devexpert.madhouse.graphql.fetchers

import eu.devexpert.madhouse.domain.infra.Zone
import grails.gorm.transactions.Transactional
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Transactional
class Navigation {

    def breadcrumb() {
        return new DataFetcher() {
            @Override
            Object get(DataFetchingEnvironment environment) throws Exception {
                def zoneId = environment.getArgument("zoneId")
                def zones = []
                getParent(zones, zoneId)
                return zones.reverse()
            }

            def getParent(zones, zoneId) {
                if (zoneId != null) {
                    def zone = Zone.findById(zoneId)
                    zones << [name: zone.name, zoneId: zone.id]
                    if (zone.parent != null) {
                        getParent(zones, zone.parent.id)
                    }
                }
            }
        }

    }
}
