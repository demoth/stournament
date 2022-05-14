package org.demoth.betelgeuse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.davnokodery.rigel.ClientWsMessage
import org.davnokodery.rigel.JwtMessage
import org.davnokodery.rigel.ServerWsMessage
import org.springframework.web.socket.*
import org.springframework.web.socket.client.standard.StandardWebSocketClient

private const val WS_URL = "ws://localhost:8080/web-socket"

class WsClient(private val jwt: String, private val handle: (ServerWsMessage) -> Unit): WebSocketHandler, AutoCloseable {


    private val mapper = jacksonObjectMapper()

    private val session = StandardWebSocketClient().doHandshake(this, WS_URL).get()!!
    override fun close() {
        session.close()
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        session.sendMessage(TextMessage(mapper.writeValueAsString(JwtMessage(jwt))))
    }

    override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
        if (message is TextMessage) {
            val serverWsMessage = mapper.readValue<ServerWsMessage>(message.payload)
            println("Received: $serverWsMessage")
            handle(serverWsMessage)
        } else {
            println("Unexpected message type: ")
        }
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

    fun send(msg: ClientWsMessage) {
        session.sendMessage(TextMessage(mapper.writeValueAsString(msg)))
    }
}
