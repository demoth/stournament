package org.davnokodery.stournament

import java.util.*

data class GameState (
    val player1:Player = Player(),
    val player2:Player = Player(),
    var status: GameStatus = GameStatus.PLAYER_1_TURN
) {
    fun performAction(player: Int, cardId: String) {
        val currentPlayer = if (player == 1) player1 else player2
        val currentEnemy = if (player != 1) player1 else player2
        val card = currentPlayer.cards.find { it.id == cardId } ?: throw IllegalArgumentException("No such card")
        when (card.action) {
            CardAction.FIREBALL -> currentEnemy.health -= 5
            CardAction.APPLE -> currentPlayer.health += 5
        }
        currentPlayer.cards.removeIf { it.id == cardId }
    }
}

enum class GameStatus {
    PLAYER_1_TURN,
    PLAYER_2_TURN,
    PLAYER_1_WON,
    PLAYER_2_WON
}

data class Player(
    var health: Int = 20,
    val cards: MutableList<Card> = mutableListOf(fireball(), healing())
)

fun fireball() = Card(
    "Fireball",
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
