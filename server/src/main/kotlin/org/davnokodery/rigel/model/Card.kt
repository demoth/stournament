package org.davnokodery.rigel.model

import java.util.*

fun interface Validator {
    fun validate(self: Card, owner: Player, enemy: Player, targetEffect: Card?): String?
}

fun interface CardAction {
    fun activate(self: Card, owner: Player, enemy: Player, targetEffect: Card?)
}

fun interface CardEffect {
    fun effect(self: Card, owner: Player, enemy: Player)
}

typealias CardId = String


data class Card(
    // template section
    val name: String,
    val iconName: String,
    val description: String,
    val tags: Set<String> = hashSetOf(),

    // returns Error message if the card cannot be played
    val validator: Validator? = null,

    // instance section
    val properties: MutableMap<String, Int> = hashMapOf(),
    val id: CardId = UUID.randomUUID().toString(),
    /**
     * Instant action related the played card, the card can save the id of the target,
     * but should check it is still valid (not expired)
     */
    val onApply: CardAction? = null,

    // effect related, todo: make more explicit ?

    // invoked at owners turn
    val onTick: CardEffect? = null,
    // invoked when ttl reaches 0
    val onExpire: CardEffect? = null,
    /**
     * decremented at the end of the owning player's turn.
     * TTL = 2 means 2 owning player turns and 1 enemy player's turn.
     */
    var ttl: Int = 0, // when reaches 0 the effect expires
) {
    fun toCardData() = CardData(id, name, iconName, description, tags)
}

/**
 * What is sent to the client
 */
data class CardData(
    val id: String,
    val name: String,
    val iconName: String,
    val description: String,
    val tags: Set<String> = hashSetOf()
)