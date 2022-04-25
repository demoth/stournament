package org.davnokodery.rigel

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.davnokodery.rigel.model.GameSession
import org.davnokodery.rigel.model.SessionPlayer
import org.slf4j.LoggerFactory
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

fun interface MessageSender {
    fun send(message: ServerWsMessage)
}

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
        logger.debug("Received message from ${session.id}: ${message.payload}")
        val userSession = sessions[session.id]
        if (userSession == null) {
            logger.error("Received message before connection is established!")
            session.close()
            return
        }

        val request: ClientWsMessage = try {
            mapper.readValue(message.payload)
        } catch (e: Exception) {
            logger.error("Could not parse message from session=${session.id}, message='${message.payload}'", e)
            session.close()
            return
        }

        when (request) {
            is JwtMessage -> {
                // check user authentication
                if (userSession.user == null) {
                    try {
                        val user = authService.validateToken(request.jwt)
                        userSession.user = user

                        // drop other active sessions for the same user
                        sessions.values.forEach {
                            if (it.user != null
                                && it.user?.name == user.name
                                && it.session.id != userSession.session.id
                            ) {
                                logger.debug("Dropping existing session ${it.session.id} for user ${user.name}")
                                it.session.close()
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Could not prove login: ", e)
                        session.close()
                        return
                    }
                } else {
                    logger.warn("Unexpected JwtMessage for user: ${session.id}")
                }
            }
            is CreateGameMessage -> {
                // todo: check that there is no running game
                // create new game
                val gameId = UUID.randomUUID().toString()
                val newGameSession = GameSession(
                    id = gameId,
                    player1 = SessionPlayer(
                        sessionId = session.id,
                        name = userSession.user!!.name,
                        sender = {
                            userSession.session.sendMessage(toJson(it))
                        }),
                    sender = {
                        val gameSession = games[gameId]!!
                        // broadcast message
                        when {
                            it is GameStatusUpdate
                                    || it is CardPlayed
                                    || it is GameMessageUpdate && it.playerSessionId == null -> {
                                sessions[gameSession.player1.sessionId]?.session?.sendMessage(toJson(it))
                                sessions[gameSession.player2!!.sessionId]?.session?.sendMessage(toJson(it))
                            }
                            it is PlayerPropertyChange -> {
                                // unicast message
                                sessions[it.playerSessionId]?.session?.sendMessage(toJson(it))
                            }
                            it is GameMessageUpdate && it.playerSessionId != null -> {
                                // unicast message
                                sessions[it.playerSessionId]?.session?.sendMessage(toJson(it))
                            }
                        }
                    }
                )
                games[gameId] = newGameSession
                logger.debug("Created new game $gameId")
                sessions.values.forEach {
                    it.session.sendMessage(toJson(NewGameCreated(gameId)))
                }

            }
            is StartGameMessage -> {
                val gameSession = findGameByPlayerId(session.id)
                if (gameSession == null) {
                    logger.warn("No such game") // todo: send error response
                    return
                }
                if (gameSession.player2 != null) {
                    // todo: check if game is not started yet
                    gameSession.startGame()
                    logger.debug("Game started ${gameSession.id}")
                } else {
                    session.sendMessage(toJson(GameMessageUpdate("Not enough players")))
                }

            }
            is JoinGameMessage -> {
                val gameSession = games[request.gameId]
                if (gameSession == null) {
                    logger.warn("No such game") // todo: send error response
                    return
                }
                if (gameSession.player2 == null) {
                    gameSession.player2 = SessionPlayer(
                        sessionId = session.id,
                        name = userSession.user!!.name,
                        sender = {
                            userSession.session.sendMessage(TextMessage(mapper.writeValueAsString(it)))
                        }
                    )
                    logger.debug("Joined player: ${userSession.user!!.name}, game id: ${gameSession.id}")
                } else {
                    session.sendMessage(toJson(GameMessageUpdate("Game is full")))
                }
            }
            is PlayCardMessage -> {
                val gameSession = findGameByPlayerId(session.id)
                if (gameSession == null) {
                    logger.warn("No such game") // todo: send error response
                    return
                }
                gameSession.play(session.id, request.cardId, request.targetId)
                logger.debug("Played card: ${request.cardId}, target: ${request.targetId}, user: ${userSession.user!!.name}")
            }
            is GameListRequest -> {
                userSession.session.sendMessage(toJson(GamesListResponse(games.keys.toList())))
            }
            is SkipTurnMessage -> TODO()
        }
    }

    private fun findGameByPlayerId(sessionId: String) = games.values.firstOrNull {
        sessionId == it.player1.sessionId || sessionId == it.player2?.sessionId
    }

    private fun toJson(it: ServerWsMessage) = TextMessage(mapper.writeValueAsString(it))
}
