package org.davnokodery.rigel

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

const val WEBSOCKET_PATH = "/web-socket"

@Configuration
@EnableWebSocket
class RigelWebsocketConfig : WebSocketConfigurer {

    @Autowired lateinit var handler: UserSessionManager

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(handler, WEBSOCKET_PATH).setAllowedOrigins("*")
    }
}
