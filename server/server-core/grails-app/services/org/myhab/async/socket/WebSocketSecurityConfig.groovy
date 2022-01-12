package org.myhab.async.socket

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer

@Configuration
class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages.anyMessage().authenticated();
        messages.nullDestMatcher().permitAll()
                .simpSubscribeDestMatchers("/user/queue/errors").permitAll()
                .simpDestMatchers("/app/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                .simpSubscribeDestMatchers("/user/**", "/topic/**").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
    }

    @Override
    protected boolean sameOriginDisabled() {
        return true;
    }


}