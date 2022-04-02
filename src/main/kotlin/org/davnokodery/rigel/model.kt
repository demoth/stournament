package org.davnokodery.rigel

import org.davnokodery.rigel.GameSessionStatus.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.persistence.Entity
import javax.persistence.Id

val logger = LoggerFactory.getLogger("model.kt")

fun interface Validator {
    fun validate(self: Card, owner: SessionPlayer, enemy: SessionPlayer, targetEffect: Card?): String?
}

fun interface CardAction {
    fun activate(self: Card, owner: SessionPlayer, enemy: SessionPlayer, targetEffect: Card?)
}

fun interface CardEffect {
    fun effect(self: Card, owner: SessionPlayer, enemy: SessionPlayer)
}

// todo: split into Card and Effect?
typealias CardId = String;

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
    val id: CardId = UUID.randomUUID().toString(),
    /**
     * Instant action related the played card, the card can save the id of the target,
     * but should check it is still valid (not expired)
     */
    val onApply: CardAction? = null,
    val onTick: CardEffect? = null,
    val onExpire: CardEffect? = null,
    var ttl: Int = 0, // when reaches 0 the effect expires
)



sealed class GameUpdate(
    val id: String = UUID.randomUUID().toString(), // todo -> change to the incrementing counter (owner: GameSession)
)

data class GameStatusUpdate(val newStatus: GameSessionStatus) : GameUpdate()
data class GameMessageUpdate(val message: String) : GameUpdate()
data class PlayerPropertyChange(val playerName: String, val property: PlayerProperty, val delta: Int): GameUpdate()
data class CardPlayed(val cardId: String, val discarded: Boolean): GameUpdate()

enum class PlayerProperty {
    Health,
    MaxHealth
}

data class SessionPlayer(
    val name: String,
    private val properties: EnumMap<PlayerProperty, Int> = EnumMap(PlayerProperty::class.java),
    val propertyChanges: EnumMap<PlayerProperty, MutableMap<CardId, Int>> = EnumMap(PlayerProperty::class.java),
    val cards: MutableMap<String, Card> = hashMapOf(), // todo make it private
    val effects: MutableMap<String, Card> = hashMapOf(),
    val updates: Queue<GameUpdate> = ConcurrentLinkedQueue(), // player specific updates

) {

    fun getProperty(property: PlayerProperty ): Int {
        return properties[property]!! + (propertyChanges[property]?.values?.sum() ?:0)
    }

    fun changeProperty(property: PlayerProperty, delta: Int) {
        val oldValue = properties[property]!!
        properties[property] = oldValue + delta
        // todo need to calculate new values for health or mana and broadcast them
        updates.offer(PlayerPropertyChange(name, property, delta))
    }

    fun changePropertyTemporary(property: PlayerProperty, delta: Int, cardId: String) {
        val changes = propertyChanges[property]

        if (changes == null) {
            propertyChanges[property] = mutableMapOf(cardId to delta)
        } else {
            changes[cardId] = delta
        }

        // todo need to calculate new values for health or mana and broadcast them
        updates.offer(PlayerPropertyChange(name, property, delta))
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
    private val player1: SessionPlayer,
    private val player2: SessionPlayer,
    internal val updates: Queue<GameUpdate> = ConcurrentLinkedQueue(), // common updates
    internal var status: GameSessionStatus = Created,
    private var turn: Int = 1,

    ) {
    private fun broadcast(update: GameUpdate) {
        updates.offer(update)
    }

    private fun unicast(update: GameUpdate, player: SessionPlayer) {
        player.updates.offer(update)
    }

    private fun changeStatus(newState: GameSessionStatus) {
        status = newState
        broadcast(GameStatusUpdate(newState))
    }

    fun startGame() {
        if (Random().nextBoolean()) {
            changeStatus(Player_1_Turn)
        } else {
            changeStatus(Player_2_Turn)
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

        if (cardId != null) {

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

            card.onApply?.activate(card, currentPlayer, enemyPlayer, targetEffect)
            currentPlayer.cards.remove(card.id)
            if (card.ttl > 0) {
                // if a card has a lasting effect -> move it to the current effects
                currentPlayer.effects[card.id] = card
                broadcast(CardPlayed(card.id, false))
            } else {
                // discard otherwise
                broadcast(CardPlayed(card.id, true))
            }
        } else {
            // end turn
            // activate current effects
            val expiredEffects = currentPlayer.effects.values.filter {
                it.onTick?.effect(it, currentPlayer, enemyPlayer)
                it.ttl--
                if (it.ttl <= 0) {
                    it.onExpire?.effect(it, currentPlayer, enemyPlayer)
                }
                it.ttl <= 0
            }
            // remove the expired effects and sent updates
            expiredEffects.forEach {
                currentPlayer.effects.remove(it.id)
                broadcast(CardPlayed(it.id, true))
            }
            changeStatus(if (status == Player_1_Turn) Player_2_Turn else Player_1_Turn)
        }
    }

}

@Entity
data class User(
    @Id var name: String,
    var password: String
)

data class LoginRequest(
    val name: String,
    val password: String
)

data class LoginResponse(
    val jwt: String
)
