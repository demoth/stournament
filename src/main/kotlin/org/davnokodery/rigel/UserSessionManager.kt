package org.davnokodery.rigel

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.davnokodery.rigel.model.GameSession
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

@Component
class UserSessionManager(
    @Autowired val authService: AuthService
): TextWebSocketHandler(){

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
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        logger.debug("Received message from ${session.id}: ${message.payload}")
        val userSession = sessions[session.id]
        if (userSession == null) {
            logger.error("Received message before connection is established!")
            return
        }

        val response: RigelWsMessage = try {
            mapper.readValue(message.payload)
        } catch (e: Exception) {
            logger.error("Could not parse message from session=${session.id}, message='${message.payload}'")
            session.close()
            return
        }

        // check user authentication
        if (userSession.user == null) {
            if (response is JwtMessage) {
                try {
                    userSession.user = authService.validateToken(response.jwt)
                } catch (e: Exception) {
                    logger.error("Could not prove login: ", e)
                    session.close()
                }
            } else {
                logger.error("First message is not jwt for session: ${session.id}")
                session.close()
            }
        }

        when (response) {
            is CreateGameMessage -> {
                // todo: check that there is no running game
                // create new game
                //val newGameSession = GameSession()
            }
            is StartGameMessage -> {

            }
            is JoinGameMessage -> {

            }
        }
    }
}
