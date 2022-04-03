package org.davnokodery.rigel.model

import org.davnokodery.rigel.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

enum class GameSessionStatus {
    Created,
    Player_1_Turn,
    Player_2_Turn,
    Player_1_Won,
    Player_2_Won,
}

data class GameSession(
    private val player1: SessionPlayer,
    private var player2: SessionPlayer,
    internal val updates: Queue<GameUpdate> = ConcurrentLinkedQueue(), // common updates
    internal var status: GameSessionStatus = GameSessionStatus.Created,
    val id: String = UUID.randomUUID().toString(),
) {
    private val logger = LoggerFactory.getLogger(GameSession::class.java)

    private fun send(update: GameUpdate) {
        updates.offer(update)
    }

    private fun changeStatus(newState: GameSessionStatus) {
        status = newState
        send(GameStatusUpdate(newState))
    }

    fun startGame() {
        if (Random().nextBoolean()) {
            changeStatus(GameSessionStatus.Player_1_Turn)
        } else {
            changeStatus(GameSessionStatus.Player_2_Turn)
        }
    }

    // todo end turn move to another function
    /**
     * Run the game logic of the user playing a card.
     * @param playerName - current player name, todo: switch to jwt,
     * @param cardId - either id of the card in the hand or the effect card id, when null - end turn,
     * @param target - id of the card to apply effect to (if applicable)
     */
    fun play(playerName: String, cardId: String? = null, target: String? = null) {

        // Validations

        // todo: change playerName to jwt validation
        if (playerName != player1.name && playerName != player2.name) {
            logger.warn("No such player: $playerName")
            return
        }

        if (status == GameSessionStatus.Created) {
            send(GameMessageUpdate("Game is not started yet"))
            return
        }

        if (status == GameSessionStatus.Player_1_Won || status == GameSessionStatus.Player_2_Won) {
            send(GameMessageUpdate("Game is finished, start a new game"))
            return
        }

        val currentPlayer = if (status == GameSessionStatus.Player_1_Turn) player1 else player2
        val enemyPlayer = if (status == GameSessionStatus.Player_2_Turn) player1 else player2

        if (playerName != currentPlayer.name) {
            send(GameMessageUpdate("It is ${currentPlayer.name}'s turn!", playerName))
            return
        }

        if (cardId != null) {

            // selected card or effect exists
            val card = currentPlayer.cards[cardId] ?: currentPlayer.effects[cardId]
            if (card == null) {
                send(GameMessageUpdate("Error! No such card!", currentPlayer.name))
                return
            }

            val targetEffect = currentPlayer.effects[target] ?: enemyPlayer.effects[target]

            if (target != null && targetEffect == null) {
                send(GameMessageUpdate("Target not found!", currentPlayer.name))
                return
            }

            val cardError = card.validator?.validate(card, currentPlayer, enemyPlayer, targetEffect)
            if (cardError != null) {
                send(GameMessageUpdate(cardError, currentPlayer.name))
                return
            }

            card.onApply?.activate(card, currentPlayer, enemyPlayer, targetEffect)
            currentPlayer.cards.remove(card.id)
            if (card.ttl > 0) {
                // if a card has a lasting effect -> move it to the current effects
                currentPlayer.effects[card.id] = card
                send(CardPlayed(card.id, false))
            } else {
                // discard otherwise
                send(CardPlayed(card.id, true))
            }
        } else {
            // end turn
            // activate current effects
            val expiredEffects = currentPlayer.effects.values.filter {
                it.onTick?.effect(it, currentPlayer, enemyPlayer)
                it.ttl--
                if (it.ttl <= 0) {
                    it.onExpire?.effect(it, currentPlayer, enemyPlayer)
                    listOf(player1, player2).forEach { p -> p.removeTemporaryPropertyChange(it.id) }
                }
                it.ttl <= 0
            }
            // remove the expired effects and sent updates
            expiredEffects.forEach {
                currentPlayer.effects.remove(it.id)
                send(CardPlayed(it.id, true))
            }
            changeStatus(if (status == GameSessionStatus.Player_1_Turn) GameSessionStatus.Player_2_Turn else GameSessionStatus.Player_1_Turn)
        }
    }

}
