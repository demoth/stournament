package org.demoth.betelgeuse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.davnokodery.rigel.CreateGameMessage
import org.davnokodery.rigel.JwtMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient

class WsClient: WebSocketHandler {
    private val mapper = jacksonObjectMapper()
    var jwt: String = ""

    private val session = StandardWebSocketClient().doHandshake(this, "ws://localhost:8080/web-socket").get()!!

    override fun afterConnectionEstablished(session: WebSocketSession) {
        println("afterConnectionEstablished")
    }

    override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
        println("Received: $message")
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        println("handleTransportError")
    }

    override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
        println("afterConnectionClosed")
    }

    override fun supportsPartialMessages(): Boolean {
        println("supportsPartialMessages")
        return false
    }

    fun login() {
        session.sendMessage(TextMessage(mapper.writeValueAsString(JwtMessage(jwt))))
    }

    fun startGame() {
        session.sendMessage(TextMessage(mapper.writeValueAsString(CreateGameMessage())))
    }
}
