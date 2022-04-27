package org.davnokodery.rigel.model

import org.davnokodery.rigel.MessageSender
import org.davnokodery.rigel.PlayerPropertyChange
import java.util.*

data class SessionPlayer(
    val sessionId: String,
    val name: String,
    val sender: MessageSender,
    private val properties: MutableMap<String, Int> = hashMapOf(),
    private val propertyChanges: MutableMap<String, MutableMap<CardId, Int>> = hashMapOf(),
    val cards: MutableMap<String, Card> = hashMapOf(), // todo make it private
    val effects: MutableMap<String, Card> = hashMapOf()
) {

    fun getProperty(property: String): Int {
        return properties[property]!! + (propertyChanges[property]?.values?.sum() ?: 0)
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
    fun removeTemporaryPropertyChange(id: CardId) {
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
}
