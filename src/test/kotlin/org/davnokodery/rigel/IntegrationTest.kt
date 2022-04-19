package org.davnokodery.rigel

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.davnokodery.rigel.TestDataCreator.Companion.testUser1
import org.davnokodery.rigel.model.GameSessionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.socket.*
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

    @Test
    fun `start game - not enough players`() = runBlocking {
        val loginResponse = restTemplate.postForEntity<LoginResponse>(
            "http://$url/login",
            LoginRequest(testUser1.name, testUser1.password)
        )
        assertEquals(HttpStatus.OK, loginResponse.statusCode)

        val handler = TestClient(loginResponse.body!!.jwt)
        val session = StandardWebSocketClient().doHandshake(handler, "ws://$url/web-socket").get()!!

        delay(200)
        session.sendMessage(toJson(CreateGameMessage()))

        delay(200)
        assertNotNull(handler.newGameId, "Game is not created")

        session.sendMessage(toJson(StartGameMessage()))
        delay(200)
        assertEquals("Not enough players", handler.messages.firstOrNull()?.message)
    }

    private fun toJson(msg: ClientWsMessage) = TextMessage(mapper.writeValueAsString(msg))

    inner class TestClient(val jwt: String) : WebSocketHandler {
        private val mapper = jacksonObjectMapper()

        var newGameId: String? = null
        var gameStatus: GameSessionStatus? = null

        val messages = arrayListOf<GameMessageUpdate>()

        override fun afterConnectionEstablished(session: WebSocketSession) {
            logger.debug("Connection established")
            session.sendMessage(TextMessage(mapper.writeValueAsString(JwtMessage("Bearer $jwt"))))

        }

        override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
            logger.debug("Received message: $message")
            if (message !is TextMessage)
                return

            val msg: ServerWsMessage = mapper.readValue(message.payload)
            logger.debug("Received: $msg")
            when (msg) {
                is NewGameCreated -> {
                    newGameId = msg.gameId
                }
                is GameMessageUpdate -> {
                    messages.add(msg)
                }
                is GameStatusUpdate -> {
                    gameStatus = msg.newStatus
                }
                is CardPlayed -> TODO()
                is GamesListResponse -> TODO()
                is PlayerPropertyChange -> TODO()
            }

        }

        override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
            logger.debug("handleTransportError: ", exception)
        }

        override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
            logger.debug("connection closed status=$closeStatus")
        }

        override fun supportsPartialMessages(): Boolean {
            return false
        }
    }
}
