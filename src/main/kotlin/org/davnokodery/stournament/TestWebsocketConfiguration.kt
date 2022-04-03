package org.davnokodery.stournament

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableScheduling
@EnableWebSocketMessageBroker
class TestWebsocketConfiguration: WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        super.configureMessageBroker(registry)
        registry.enableSimpleBroker("/topic")   // outgoing
        registry.setApplicationDestinationPrefixes("/app")      // incoming
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        super.registerStompEndpoints(registry)
        registry.addEndpoint("/gs-guide-websocket").setAllowedOriginPatterns("*").withSockJS()
    }
}
