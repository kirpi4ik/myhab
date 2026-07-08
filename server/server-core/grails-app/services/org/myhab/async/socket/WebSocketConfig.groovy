package org.myhab.async.socket

import grails.plugin.springwebsocket.DefaultWebSocketConfig
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends DefaultWebSocketConfig {

    def configProvider

    /**
     * Scheduler required for SimpleBroker STOMP heartbeats (without one,
     * setHeartbeatValue fails at startup).
     */
    @Bean
    ThreadPoolTaskScheduler wsHeartbeatTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler()
        scheduler.poolSize = 1
        scheduler.threadNamePrefix = 'ws-heartbeat-'
        return scheduler
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry messageBrokerRegistry) {
        // Enable STOMP heartbeats (server sends every 10s, expects every 10s).
        // SimpleBroker defaults to {0,0} (none), which lets half-open sockets
        // (mobile Wi-Fi drops, NAT rebinds) go undetected forever: clients keep
        // believing they are connected and never reconnect, so long-running
        // dashboards silently stop receiving events until a manual reload.
        messageBrokerRegistry.enableSimpleBroker("/queue", "/topic")
                .setHeartbeatValue([10000L, 10000L] as long[])
                .setTaskScheduler(wsHeartbeatTaskScheduler())
        messageBrokerRegistry.setApplicationDestinationPrefixes "/app"
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/stomp")
                .setAllowedOrigins(configProvider.getList(String.class, "cors.allowedOrigin") as String[])
                .withSockJS();
    }

}
