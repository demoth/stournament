package org.davnokodery.rigel.model

import org.davnokodery.rigel.GameUpdate
import org.davnokodery.rigel.PlayerPropertyChange
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

enum class PlayerProperty {
    Health,
    MaxHealth,
    ColdResist,
    FireResist,
}

data class SessionPlayer(
    val name: String,
    private val properties: EnumMap<PlayerProperty, Int> = EnumMap(PlayerProperty::class.java),
    private val propertyChanges: EnumMap<PlayerProperty, MutableMap<CardId, Int>> = EnumMap(PlayerProperty::class.java),
    val cards: MutableMap<String, Card> = hashMapOf(), // todo make it private
    val effects: MutableMap<String, Card> = hashMapOf(),
    val updates: Queue<GameUpdate> = ConcurrentLinkedQueue(), // player specific updates
) {

    fun getProperty(property: PlayerProperty): Int {
        return properties[property]!! + (propertyChanges[property]?.values?.sum() ?: 0)
    }

    fun changeProperty(property: PlayerProperty, delta: Int) {
        val oldValue = properties[property]!!
        properties[property] = oldValue + delta
        // todo need to calculate new values for health or mana and broadcast them
        updates.offer(PlayerPropertyChange(name, property, delta))
    }

    fun removeTemporaryPropertyChange(id: CardId) {
        propertyChanges.forEach { (property, changes) ->
            val oldDelta = changes.remove(id)
            if (oldDelta != null) {
                updates.offer(PlayerPropertyChange(name, property, -oldDelta))
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
                updates.offer(PlayerPropertyChange(name, property, -oldDelta))
            changes[cardId] = delta
        }

        // todo need to calculate new values for health or mana and broadcast them
        updates.offer(PlayerPropertyChange(name, property, delta))
    }
}
