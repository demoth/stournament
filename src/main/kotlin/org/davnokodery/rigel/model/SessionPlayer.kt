package org.davnokodery.rigel.model

import org.davnokodery.rigel.CardPlayed
import org.davnokodery.rigel.MessageSender
import org.davnokodery.rigel.NewCard
import org.davnokodery.rigel.PlayerPropertyChange

data class SessionPlayer(
    val sessionId: String,
    val name: String,
    private val sender: MessageSender,
    private val properties: MutableMap<String, Int> = hashMapOf(),
    private val propertyChanges: MutableMap<String, MutableMap<CardId, Int>> = hashMapOf(),
    private val cards: MutableMap<String, Card> = hashMapOf(), // todo make it private
    private val effects: MutableMap<String, Card> = hashMapOf()
) {

    fun addCard(card: Card) {
        check(!cards.contains(card.id)) { "Player already has a card with id ${card.id}" }
        cards[card.id] = card
        sender.send(NewCard(card.toCardData()))
    }

    fun getProperty(property: String): Int {
        return (properties[property] ?: 0) + (propertyChanges[property]?.values?.sum() ?: 0)
    }

    fun changeProperty(property: String, delta: Int) {
        val oldValue = properties[property] ?: 0
        properties[property] = oldValue + delta
        // todo need to calculate new values for health or mana and broadcast them
        sender.send(PlayerPropertyChange(sessionId, property, delta))
    }

    /**
     * Remove all temporary property changes associates with a given card id.
     */
    private fun removeTemporaryPropertyChange(id: CardId) {
        propertyChanges.forEach { (property, changes) ->
            val oldDelta = changes.remove(id)
            if (oldDelta != null) {
                sender.send(PlayerPropertyChange(sessionId, property, -oldDelta))
            }
        }
    }

    fun changePropertyTemporary(property: String, delta: Int, cardId: String) {
        val changes = propertyChanges[property]

        if (changes == null) {
            propertyChanges[property] = mutableMapOf(cardId to delta)
        } else {
            // notify that old change has expired
            val oldDelta = changes[cardId]
            if (oldDelta != null)
                sender.send(PlayerPropertyChange(sessionId, property, -oldDelta))
            changes[cardId] = delta
        }

        // todo need to calculate new values for health or mana and broadcast them
        sender.send(PlayerPropertyChange(sessionId, property, delta))
    }

    fun cardPlayed(card: Card) {
        cards.remove(card.id)
        if (card.ttl > 0) {
            // if a card has a lasting effect -> move it to the current effects
            effects[card.id] = card
            sender.send(CardPlayed(card.id, false))
        } else {
            // discard otherwise
            sender.send(CardPlayed(card.id, true))
        }
    }

    fun findCardByName(name: String) = cards.values.find { it.name == name }

    fun findCardById(id: String) = cards.values.find { it.id == id }

    fun findEffectById(id: String) = effects.values.find { it.id == id }

    fun updateEffects(currentPlayer: SessionPlayer, enemyPlayer: SessionPlayer) = effects.values.filter {
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
        effects.remove(id)
        sender.send(CardPlayed(id, true))
    }
}
