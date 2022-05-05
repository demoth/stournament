package org.davnokodery.rigel

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.davnokodery.rigel.model.GameSession
import org.davnokodery.rigel.model.GameSessionStatus
import org.davnokodery.rigel.model.SessionPlayer
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class UserSession(
    val session: WebSocketSession,
    var user: User? = null
)

interface MessageSender {
    fun unicast(message: ServerWsMessage, receiver: String)

    fun broadcast(message: ServerWsMessage)
}

private const val WS_ID_TAG = "WID"
private const val USER_ID_TAG = "UID"
private const val GAME_ID_TAG = "GID"

@Component
class UserSessionManager(
    @Autowired val authService: AuthService
) : TextWebSocketHandler() {

    private val logger = LoggerFactory.getLogger(UserSessionManager::class.java)
    private val sessions: MutableMap<String, UserSession> = ConcurrentHashMap()
    private val games: MutableMap<String, GameSession> = ConcurrentHashMap()
    private val mapper = jacksonObjectMapper()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        logger.debug("Handshake headers: ${session.handshakeHeaders}")
        logger.info("Connected ${session.id}")
        sessions[session.id] = UserSession(session)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        logger.info("Disconnected ${sessions[session.id]?.user?.name} (${session.id})")
        sessions.remove(session.id)
        // todo: leave and stop running games related to this user
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        try {
            MDC.put(WS_ID_TAG, session.id)
            logger.debug("Received message: ${message.payload}")
            val userSession = sessions[session.id]
            if (userSession == null) {
                logger.error("Received message before connection is established")
                session.close()
                return
            }
            MDC.put(USER_ID_TAG, userSession.user?.name)

            when (val request: ClientWsMessage = mapper.readValue(message.payload)) {
                is JwtMessage -> validateJwtAuth(session, request, userSession)
                is CreateGameMessage -> createGameSession(session, userSession)
                is StartGameMessage -> startGameSession(session)
                is JoinGameMessage -> joinGameSession(session, request, userSession)
                is PlayCardMessage -> playCard(session, request)
                is GameListRequest -> {
                    userSession.session.sendMessage(toJson(GamesListResponse(games.keys.toList())))
                }
                is EndTurnMessage -> endTurn(session)
            }
        } catch (e: Exception) {
            logger.error("Could not process message: '${message.payload}'", e)
            session.close()
        } finally {
            MDC.clear()
        }
    }

    private fun playCard(session: WebSocketSession, request: PlayCardMessage) {
        val gameSession = findGameByPlayerId(session.id)
        if (gameSession == null) {
            logger.warn("Not in game")
            session.sendMessage(toJson(GameMessageUpdate("Not in game")))
            return
        }
        MDC.put(GAME_ID_TAG, gameSession.id)
        gameSession.play(session.id, request.cardId, request.targetId)
        logger.debug("Played card: ${request.cardId}, target: ${request.targetId}")
    }

    private fun endTurn(session: WebSocketSession) {
        val gameSession = findGameByPlayerId(session.id)
        if (gameSession == null) {
            logger.warn("Not in game")
            session.sendMessage(toJson(GameMessageUpdate("Not in game")))
            return
        }
        MDC.put(GAME_ID_TAG, gameSession.id)
        gameSession.endTurn(session.id)
    }

    private fun joinGameSession(session: WebSocketSession, request: JoinGameMessage, userSession: UserSession) {
        val gameSession = games[request.gameId]
        if (gameSession == null) {
            logger.warn("No such game ${request.gameId}")
            session.sendMessage(toJson(GameMessageUpdate("No such game ${request.gameId}")))
            return
        }
        MDC.put(GAME_ID_TAG, gameSession.id)
        if (gameSession.player2 == null) {
            gameSession.player2 = SessionPlayer(
                sessionId = session.id,
                name = userSession.user!!.name,
                sender = createMessageSender(gameSession.id)
            )
            logger.debug("Joined")
        } else {
            logger.debug("Game is full")
            session.sendMessage(toJson(GameMessageUpdate("Game is full")))
        }
    }

    /**
     * Start a game session that was previously created
     */
    private fun startGameSession(session: WebSocketSession) {
        val gameSession = findGameByPlayerId(session.id)
        if (gameSession == null) {
            logger.warn("No game created")
            session.sendMessage(toJson(GameMessageUpdate("No game created")))
            return
        }
        MDC.put(GAME_ID_TAG, gameSession.id)
        if (gameSession.player2 != null) {
            if (gameSession.status == GameSessionStatus.Created) {
                gameSession.startGame()
                logger.debug("Game started")
            } else {
                logger.warn("Game not in Created state ${gameSession.status}")
                session.sendMessage(toJson(GameMessageUpdate("Game not in Created state")))
            }
        } else {
            logger.debug("Not enough players")
            session.sendMessage(toJson(GameMessageUpdate("Not enough players")))
        }
        return
    }

    /**
     * Create a new game session
     * todo: check that there is no running game with this user
     */
    private fun createGameSession(session: WebSocketSession, userSession: UserSession) {
        val gameId = UUID.randomUUID().toString()
        val newGameSession = GameSession(
            id = gameId,
            player1 = SessionPlayer(
                sessionId = session.id,
                name = userSession.user!!.name,
                sender = createMessageSender(gameId)),
            sender = createMessageSender(gameId),
            gameRules = provingGroundsRules() // todo: make configurable
        )
        games[gameId] = newGameSession
        MDC.put(GAME_ID_TAG, gameId)
        logger.debug("Created new game")
        sessions.values.forEach {
            it.session.sendMessage(toJson(NewGameCreated(gameId)))
        }
    }

    private fun createMessageSender(gameId: String): MessageSender {
        return object: MessageSender {
            override fun unicast(message: ServerWsMessage, receiver: String) {
                sessions[receiver]?.session?.sendMessage(toJson(message))
            }

            override fun broadcast(message: ServerWsMessage) {
                val gameSession = games[gameId]
                check(gameSession != null) { "Game session does not exist $gameId" }
                sessions[gameSession.player1.sessionId]?.session?.sendMessage(toJson(message))
                sessions[gameSession.player2?.sessionId]?.session?.sendMessage(toJson(message))
            }
        }
    }

    /**
     * Validate jwt message and initialize the websocket connection. Disconnect for any problem.
     */
    private fun validateJwtAuth(session: WebSocketSession, request: JwtMessage, userSession: UserSession) {
        if (userSession.user == null) {
            val user = authService.validateToken(request.jwt)
            userSession.user = user
            MDC.put(USER_ID_TAG, user.name)
            // drop other active sessions for the same user
            sessions.values.forEach {
                if (it.user != null
                    && it.user?.name == user.name
                    && it.session.id != userSession.session.id
                ) {
                    logger.debug("Dropping existing session ${it.session.id}")
                    it.session.close()
                }
            }
        } else {
            logger.warn("Unexpected JwtMessage")
        }
    }

    private fun findGameByPlayerId(sessionId: String) = games.values.firstOrNull {
        sessionId == it.player1.sessionId || sessionId == it.player2?.sessionId
    }

    private fun toJson(it: ServerWsMessage) = TextMessage(mapper.writeValueAsString(it))

    fun reset() {
        sessions.values.forEach { it.session.close() }
        sessions.clear()
        games.clear()
    }
}
