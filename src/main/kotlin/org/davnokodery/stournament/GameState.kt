package org.davnokodery.stournament

import java.util.*

data class GameState (
    val player1:Player = Player(),
    val player2:Player = Player(),
    var status: GameStatus = GameStatus.PLAYER_1_TURN
)

enum class GameStatus {
    PLAYER_1_TURN,
    PLAYER_2_TURN,
    PLAYER_1_WON,
    PLAYER_2_WON
}

data class Player(
    var health: Int = 20,
    val action: List<Card> = mutableListOf(fireball(), healing())
)

fun fireball() = Card(
    "Lava Axe",
    "SpellBook01_84.PNG",
    CardAction.FIREBALL
)

fun healing() = Card(
    "Healing",
    "SpellBook08_118.PNG",
    CardAction.APPLE
)

data class Card(
    val name: String,
    val iconName: String,
    val action: CardAction,
    val id: String = UUID.randomUUID().toString()
)

enum class CardAction {
    FIREBALL,
    APPLE
}

data class ClientAction (val cardId: String)
