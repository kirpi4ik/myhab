package eu.devexpert.madhouse.async.socket

import grails.plugin.springwebsocket.DefaultWebSocketConfig
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig extends DefaultWebSocketConfig {

    @Value('${allowedOrigin}')
    String allowedOrigin

    @Override
    public void configureMessageBroker(MessageBrokerRegistry messageBrokerRegistry) {
        messageBrokerRegistry.enableSimpleBroker "/queue", "/topic"
        messageBrokerRegistry.setApplicationDestinationPrefixes "/app"
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/stomp")
                .setAllowedOrigins(allowedOrigin.split(","))
                .withSockJS();
    }

}
