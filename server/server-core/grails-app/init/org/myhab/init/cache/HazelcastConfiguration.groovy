package org.myhab.init.cache

import com.hazelcast.config.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
public class HazelcastConfiguration {
    @Bean
    public Config hazelCastConfig() {
        Config config = new Config();
        config.setInstanceName("hazelcast-instance")
                .addMapConfig(new MapConfig()
                        .setName("internal-cache")
                        .setTimeToLiveSeconds(0)
                        .setEvictionConfig(new EvictionConfig()
                                .setSize(200)
                                .setMaxSizePolicy(MaxSizePolicy.FREE_HEAP_SIZE)
                                .setEvictionPolicy(EvictionPolicy.LRU)
                        ))
        return config;
    }
}
