package org.davnokodery.rigel

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.davnokodery.rigel.model.GameSessionStatus
import org.davnokodery.rigel.model.PlayerProperty
import java.util.*

///////////////////////
// server messages
///////////////////////
sealed class GameUpdate(
    val id: String = UUID.randomUUID().toString(), // todo -> change to the incrementing counter (owner: GameSession)
    val date: Date = Date()
)

// common
data class GameStatusUpdate(val newStatus: GameSessionStatus) : GameUpdate()
data class CardPlayed(val cardId: String, val discarded: Boolean) : GameUpdate()

// targeted
data class GameMessageUpdate(val message: String, val playerName: String? = null) : GameUpdate()
data class PlayerPropertyChange(val playerName: String, val property: PlayerProperty, val delta: Int) :
    GameUpdate(playerName)


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
    JsonSubTypes.Type(value = CreateGameMessage::class, name = "newgame")
)
abstract class RigelWsMessage {
}

data class JwtMessage(val jwt: String) : RigelWsMessage()

class CreateGameMessage : RigelWsMessage()
class JoinGameMessage(val gameId: String): RigelWsMessage()
class StartGameMessage: RigelWsMessage()
