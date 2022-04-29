package org.davnokodery.rigel.model

import org.davnokodery.rigel.*
import org.slf4j.LoggerFactory
import java.util.*

enum class GameSessionStatus {
    Created,
    Player_1_Turn,
    Player_2_Turn,
    Player_1_Won,
    Player_2_Won;

    fun started(): Boolean {
        return this == Player_1_Turn || this == Player_2_Turn
    }
    
    fun finished(): Boolean {
        return this == Player_1_Won || this == Player_2_Won
    }
}

interface GameRules {

    /**
     * Initialize players: add required properties and starting cards.
     */
    fun onGameStarted(player: SessionPlayer, enemyPlayer: SessionPlayer, gameSession: GameSession)

    fun afterCardPlayed(card: Card, player: SessionPlayer, enemyPlayer: SessionPlayer, gameSession: GameSession)

    /**
     * Place common checks before the card is going to be played.
     * All technical checks are passed till this point, but the card can still be rejected by the card validation logic.
     * Therefore, don't change the card or players, because the card may not be played.
     * Information messages can be sent to the player through the player.sender
     * @return false if card should not be played.
     */
    fun beforeCardPlayed(card: Card, player: SessionPlayer, enemyPlayer: SessionPlayer, gameSession: GameSession): Boolean

    /**
     * Called just before the turn is changed. All effects and expiration logic is already executed.
     */
    fun onEndTurn(player: SessionPlayer, enemyPlayer: SessionPlayer, gameSession: GameSession)
}

data class GameSession(
    val id: String,
    val player1: SessionPlayer,
    internal val sender: MessageSender,
    internal val gameRules: GameRules,
    internal var status: GameSessionStatus = GameSessionStatus.Created
) {
    private val logger = LoggerFactory.getLogger(GameSession::class.java)

    var player2: SessionPlayer? = null

    private fun send(update: ServerWsMessage) {
        sender.send(update)
    }

    private fun changeStatus(newState: GameSessionStatus) {
        status = newState
        send(GameStatusUpdate(newState))
    }

    fun startGame() {
        checkNotNull(player2)
        gameRules.onGameStarted(player1, player2!!, this)
        if (Random().nextBoolean()) {
            changeStatus(GameSessionStatus.Player_1_Turn)
        } else {
            changeStatus(GameSessionStatus.Player_2_Turn)
        }
    }

    // todo end turn move to another function
    /**
     * Run the game logic of the user playing a card.
     * @param playerSessionId - current player session id,
     * @param cardId - either id of the card in the hand or the effect card id, when null - end turn,
     * @param target - id of the card to apply effect to (if applicable)
     */
    fun play(playerSessionId: String, cardId: String? = null, target: String? = null) {
        if (status == GameSessionStatus.Created) {
            send(GameMessageUpdate("Game is not started yet"))
            return
        }

        if (status == GameSessionStatus.Player_1_Won || status == GameSessionStatus.Player_2_Won) {
            send(GameMessageUpdate("Game is finished, start a new game"))
            return
        }

        // Validations
        val secondPlayer = player2
        check(secondPlayer != null) { "Player 2 is not initialized (Game was not started?)" }

        if (playerSessionId != player1.sessionId && playerSessionId != secondPlayer.sessionId) {
            logger.warn("No such player: $playerSessionId")
            return
        }


        val currentPlayer = if (status == GameSessionStatus.Player_1_Turn) player1 else secondPlayer
        val enemyPlayer = if (status == GameSessionStatus.Player_2_Turn) player1 else secondPlayer

        if (playerSessionId != currentPlayer.sessionId) {
            send(GameMessageUpdate("It is ${currentPlayer.name}'s turn!", playerSessionId))
            return
        }

        if (cardId != null) {

            // selected card or effect exists
            val card = currentPlayer.findCardById(cardId) ?: currentPlayer.findEffectById(cardId)
            if (card == null) {
                send(GameMessageUpdate("Error! No such card!", currentPlayer.sessionId))
                return
            }

            val targetEffect = if (target != null) {
                currentPlayer.findEffectById(target) ?: enemyPlayer.findEffectById(target)
            } else null

            if (target != null && targetEffect == null) {
                send(GameMessageUpdate("Target not found!", currentPlayer.sessionId))
                return
            }

            val cardError = card.validator?.validate(card, currentPlayer, enemyPlayer, targetEffect)
            if (cardError != null) {
                send(GameMessageUpdate(cardError, currentPlayer.sessionId))
                return
            }

            if (gameRules.beforeCardPlayed(card, currentPlayer, enemyPlayer, this)) {
                card.onApply?.activate(card, currentPlayer, enemyPlayer, targetEffect)
                
                currentPlayer.cardPlayed(card)
                
                gameRules.afterCardPlayed(card, currentPlayer, enemyPlayer, this)
            }
        } else {
            // end turn
            // activate current effects
            val expiredEffects = currentPlayer.updateEffects(currentPlayer, enemyPlayer)
            // remove the expired effects and sent updates
            expiredEffects.forEach {
                currentPlayer.removeEffect(it.id)
            }
            gameRules.onEndTurn(currentPlayer, enemyPlayer, this)
            changeStatus(if (status == GameSessionStatus.Player_1_Turn) GameSessionStatus.Player_2_Turn else GameSessionStatus.Player_1_Turn)
        }
    }

}
