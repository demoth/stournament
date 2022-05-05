package org.davnokodery.rigel.model

import org.davnokodery.rigel.CardPlayed
import org.davnokodery.rigel.MessageSender
import org.davnokodery.rigel.NewCard
import org.davnokodery.rigel.PlayerPropertyChange

/**
 * Represents connection related data, can change during player reconnect
 */
data class PlayerSession(
    val sessionId: String,
    val sender: MessageSender
)

/**
 * Represents game related player data. Cannot change during the game
 */
data class Player(
    val name: String,
    var session: PlayerSession? = null,
    private val properties: MutableMap<String, Int> = hashMapOf(),
    private val propertyChanges: MutableMap<String, MutableMap<CardId, Int>> = hashMapOf(),
    private val cards: MutableMap<String, Card> = hashMapOf(),
    private val effects: MutableMap<String, Card> = hashMapOf(),
) {

    fun addCard(card: Card) {
        check(!cards.contains(card.id)) { "Player already has a card with id ${card.id}" }
        val session = checkNotNull(session) { "Player disconnected" } // fixme: player can disconnect anytime!
        cards[card.id] = card
        session.sender.unicast(NewCard(card.toCardData()), session.sessionId) // todo: make opponent know
    }

    fun getProperty(property: String): Int {
        return (properties[property] ?: 0) + (propertyChanges[property]?.values?.sum() ?: 0)
    }

    fun changeProperty(property: String, delta: Int) {
        val oldValue = properties[property] ?: 0
        properties[property] = oldValue + delta
        sendPropertyUpdate(PlayerPropertyChange(name, property, delta, getProperty(property)))
    }

    /**
     * Remove all temporary property changes associates with a given card id.
     */
    private fun removeTemporaryPropertyChange(id: CardId) {
        propertyChanges.forEach { (property, changes) ->
            val oldDelta = changes.remove(id)
            if (oldDelta != null) {
                sendPropertyUpdate(PlayerPropertyChange(name, property, -oldDelta, getProperty(property)))
            }
        }
    }

    private fun sendPropertyUpdate(msg: PlayerPropertyChange) {
        val session = checkNotNull(session) { "Player disconnected" } // fixme: player can disconnect anytime!
        session.sender.broadcast(msg)
    }

    fun changePropertyTemporary(property: String, delta: Int, cardId: String) {
        val changes = propertyChanges[property]

        if (changes == null) {
            propertyChanges[property] = mutableMapOf(cardId to delta)
        } else {
            // notify that old change has expired
            val oldDelta = changes[cardId]
            if (oldDelta != null)
                sendPropertyUpdate(PlayerPropertyChange(name, property, -oldDelta, getProperty(property)))
            changes[cardId] = delta
        }

        sendPropertyUpdate(PlayerPropertyChange(name, property, delta, getProperty(property)))
    }

    fun cardPlayed(card: Card) {
        val session = checkNotNull(session) { "Player disconnected" } // fixme: player can disconnect anytime!

        cards.remove(card.id)
        if (card.ttl > 0) {
            // if a card has a lasting effect -> move it to the current effects
            effects[card.id] = card
            session.sender.broadcast(CardPlayed(card.id, false)) //todo: reveal when played
        } else {
            // discard otherwise
            session.sender.broadcast(CardPlayed(card.id, true))
        }
    }

    /**
     * throws an NoSuchElementException if not found. Used in tests
     */
    fun findCardByName(name: String) = cards.values.first { it.name == name }

    /**
     * returns null if not found
     */
    fun findCardById(id: String) = cards.values.find { it.id == id }

    /**
     * returns null if not found
     */
    fun findEffectById(id: String) = effects.values.find { it.id == id }

    fun updateEffects(currentPlayer: Player, enemyPlayer: Player) = effects.values.filter {
        it.onTick?.effect(it, currentPlayer, enemyPlayer)
        it.ttl--
        if (it.ttl <= 0) {
            it.onExpire?.effect(it, currentPlayer, enemyPlayer)
            currentPlayer.removeTemporaryPropertyChange(it.id)
            enemyPlayer.removeTemporaryPropertyChange(it.id)
        }
        it.ttl <= 0
    }

    fun removeEffect(id: String) {
        val session = checkNotNull(session) { "Player disconnected" } // fixme: player can disconnect anytime!

        effects.remove(id)
        session.sender.broadcast(CardPlayed(id, true))
    }
}
