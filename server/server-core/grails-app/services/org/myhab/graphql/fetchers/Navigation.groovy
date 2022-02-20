package org.myhab.graphql.fetchers

import org.myhab.domain.EntityType
import org.myhab.domain.infra.Zone
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
                def type = environment.getArgument("type")
                def id = environment.getArgument("id")
                def zones = []
                getParent(zones, id)
                return zones.reverse()
            }

            def getParent(zones, id) {
                if (id != null) {
                    def zone = Zone.findById(id)
                    zones << [name: zone.name, id: zone.id, type: EntityType.ZONE]
                    if (zone.parent != null) {
                        getParent(zones, zone.parent.id)
                    }
                }
            }
        }

    }
}
