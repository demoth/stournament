package org.davnokodery.stournament

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*

data class Player(
    val name: String,
    var health: Int = 20,
    var maxHealth: Int = 30,
    val cards: MutableMap<String, Card> = createRandomCards(),
    val effects: Queue<Card> = LinkedList()
)

data class Card(
    val name: String,
    val iconName: String,
    var description: String,
    // returns if the card was played
    @JsonIgnore var effect: (self: Player, target: Player) -> Unit,
    @JsonIgnore val validate:  ((self: Player, target: Player) -> String)? = null,
    @JsonIgnore val expire: ((self: Player, target: Player) -> Unit)? = null,
    @JsonIgnore val params: MutableMap<String, Int> = mutableMapOf(),
    var turns: Int = 1,
    val id: String = UUID.randomUUID().toString()
) {
    fun getasdf(): Int = 4
}

data class ErrorResponse(val message: String)

enum class GameStatus {
    PLAYER_1_TURN,
    PLAYER_2_TURN,
    PLAYER_1_WON,
    PLAYER_2_WON
}

private fun createRandomCards() = (0..7)
    .map { if (Random().nextInt() % 2 == 0) fireball() else healing() }
    .associateBy { it.id }
    .toMutableMap()

