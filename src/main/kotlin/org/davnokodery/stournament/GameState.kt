package org.davnokodery.stournament

import com.fasterxml.jackson.annotation.JsonIgnore
import org.davnokodery.stournament.GameStatus.*
import java.lang.Math.min
import java.util.*

data class GameState(
    var status: GameStatus = PLAYER_1_TURN,
    val player1: Player = Player("Radiant"),
    val player2: Player = Player("Dire"),
    var message: String = ""
) {
    fun performAction(player: Int, cardId: String): Boolean {
        val currentPlayer = if (player == 1) player1 else player2
        val currentEnemy = if (player != 1) player1 else player2

        message = if (status == PLAYER_1_WON || status == PLAYER_2_WON) {
            return true
        } else if (player == 1 && status != PLAYER_1_TURN || player != 1 && status != PLAYER_2_TURN) {
            "Not your turn"
        } else if (currentPlayer.cards.find { it.id == cardId } == null) {
            "No such card"
        } else {
            val card = currentPlayer.cards.find { it.id == cardId }!!
            val result = card.action.invoke(currentPlayer, currentEnemy)
            if (result.isBlank()) {
                println(currentPlayer.name + " played " + card.name)
                currentPlayer.cards.removeIf { it.id == cardId }
                status = updateStatus()
                logState()

                when (status) {
                    PLAYER_1_WON -> {
                        getWinnerMessage(player1.name)
                    }
                    PLAYER_2_WON -> {
                        getWinnerMessage(player2.name)
                    }
                    else -> ""
                }

            } else {
                val cardError = currentPlayer.name + " cannot play " + card.name + " right now: $result"
                println(cardError)
                cardError
            }
        }
        return false
    }

    private fun getWinnerMessage(winner: String): String = "$winner has won! Click again to restart"

    private fun logState() {
        println("Player 1 : ${player1.health}/20")
        println("Player 2 : ${player2.health}/20")
        println("State: $status")
    }

    private fun updateStatus(): GameStatus =
            if (player1.health <= 0)
                PLAYER_2_WON
            else if (player2.health <= 0)
                PLAYER_1_WON
            else if (status == PLAYER_1_TURN)
                PLAYER_2_TURN
            else
                PLAYER_1_TURN

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
    val description: String,
    // returns if the card was played
    @JsonIgnore val action: (self: Player, target: Player) -> String,
    val id: String = UUID.randomUUID().toString()
)

fun fireball() = Card("Fireball",
    "SpellBook01_84.PNG",
    "Deals 10 damage to the opponent",
    { self, target ->
        target.health -= 10
        ""
    }
)

fun healing() = Card(
    "Healing",
    "SpellBook08_118.PNG",
    "Heals 10 points",
    { self, target ->
        if (self.health >= self.maxHealth)
            "Already at full health"
        else {
            self.health = min(self.maxHealth, self.health + 10)
            ""
        }
    }
)

data class ErrorResponse(
    val message: String
)
