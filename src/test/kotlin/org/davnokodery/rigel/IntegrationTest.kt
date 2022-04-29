package org.davnokodery.rigel

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.davnokodery.rigel.TestDataCreator.Companion.testUser1
import org.davnokodery.rigel.TestDataCreator.Companion.testUser2
import org.davnokodery.rigel.TestDataCreator.Companion.testUser3
import org.davnokodery.rigel.model.CardData
import org.davnokodery.rigel.model.GameSessionStatus
import org.davnokodery.rigel.model.GameSessionStatus.Player_1_Turn
import org.davnokodery.rigel.model.GameSessionStatus.Player_2_Turn
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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
class IntegrationTest(
    @Autowired
    val userSessionManager: UserSessionManager
) {
    @LocalServerPort
    private var port: Int = 0
    private var restTemplate = TestRestTemplate()
    private val logger = LoggerFactory.getLogger(IntegrationTest::class.java)
    private val mapper = jacksonObjectMapper()

    @AfterEach
    fun tearDown() {
        userSessionManager.reset()
    }

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
        assertEquals(tester1.currentGameStatus, tester2.currentGameStatus)
        assertTrue(tester1.currentGameStatus == Player_1_Turn || tester1.currentGameStatus == Player_2_Turn)
        assertTrue(tester1.currentGameStatus!!.started())
    }

    @Test
    fun `play card - positive`() = runBlocking {
        val tester1 = TestClient(loginPost(testUser1))
        tester1.login()

        val tester2 = TestClient(loginPost(testUser2))
        tester2.login()

        tester1.createGame()
        tester2.joinGame()
        tester1.startGame()
        
        check(tester1.currentGameStatus != null) { "Not received the state yet" }
        val currentPlayer = if (tester1.currentGameStatus == Player_1_Turn) tester1 else tester2
        val opponent = if (tester1.currentGameStatus != Player_1_Turn) tester1 else tester2
        
        assertEquals(100, opponent.properties[PROP_HEALTH])
        currentPlayer.playCard(currentPlayer.cards.values.find { it.name == FIRE_BALL_NAME })
        assertEquals(95, opponent.properties[PROP_HEALTH])
        assertNull(currentPlayer.cards.values.find { it.name == FIRE_BALL_NAME })
    }

    @Test
    fun `play card - end turn`() = runBlocking {
        val tester1 = TestClient(loginPost(testUser1))
        tester1.login()

        val tester2 = TestClient(loginPost(testUser2))
        tester2.login()

        tester1.createGame()
        tester2.joinGame()
        tester1.startGame()
        
        val currentPlayer = if (tester1.currentGameStatus == Player_1_Turn) tester1 else tester2
        val status = tester1.currentGameStatus
        val amountOfCards = currentPlayer.cards.size
        
        currentPlayer.playCard(null)
        
        // one new received
        assertEquals(amountOfCards + 1, currentPlayer.cards.size)
        // end turn
        assertNotEquals(status, tester1.currentGameStatus)
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

    @Test
    fun `gameList - get game ids`() = runBlocking {
        val tester1 = TestClient(loginPost(testUser1))
        tester1.login()
        tester1.createGame()
        assertFalse(tester1.gameIds.isEmpty())

        val tester2 = TestClient(loginPost(testUser2))
        tester2.login()

        // no games since joined after the game was created
        assertTrue(tester2.gameIds.isEmpty())
        tester2.requestGameIds()
        // should have received games by this time
        assertFalse(tester2.gameIds.isEmpty())

        // should be the same as above
        assertEquals(tester1.gameIds.first(), tester2.gameIds.first())
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

        val gameIds = hashSetOf<String>()
        var currentGameStatus: GameSessionStatus? = null
        
        val messages = arrayListOf<GameMessageUpdate>()
        val properties = mutableMapOf<String, Int>()
        val cards = mutableMapOf<String, CardData>()
        val effects = mutableMapOf<String, CardData>()
        var connected = false

        suspend fun login() {
            check(connected) { "Not connected!" }
            session.sendMessage(TextMessage(mapper.writeValueAsString(JwtMessage("Bearer $jwt"))))
            delay(200)
        }

        suspend fun createGame() {
            check(connected) { "Not connected!" }
            session.sendMessage(toJson(CreateGameMessage()))
            delay(200)
        }

        suspend fun startGame() {
            check(connected) { "Not connected!" }
            session.sendMessage(toJson(StartGameMessage()))
            delay(200)
        }

        suspend fun joinGame() {
            check(connected) { "Not connected!" }
            session.sendMessage(toJson(JoinGameMessage(gameId = gameIds.first())))
            delay(200)
        }

        suspend fun requestGameIds() {
            check(connected) { "Not connected!" }
            session.sendMessage(toJson(GameListRequest()))
            delay(200)
        }

        suspend fun playCard(cardId: CardData?) {
            check(connected) { "Not connected!" }
            session.sendMessage(toJson(PlayCardMessage(cardId?.id)))
            delay(200)
        }

        override fun afterConnectionEstablished(session: WebSocketSession) {
            logger.debug("Connection established")
            connected = true
        }

        override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
            check(connected) { "Not connected!" }
            if (message !is TextMessage) {
                logger.error("Non text message received!")
                return
            }

            val msg: ServerWsMessage = mapper.readValue(message.payload)
            logger.debug("Received (${session.id}): $msg")
            when (msg) {
                is NewGameCreated -> gameIds.add(msg.gameId)
                is GameMessageUpdate -> messages.add(msg)
                is GameStatusUpdate -> currentGameStatus = msg.newStatus
                is GamesListResponse -> gameIds.addAll(msg.games)
                is PlayerPropertyChange -> properties[msg.property] = (properties[msg.property] ?: 0) + msg.delta //todo: don't calculate on client side, receive final value
                is NewCard -> cards[msg.cardData.id] = msg.cardData
                is CardPlayed -> {
                    if (msg.discarded) {
                        cards.remove(msg.cardId)
                        effects.remove(msg.cardId)
                    } else {
                        val card = cards[msg.cardId]
                        check(card != null) { "Effect cannot be applied: no such card: ${msg.cardId}"}
                        effects[msg.cardId] = cards.remove(msg.cardId)!!
                    }
                }
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
