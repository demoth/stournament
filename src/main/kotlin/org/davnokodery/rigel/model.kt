package org.davnokodery.rigel

import org.davnokodery.rigel.GameSessionStatus.*
import org.davnokodery.stournament.GameException
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

fun interface Validator {
    fun validate(self: Card, owner: SessionPlayer, enemy: SessionPlayer, targetEffect: Card?): String?
}

data class Card(
    // template section
    val name: String,
    val iconName: String,
    val description: String,
    val tags: Set<String> = hashSetOf(), // todo: enum?

    // returns Error message if the card cannot be played
    val validator: Validator? = null,

    // instance section
    val properties: MutableMap<String, Int> = hashMapOf(), // todo: enum keys?
    val id: String = UUID.randomUUID().toString(),
    val onApply: (() -> Unit)? = null,
    val onTick: (() -> Unit)? = null,
    var ttl: Int = 0,
    val onExpire: (() -> Unit)? = null,
)

sealed class GameUpdate(
    val id: String = UUID.randomUUID().toString(),
)

data class GameStatusUpdate(val newStatus: GameSessionStatus) : GameUpdate()
data class GameMessageUpdate(val message: String) : GameUpdate()
data class PlayerPropertyChange(val playerName: String, val property: PlayerProperty, val delta: Int)

enum class PlayerProperty {
    Health
}

data class SessionPlayer(
    val name: String,
    val properties: EnumMap<PlayerProperty, Int> = EnumMap(PlayerProperty::class.java),
    val cards: MutableMap<String, Card> = hashMapOf(),
    val effects: MutableMap<String, Card> = hashMapOf(),
    val updates: Queue<GameUpdate> = ConcurrentLinkedQueue() // player specific updates
) {
    fun changeProperty(property: PlayerProperty, delta: Int): PlayerPropertyChange {
        val oldValue = properties[property]!!
        properties[property] = oldValue + delta

        return PlayerPropertyChange(name, property, delta)
    }
}

enum class GameSessionStatus {
    Created,
    Player_1_Turn,
    Player_2_Turn,
    Player_1_Won,
    Player_2_Won,
}

data class GameSession(
    val player1: SessionPlayer,
    val player2: SessionPlayer,
    val updates: Queue<GameUpdate> = ConcurrentLinkedQueue(), // common updates
    var status: GameSessionStatus = Created,
    var turn: Int = 1,

    ) {
    private fun broadcast(update: GameUpdate) {
        updates.offer(update)
    }

    private fun unicast(update: GameUpdate, player: SessionPlayer) {
        player.updates.offer(update)
    }

    private fun changeStatus(newState: GameSessionStatus) {
        status = newState
        updates.offer(GameStatusUpdate(newState))
    }

    fun startGame() {
        if (Random().nextBoolean()) {
            changeStatus(Player_1_Turn)
        } else {
            changeStatus(Player_2_Turn)
        }
    }

    /**
     * @param playerName - current player name, todo: switch to jwt,
     * @param cardId - either id of the card in the hand or the effect card id, when null - skipping turn,
     * @param target - id of the card to apply effect to (if applicable)
     */
    fun play(playerName: String, cardId: String? = null, target: String? = null) {

        // Validations

        // todo: change playerName to jwt validation
        if (playerName != player1.name && playerName != player2.name) {
            println("No such player: $playerName")
            return
        }

        if (status == Created) {
            broadcast(GameMessageUpdate("Game is not started yet"))
            return
        }

        if (status == Player_1_Won || status == Player_2_Won) {
            broadcast(GameMessageUpdate("Game is finished, start a new game"))
            return
        }

        val currentPlayer = if (status == Player_1_Turn) player1 else player2
        val enemyPlayer = if (status == Player_2_Turn) player1 else player2

        if (playerName != currentPlayer.name) {
            unicast(GameMessageUpdate("It is ${currentPlayer.name}'s turn!"), if (playerName == player1.name) player1 else player2)
            return
        }

        // skipping turn
        if (cardId == null) {
            // todo: apply current effects
            changeStatus(if (status == Player_1_Turn) Player_2_Turn else Player_1_Turn)
            return
        }

        // selected card or effect exists
        val card = currentPlayer.cards[cardId] ?: currentPlayer.effects[cardId]
        if (card == null) {
            unicast(GameMessageUpdate("Error! No such card!"), currentPlayer)
            return
        }

        val targetEffect = currentPlayer.effects[target] ?: enemyPlayer.effects[target]

        if (target != null && targetEffect == null) {
            unicast(GameMessageUpdate("Target not found!"), currentPlayer)
            return
        }

        val cardError = card.validator?.validate(card, currentPlayer, enemyPlayer, targetEffect)
        if (cardError != null) {
            unicast(GameMessageUpdate(cardError), currentPlayer)
            return
        }

        // todo: everything else
    }

}
