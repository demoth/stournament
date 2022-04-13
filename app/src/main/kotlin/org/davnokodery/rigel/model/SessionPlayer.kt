package org.davnokodery.rigel.model

import org.davnokodery.rigel.MessageSender
import org.davnokodery.rigel.PlayerPropertyChange
import java.util.*

enum class PlayerProperty {
    Health,
    MaxHealth,
    ColdResist,
    FireResist,
}

data class SessionPlayer(
    val sessionId: String,
    val name: String,
    val sender: MessageSender,
    private val properties: EnumMap<PlayerProperty, Int> = EnumMap(PlayerProperty::class.java),
    private val propertyChanges: EnumMap<PlayerProperty, MutableMap<CardId, Int>> = EnumMap(PlayerProperty::class.java),
    val cards: MutableMap<String, Card> = hashMapOf(), // todo make it private
    val effects: MutableMap<String, Card> = hashMapOf()
) {

    fun getProperty(property: PlayerProperty): Int {
        return properties[property]!! + (propertyChanges[property]?.values?.sum() ?: 0)
    }

    fun changeProperty(property: PlayerProperty, delta: Int) {
        val oldValue = properties[property]!!
        properties[property] = oldValue + delta
        // todo need to calculate new values for health or mana and broadcast them
        sender.send(PlayerPropertyChange(sessionId, property, delta))
    }

    fun removeTemporaryPropertyChange(id: CardId) {
        propertyChanges.forEach { (property, changes) ->
            val oldDelta = changes.remove(id)
            if (oldDelta != null) {
                sender.send(PlayerPropertyChange(sessionId, property, -oldDelta))
            }
        }
    }

    fun changePropertyTemporary(property: PlayerProperty, delta: Int, cardId: String) {
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
