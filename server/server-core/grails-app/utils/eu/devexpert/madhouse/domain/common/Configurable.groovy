package eu.devexpert.madhouse.domain.common

import eu.devexpert.madhouse.domain.Configuration
import eu.devexpert.madhouse.domain.EntityType

trait Configurable<BaseEntity> {
    Set<Configuration> getConfigurations() {
        return Configuration.withCriteria {
            eq('entityType', getEntityType())
            eq('entityId', getId())
        }
    }

    def getConfigurationByKey(String key) {
        def cfgList = Configuration.createCriteria().list {
            eq('entityType', getEntityType())
            eq('entityId', getId())
            eq('key', key)
        }
        if (cfgList) {
            return cfgList.first()
        } else return null
    }

    abstract Long getId()

    abstract EntityType getEntityType()
}