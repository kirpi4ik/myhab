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

    List<Configuration> getConfigurationByKey(String key) {
        return Configuration.createCriteria().list {
            eq('entityType', getEntityType())
            eq('entityId', getId())
            eq('key', key)
        }
    }
    abstract Long getId()
    abstract EntityType getEntityType()
}