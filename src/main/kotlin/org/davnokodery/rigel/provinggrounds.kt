package org.davnokodery.rigel

import org.davnokodery.rigel.model.*

// Test game logic, a.k.a Proving Grounds

const val PROP_HEALTH = "health"
const val PROP_HEALTH_MAX = "health_max"
const val PROP_COLD_RESIST = "resist_cold"
const val PROP_FIRE_RESIST = "resist_fire"

fun provingGroundsRules(): GameRules {
    return object : GameRules {
        override fun onGameStarted(player: SessionPlayer, enemyPlayer: SessionPlayer, gameSession: GameSession) {
            listOf(player, enemyPlayer).forEach { p ->
                // initialize properties
                hashMapOf(
                    PROP_HEALTH to 100,
                    PROP_HEALTH_MAX to 100,
                    PROP_COLD_RESIST to 0,
                    PROP_FIRE_RESIST to 0,
                ).forEach { (prop, value) -> p.changeProperty(prop, value) }

                // add start deck
                createHealingCard(p.name).let { p.cards[it.id] = it }
                createFireballCard(p.name).let { p.cards[it.id] = it }
                createIceShieldCard(p.name).let { p.cards[it.id] = it }
                createFireShieldCard(p.name).let { p.cards[it.id] = it }
            }
        }
        
        override fun beforeCardPlayed(
            card: Card,
            player: SessionPlayer,
            enemyPlayer: SessionPlayer,
            gameSession: GameSession
        ): Boolean {
            // do some common logic, like check of there is enough action points
            return true
        }
        
        override fun afterCardPlayed(
            card: Card,
            player: SessionPlayer,
            enemyPlayer: SessionPlayer,
            gameSession: GameSession
        ) {
            // do some common logic, like use action point
        }

        override fun onEndTurn(player: SessionPlayer, enemyPlayer: SessionPlayer, gameSession: GameSession) {
            // deal a new card for example  
        }
    }
}

private fun createHealingCard(player: String) = Card(
    id = "healing_$player",
    name = "healing_$player",
    iconName = "healing_$player",
    description = "heal 1 hp for 3 turns",
    validator = { _, p, _, _ ->
        if (p.getProperty(PROP_HEALTH) >= p.getProperty(PROP_HEALTH_MAX))
            "Health is already full"
        else
            null
    },
    onTick = { _, p, _ ->
        if (p.getProperty(PROP_HEALTH) < p.getProperty(PROP_HEALTH_MAX)) {
            p.changeProperty(PROP_HEALTH, 1)
        } // fixme: expire sooner if health is full 
    },
    ttl = 2
)

// todo: use damage function instead of direct property manipulation
private fun createFireballCard(player: String) = Card(
    id = "fireball_$player",
    name = "fireball_$player",
    iconName = "fireball_$player",
    description = "fireball_$player",
    onApply = { _, _, e, _ ->
        e.changeProperty(PROP_HEALTH, -5)
    })

private fun createIceShieldCard(player: String) = Card(
    id = "iceShield_$player",
    name = "iceShield_$player",
    iconName = "iceShield_$player",
    description = "iceShield_$player",
    ttl = 2,
    onApply = { _, p, _, _ ->
        p.changePropertyTemporary(PROP_COLD_RESIST, 15, "iceShield_$player")
    })

/**
 * Adds a diminishing fire resist for 2 turns: 1st turn +20%, 2nd +10%
 */
private fun createFireShieldCard(player: String) = Card(
    id = "fireShield_$player",
    name = "fireShield_$player",
    iconName = "fireShield_$player",
    description = "fireShield_$player",
    ttl = 3,
    onTick = { c, p, _ ->
        p.changePropertyTemporary(PROP_FIRE_RESIST, c.ttl * 10, "fireShield_$player")
    })
