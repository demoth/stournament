package org.davnokodery.stournament

import org.davnokodery.stournament.GameStatus.*

data class GameState(
    var status: GameStatus = PLAYER_1_TURN,
    val player1: Player = Player("Radiant"),
    val player2: Player = Player("Dire"),
    @Deprecated("Error messages")
    var message: String = ""
) {
    fun performAction(player: Int, cardId: String?) {

        val currentPlayer = if (player == 1) player1 else player2

        val currentEnemy = if (player != 1) player1 else player2

        if (status == PLAYER_1_WON || status == PLAYER_2_WON) {
            return
        }

        if (player == 1 && status != PLAYER_1_TURN || player != 1 && status != PLAYER_2_TURN) {
            message = "Not your turn"
            return
        }

        if (cardId != null && !currentPlayer.cards.containsKey(cardId)) {
            message = "No such card"
            return
        }

        // try to play selected card
        if (cardId != null) {
            val card = currentPlayer.cards[cardId]!!
            val cardError = card.validate?.invoke(currentPlayer, currentEnemy)

            if (!cardError.isNullOrBlank()) {
                val cardErrorMessage = currentPlayer.name + " cannot play " + card.name + " right now: $cardError"
                println(cardErrorMessage)
                message = cardErrorMessage
            } else {
                println(currentPlayer.name + " played " + card.name)
                currentPlayer.cards.remove(cardId)
                currentPlayer.effects.add(card)
            }

        }

        // update existing state
        currentPlayer.effects.removeIf {
            if (it.turns == 0) {
                // safety net condition, ideally cards should expire on the turn they are used
                it.expire?.invoke(currentPlayer, currentEnemy)
                true
            } else {
                it.effect.invoke(currentPlayer, currentEnemy)
                it.turns--
                if (it.turns == 0) {
                    it.expire?.invoke(currentPlayer, currentEnemy)
                    true
                } else {
                    false
                }
            }
        }

        status = updateStatus()
        logState()
    }

    private fun logState() {
        println("Player 1 : ${player1.health}/${player1.maxHealth}")
        println("Player 2 : ${player2.health}/${player2.maxHealth}")
        println("State: $status")
    }

    private fun updateStatus(): GameStatus =
        if (player1.health <= 0 || player1.cards.isEmpty())
            PLAYER_2_WON
        else if (player2.health <= 0 || player2.cards.isEmpty())
            PLAYER_1_WON
        else if (status == PLAYER_1_TURN)
            PLAYER_2_TURN
        else
            PLAYER_1_TURN

}



