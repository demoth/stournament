package org.davnokodery.stournament

import org.davnokodery.stournament.GameStatus.*
import java.util.*

data class GameState(
    var status: GameStatus = PLAYER_1_TURN,
    val player1: Player = Player(),
    val player2: Player = Player()
) {
    fun performAction(player: Int, cardId: String) {
        val currentPlayer = if (player == 1) player1 else player2
        val currentEnemy = if (player != 1) player1 else player2

        if (player == 1 && status != PLAYER_1_TURN || player != 1 && status != PLAYER_2_TURN)
            throw IllegalStateException("Not your turn")

        if (status == PLAYER_1_WON || status == PLAYER_2_WON)
            throw IllegalStateException("Game is over")

        val card = currentPlayer.cards.find { it.id == cardId } ?: throw IllegalArgumentException("No such card")
        when (card.action) {
            CardAction.FIREBALL -> currentEnemy.health -= 5
            CardAction.HEALING -> currentPlayer.health += 5
        }
        currentPlayer.cards.removeIf { it.id == cardId }

        updateStatus()
    }

    private fun updateStatus() {
        status =
            if (player1.health <= 0)
                PLAYER_2_WON
            else if (player2.health <= 0)
                PLAYER_1_WON
            else if (status == PLAYER_1_TURN)
                PLAYER_2_TURN
            else
                PLAYER_1_TURN
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
    val cards: MutableList<Card> = mutableListOf(
        fireball(),
        fireball(),
        fireball(),
        fireball(),
        fireball(),
        fireball(),
        healing()
    )
)

fun fireball() = Card(
    "Fireball",
    "SpellBook01_84.PNG",
    CardAction.FIREBALL
)

fun healing() = Card(
    "Healing",
    "SpellBook08_118.PNG",
    CardAction.HEALING
)

data class Card(
    val name: String,
    val iconName: String,
    val action: CardAction,
    val id: String = UUID.randomUUID().toString()
)

enum class CardAction {
    FIREBALL,
    HEALING
}

data class ClientAction(val cardId: String)
