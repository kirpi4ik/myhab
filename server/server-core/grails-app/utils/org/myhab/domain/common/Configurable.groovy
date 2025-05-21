package org.myhab.domain.common

import org.myhab.config.CfgKey
import org.myhab.domain.Configuration
import org.myhab.domain.EntityType

trait Configurable<T extends BaseEntity> {
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

    Configuration getConfigurationByKey(CfgKey.Key key) {
        def cfgList = Configuration.createCriteria().list {
            eq('entityType', getEntityType())
            eq('entityId', getId())
            eq('key', key.key())
        }
        if (cfgList) {
            return cfgList.first()
        } else return null
    }

    abstract Long getId()

    abstract EntityType getEntityType()
}