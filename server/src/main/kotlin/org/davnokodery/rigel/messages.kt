package org.davnokodery.rigel

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.davnokodery.rigel.model.CardData
import org.davnokodery.rigel.model.GameSessionStatus
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
    JsonSubTypes.Type(value = NewCard::class, name = "new_card"),
)
sealed class ServerWsMessage(
    val id: String = UUID.randomUUID().toString(), // todo -> change to the incrementing counter (owner: GameSession)
    val date: Date = Date()
)

// common
data class GameStatusUpdate(val newStatus: GameSessionStatus) : ServerWsMessage()
data class CardPlayed(val cardId: String, val discarded: Boolean) : ServerWsMessage()
data class NewGameCreated(val gameId: String) : ServerWsMessage()
data class GameMessageUpdate(val message: String) : ServerWsMessage()
data class PlayerPropertyChange(val property: String, val delta: Int, val finalValue: Int) : ServerWsMessage()
data class GamesListResponse(val games: Collection<String>) : ServerWsMessage()
data class NewCard(val cardData: CardData) : ServerWsMessage()

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
data class JoinGameMessage(val gameId: String): ClientWsMessage()
class StartGameMessage: ClientWsMessage()
data class PlayCardMessage(val cardId: String? = null, val targetId: String? = null): ClientWsMessage()
class SkipTurnMessage: ClientWsMessage()
class GameListRequest: ClientWsMessage()
