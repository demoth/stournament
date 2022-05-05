package org.davnokodery.rigel.model

import org.davnokodery.rigel.GameMessageUpdate
import org.davnokodery.rigel.GameStatusUpdate
import org.davnokodery.rigel.MessageSender
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
    var status: GameSessionStatus = GameSessionStatus.Created
) {
    private val logger = LoggerFactory.getLogger(GameSession::class.java)

    var player2: SessionPlayer? = null

    fun changeStatus(newState: GameSessionStatus) {
        status = newState
        sender.broadcast(GameStatusUpdate(newState))
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

    /**
     * Run the game logic of the user playing a card.
     * @param playerSessionId - current player session id,
     * @param cardId - either id of the card in the hand or the effect card id
     * @param target - id of the card to apply effect to (if applicable)
     */
    fun play(playerSessionId: String, cardId: String, target: String? = null) {
        if (checkTurn(playerSessionId)) {
            playCard(cardId, playerSessionId, target)
        }
    }

    fun endTurn(playerSessionId: String) {
        if (checkTurn(playerSessionId)) {
            endTurn()
        }
    }

    /**
     * Validate session id, turn and game state
     */
    private fun checkTurn(playerSessionId: String): Boolean {
        if (status == GameSessionStatus.Created) {
            sender.unicast(GameMessageUpdate("Game is not started yet"), playerSessionId)
            return false
        }

        if (status == GameSessionStatus.Player_1_Won || status == GameSessionStatus.Player_2_Won) {
            sender.unicast(GameMessageUpdate("Game is finished, start a new game"), playerSessionId)
            return false
        }

        // Validations
        val (currentPlayer, enemyPlayer) = getPlayers()

        if (playerSessionId != currentPlayer.sessionId && playerSessionId != enemyPlayer.sessionId) {
            logger.error("Invalid player id: $playerSessionId")
            return false
        }

        if (playerSessionId != currentPlayer.sessionId) {
            sender.unicast(GameMessageUpdate("It is ${currentPlayer.name}'s turn!"), playerSessionId)
            return false
        }
        return true
    }

    private fun playCard(cardId: String, playerSessionId: String, target: String?) {
        val (currentPlayer, enemyPlayer) = getPlayers()

        // selected card or effect exists
        val card = currentPlayer.findCardById(cardId) ?: currentPlayer.findEffectById(cardId)
        if (card == null) {
            sender.unicast(GameMessageUpdate("Error! No such card!"), playerSessionId)
            return
        }

        val targetEffect = if (target != null) {
            currentPlayer.findEffectById(target) ?: enemyPlayer.findEffectById(target)
        } else null

        if (target != null && targetEffect == null) {
            sender.unicast(GameMessageUpdate("Target not found!"), playerSessionId)
            return
        }

        val cardError = card.validator?.validate(card, currentPlayer, enemyPlayer, targetEffect)
        if (cardError != null) {
            sender.unicast(GameMessageUpdate(cardError), playerSessionId)
            return
        }

        if (gameRules.beforeCardPlayed(card, currentPlayer, enemyPlayer, this)) {
            card.onApply?.activate(card, currentPlayer, enemyPlayer, targetEffect)

            currentPlayer.cardPlayed(card)

            gameRules.afterCardPlayed(card, currentPlayer, enemyPlayer, this)
        }
    }

    private fun endTurn() {
        val (currentPlayer, enemyPlayer) = getPlayers()

        // activate current effects
        val expiredEffects = currentPlayer.updateEffects(currentPlayer, enemyPlayer)
        // remove the expired effects and sent updates
        expiredEffects.forEach {
            currentPlayer.removeEffect(it.id)
        }
        gameRules.onEndTurn(currentPlayer, enemyPlayer, this)
    }

    /**
     * returns the current player and the opponent, according to the game status
     */
    private fun getPlayers(): Pair<SessionPlayer, SessionPlayer> {
        val secondPlayer = player2
        check(secondPlayer != null) { "Player 2 is not initialized (Game was not started?)" }
        val currentPlayer = if (status == GameSessionStatus.Player_1_Turn) player1 else secondPlayer
        val enemyPlayer = if (status == GameSessionStatus.Player_2_Turn) player1 else secondPlayer
        return Pair(currentPlayer, enemyPlayer)
    }

}
