package org.davnokodery.rigel

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.davnokodery.rigel.TestDataCreator.Companion.testUser1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.socket.*
import org.springframework.web.socket.client.WebSocketClient
import org.springframework.web.socket.client.standard.StandardWebSocketClient


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-data")
class IntegrationTest {
    @LocalServerPort
    private var port: Int = 0
    private var restTemplate = TestRestTemplate()
    private val logger = LoggerFactory.getLogger(IntegrationTest::class.java)
    private val mapper = jacksonObjectMapper()


    private val url
        get() = "localhost:$port"

    val client: WebSocketClient = StandardWebSocketClient()

    @Test
    fun `create game - successful`() {
        val loginResponse = restTemplate.postForEntity<LoginResponse>(
            "http://$url/login",
            LoginRequest(testUser1.name, testUser1.password)
        )
        assertEquals(HttpStatus.OK, loginResponse.statusCode)

        val client: WebSocketClient = StandardWebSocketClient()
        val jwt = loginResponse.body!!.jwt
        val handler: WebSocketHandler = TestClient(jwt)
        val session = client.doHandshake(handler, "ws://$url/web-socket").get()!!
        session.sendMessage(TextMessage(mapper.writeValueAsString(JwtMessage("Bearer $jwt"))))
        // todo: implement :)
    }

    inner class TestClient(val jwt: String) : WebSocketHandler {
        private val mapper = jacksonObjectMapper()
        var connected = false

        override fun afterConnectionEstablished(session: WebSocketSession) {
            logger.debug("Connection established")
        }

        override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
            logger.debug("Received message: $message")
            if (message !is TextMessage)
                return

            val msg: ClientWsMessage = mapper.readValue(message.payload)
            when (msg) {
                is CreateGameMessage -> TODO()
                is GameListRequest -> TODO()
                is JoinGameMessage -> TODO()
                is JwtMessage -> TODO()
                is PlayCardMessage -> TODO()
                is SkipTurnMessage -> TODO()
                is StartGameMessage -> TODO()
            }

        }

        override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
            logger.warn("handleTransportError: ", exception)
        }

        override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
            logger.debug("connection closed status=$closeStatus")
            session.sendMessage(TextMessage(mapper.writeValueAsString(JwtMessage(jwt))))
            connected = true
        }

        override fun supportsPartialMessages(): Boolean {
            return false
        }
    }
}
