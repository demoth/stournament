package org.davnokodery.rigel

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.davnokodery.rigel.model.GameSessionStatus
import org.davnokodery.rigel.model.PlayerProperty
import java.util.*

///////////////////////
// server messages
///////////////////////
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "_type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = GameStatusUpdate::class, name = "status"),
    JsonSubTypes.Type(value = CardPlayed::class, name = "card_played"),
    JsonSubTypes.Type(value = GameMessageUpdate::class, name = "message"),
    JsonSubTypes.Type(value = PlayerPropertyChange::class, name = "player_property"),
    JsonSubTypes.Type(value = NewGameCreated::class, name = "new_game_created"),
    JsonSubTypes.Type(value = GamesListResponse::class, name = "game_list"),
)
sealed class ServerWsMessage(
    val id: String = UUID.randomUUID().toString(), // todo -> change to the incrementing counter (owner: GameSession)
    val date: Date = Date()
)

// common
data class GameStatusUpdate(val newStatus: GameSessionStatus) : ServerWsMessage()
data class CardPlayed(val cardId: String, val discarded: Boolean) : ServerWsMessage()
data class NewGameCreated(val gameId: String) : ServerWsMessage()

// targeted
data class GameMessageUpdate(val message: String, val playerSessionId: String? = null) : ServerWsMessage()
data class PlayerPropertyChange(val playerSessionId: String, val property: PlayerProperty, val delta: Int) : ServerWsMessage()
data class GamesListResponse(val games: Collection<String>) : ServerWsMessage()

///////////////////////
// client messages
///////////////////////
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "_type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = JwtMessage::class, name = "jwt"),
    JsonSubTypes.Type(value = CreateGameMessage::class, name = "newgame"),
    JsonSubTypes.Type(value = JoinGameMessage::class, name = "joingame"),
    JsonSubTypes.Type(value = StartGameMessage::class, name = "startgame"),
    JsonSubTypes.Type(value = PlayCardMessage::class, name = "playcard"),
    JsonSubTypes.Type(value = SkipTurnMessage::class, name = "skipturn"),
    JsonSubTypes.Type(value = GameListRequest::class, name = "game_list"),
)
sealed class ClientWsMessage {
}

data class JwtMessage(val jwt: String) : ClientWsMessage()
class CreateGameMessage : ClientWsMessage()
class JoinGameMessage(val gameId: String): ClientWsMessage()
class StartGameMessage: ClientWsMessage()
class PlayCardMessage(val cardId: String, val targetId: String): ClientWsMessage()
class SkipTurnMessage: ClientWsMessage()
class GameListRequest: ClientWsMessage()
