package org.davnokodery.rigel

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.davnokodery.rigel.TestDataCreator.Companion.testUser1
import org.davnokodery.rigel.TestDataCreator.Companion.testUser2
import org.davnokodery.rigel.TestDataCreator.Companion.testUser3
import org.davnokodery.rigel.model.GameSessionStatus
import org.davnokodery.rigel.model.GameSessionStatus.Player_1_Turn
import org.davnokodery.rigel.model.GameSessionStatus.Player_2_Turn
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    @Test
    fun `start game - not enough players`() = runBlocking {
        val testClient = TestClient(loginPost(testUser1))
        testClient.login()
        testClient.createGame()
        testClient.startGame()
        assertEquals("Not enough players", testClient.messages.firstOrNull()?.message)
    }

    @Test
    fun `start game - game is full`() = runBlocking {
        val tester1 = TestClient(loginPost(testUser1))
        tester1.login()

        val tester2 = TestClient(loginPost(testUser2))
        tester2.login()

        val tester3 = TestClient(loginPost(testUser3))
        tester3.login()

        tester1.createGame()
        tester2.joinGame()
        tester3.joinGame() // fail

        assertEquals("Game is full", tester3.messages.firstOrNull()?.message)
    }

    @Test
    fun `start game - positive`() = runBlocking {
        val tester1 = TestClient(loginPost(testUser1))
        tester1.login()

        val tester2 = TestClient(loginPost(testUser2))
        tester2.login()

        tester1.createGame()
        tester2.joinGame()
        tester1.startGame()
        assertEquals(tester1.gameStatus, tester2.gameStatus)
        assertTrue(tester1.gameStatus == Player_1_Turn || tester1.gameStatus == Player_2_Turn)
    }

    @Test
    fun `relogin - previous connections are dropped`() = runBlocking {
        val user = TestClient(loginPost(testUser1))
        user.login()

        val sameUser = TestClient(loginPost(testUser1))
        sameUser.login()

        assertTrue(sameUser.connected)
        assertFalse(user.connected)

        val ex = assertThrows<IllegalStateException> {
            user.createGame()
        }

        assertEquals("Not connected!", ex.message)
    }

    private val url
        get() = "localhost:$port"

    private fun loginPost(user: User): String {
        val loginResponse = restTemplate.postForEntity<LoginResponse>(
            "http://$url/login",
            LoginRequest(user.name, user.password)
        )
        assertEquals(HttpStatus.OK, loginResponse.statusCode, "Could not login")
        return loginResponse.body!!.jwt
    }

    private fun toJson(msg: ClientWsMessage) = TextMessage(mapper.writeValueAsString(msg))

    inner class TestClient(private val jwt: String) : WebSocketHandler {
        private val mapper = jacksonObjectMapper()
        private val session = StandardWebSocketClient().doHandshake(this, "ws://$url/web-socket").get()!!
        private var newGameId: String? = null

        var gameStatus: GameSessionStatus? = null
        val messages = arrayListOf<GameMessageUpdate>()

        var connected = false

        suspend fun login() {
            check(connected) { "Not connected!" }
            session.sendMessage(TextMessage(mapper.writeValueAsString(JwtMessage("Bearer $jwt"))))
            delay(100)
        }

        suspend fun createGame() {
            check(connected) { "Not connected!" }
            session.sendMessage(toJson(CreateGameMessage()))
            delay(100)
        }

        suspend fun startGame() {
            check(connected) { "Not connected!" }
            session.sendMessage(toJson(StartGameMessage()))
            delay(100)
        }


        suspend fun joinGame() {
            check(connected) { "Not connected!" }
            session.sendMessage(toJson(JoinGameMessage(gameId = newGameId!!)))
            delay(100)
        }

        override fun afterConnectionEstablished(session: WebSocketSession) {
            logger.debug("Connection established")
            connected = true
        }

        override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
            check(connected) { "Not connected!" }
            logger.debug("Received message: $message")
            if (message !is TextMessage) {
                logger.error("Non text message received!")
                return
            }

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
            connected = false
        }

        override fun supportsPartialMessages() = false
    }
}
