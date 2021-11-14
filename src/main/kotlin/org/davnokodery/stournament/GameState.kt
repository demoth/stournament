package org.davnokodery.stournament

import com.fasterxml.jackson.annotation.JsonIgnore
import org.davnokodery.stournament.GameStatus.*
import java.lang.Math.min
import java.util.*

data class GameState(
    var status: GameStatus = PLAYER_1_TURN,
    val player1: Player = Player("Player 1"),
    val player2: Player = Player("Player 2")
) {
    fun performAction(player: Int, cardId: String) {
        val currentPlayer = if (player == 1) player1 else player2
        val currentEnemy = if (player != 1) player1 else player2

        if (status == PLAYER_1_WON || status == PLAYER_2_WON)
            throw GameException("Game is over")

        if (player == 1 && status != PLAYER_1_TURN || player != 1 && status != PLAYER_2_TURN)
            throw GameException("Not your turn")

        val card = currentPlayer.cards.find { it.id == cardId } ?: throw GameException("No such card")

        val successful = card.action.invoke(currentPlayer, currentEnemy)
        if (successful) {
            println(currentPlayer.name + " played " + card.name)
            currentPlayer.cards.removeIf { it.id == cardId }
            endTurn()
            logState()
        } else {
            println(currentPlayer.name + " cannot play " + card.name + " right now")
        }
    }

    private fun logState() {
        println("Player 1 : ${player1.health}/20")
        println("Player 2 : ${player2.health}/20")
        println("State: $status")
    }

    private fun endTurn() {
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
    val name: String,
    var health: Int = 20,
    var maxHealth: Int = 30,
    val cards: MutableList<Card> = (0..7).map { if (Random().nextInt() % 2 == 0) fireball() else healing() }
        .toMutableList()
)

data class Card(
    val name: String,
    val iconName: String,
    // returns if the card was played
    @JsonIgnore val action: (self: Player, target: Player) -> Boolean,
    val id: String = UUID.randomUUID().toString()
)

fun fireball() = Card("Fireball",
    "SpellBook01_84.PNG",
    { self, target ->
        target.health -= 10
        true
    }
)

fun healing() = Card(
    "Healing",
    "SpellBook08_118.PNG",
    { self, target ->
        if (self.health >= self.maxHealth)
            false
        else {
            self.health = min(self.maxHealth, self.health + 10)
            true
        }
    }
)

data class ErrorResponse(
    val message: String
)
