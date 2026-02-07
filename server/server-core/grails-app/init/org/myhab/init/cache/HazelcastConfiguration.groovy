package org.myhab.init.cache

import com.hazelcast.config.*
import com.hazelcast.core.EntryEvent
import com.hazelcast.core.EntryListener
import com.hazelcast.map.MapEvent
import com.hazelcast.core.HazelcastInstance
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Slf4j
@Component
class CacheEntryListener implements EntryListener<String, Map> {
    @Override
    void entryAdded(EntryEvent<String, Map> event) {
        log.debug("Cache entry added: key=${event.key}, value=${event.value}")
    }

    @Override
    void entryUpdated(EntryEvent<String, Map> event) {
        log.debug("Cache entry updated: key=${event.key}, oldValue=${event.oldValue}, newValue=${event.value}")
    }

    @Override
    void entryRemoved(EntryEvent<String, Map> event) {
        log.debug("Cache entry removed: key=${event.key}, oldValue=${event.oldValue}")
    }

    @Override
    void entryEvicted(EntryEvent<String, Map> event) {
        log.error("⚪ CACHE ENTRY EVICTED: key=${event.key}, oldValue=${event.oldValue}")
    }

    @Override
    void entryExpired(EntryEvent<String, Map> event) {
        log.error("🟠 CACHE ENTRY EXPIRED: key=${event.key}, oldValue=${event.oldValue}")
    }

    @Override
    void mapEvicted(MapEvent event) {
        log.error("⚫ CACHE MAP EVICTED")
    }

    @Override
    void mapCleared(MapEvent event) {
        log.error("⚫ CACHE MAP CLEARED")
    }
}

@Slf4j
@Configuration
public class HazelcastConfiguration {
    @Autowired
    private CacheEntryListener cacheEntryListener
    
    @Bean
    public Config hazelCastConfig() {
        // Use environment-specific cluster name to prevent prod/dev from joining same cluster
        def environment = System.getProperty('grails.env') ?: System.getenv('GRAILS_ENV') ?: 'development'
        def clusterName = "myhab-${environment}"
        
        Config config = new Config();
        config.setClusterName(clusterName)  // CRITICAL: Different cluster name per environment
        config.setInstanceName("hazelcast-instance-${environment}")
        
        log.info("Configuring Hazelcast for environment: ${environment}, cluster: ${clusterName}")
        
        // Enable multicast for cluster formation within the SAME environment
        config.getNetworkConfig().setPort(5701).setPortAutoIncrement(true)
        config.getNetworkConfig().getJoin().getMulticastConfig()
            .setEnabled(true)
            .setMulticastGroup("224.2.2.3")  // Default multicast group
            .setMulticastPort(54327)
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false)
        
        config.addMapConfig(new MapConfig()
                        .setName("internal-cache")
                        .setTimeToLiveSeconds(0)
                        .setEvictionConfig(new EvictionConfig()
                                .setSize(200)
                                .setMaxSizePolicy(MaxSizePolicy.FREE_HEAP_SIZE)
                                .setEvictionPolicy(EvictionPolicy.LRU)
                        ))
                // Configuration for the "expiring" cache map
                .addMapConfig(new MapConfig()
                        .setName("expiring")
                        .setBackupCount(0)  // NO backups - delete is immediate
                        .setAsyncBackupCount(0)  // NO async backups
                        .setTimeToLiveSeconds(0)  // No automatic expiration
                        .setMaxIdleSeconds(0)  // No idle timeout
                        .setReadBackupData(false)  // Always read from primary
                )
        return config;
    }
    
    @Bean
    public HazelcastInstance hazelcastInstance(Config config) {
        HazelcastInstance instance = com.hazelcast.core.Hazelcast.newHazelcastInstance(config)
        
        // Add LOCAL entry listener to the expiring map - catches all operations on this node
        def listenerId = instance.getMap("expiring").addLocalEntryListener(cacheEntryListener)
        log.debug("Hazelcast entry listener registered with ID: ${listenerId} for map 'expiring'")
        
        def members = instance.getCluster().getMembers()
        def clusterName = instance.getConfig().getClusterName()
        log.info("Hazelcast cluster: '${clusterName}', instance: ${instance.getName()}, members: ${members.size()}")
        
        if (members.size() > 1) {
            log.warn("Multiple Hazelcast members detected in cluster '${clusterName}':")
            members.each { member ->
                log.warn("  Member: ${member.getAddress()} ${member.localMember() ? '(THIS NODE)' : '(OTHER NODE)'}")
            }
        } else {
            log.info("Hazelcast running in standalone mode (cluster size: 1)")
        }
        
        return instance
    }
}
