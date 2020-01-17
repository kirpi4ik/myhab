package eu.devexpert.madhouse.gql.fetchers

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
                def zoneUid = environment.getArgument("zoneUid")
                def zones = []
                getParent(zones, zoneUid)
                return zones.reverse()
            }

            def getParent(zones, zoneUid) {
                if (zoneUid != null) {
                    def zone = Zone.findByUid(zoneUid)
                    zones << [name: zone.name, zoneUid: zone.uid]
                    if (zone.parent != null) {
                        getParent(zones, zone.parent.uid)
                    }
                }
            }
        }

    }
}
