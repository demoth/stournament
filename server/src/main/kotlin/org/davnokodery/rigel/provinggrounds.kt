package org.davnokodery.rigel

import org.davnokodery.rigel.model.Card
import org.davnokodery.rigel.model.GameRules
import org.davnokodery.rigel.model.GameSession
import org.davnokodery.rigel.model.GameSessionStatus.*
import org.davnokodery.rigel.model.Player
import kotlin.random.Random

// Test game logic, a.k.a Proving Grounds

const val PROP_HEALTH = "health"
const val PROP_HEALTH_MAX = "health_max"
const val PROP_COLD_RESIST = "resist_cold"
const val PROP_FIRE_RESIST = "resist_fire"

// card names
const val FIRE_BALL_NAME = "fireball"
const val HEALING_NAME = "healing"
const val FIRE_SHIELD_NAME = "fire_shield"
const val ICE_SHIELD_NAME = "ice_shield"
const val DEATH_RAY = "death_ray"

fun provingGroundsRules(): GameRules {
    return object : GameRules {
        override fun onGameStarted(player: Player, enemyPlayer: Player, gameSession: GameSession) {
            listOf(player, enemyPlayer).forEach { p ->
                // initialize properties
                hashMapOf(
                    PROP_HEALTH to 100,
                    PROP_HEALTH_MAX to 100,
                    PROP_COLD_RESIST to 0,
                    PROP_FIRE_RESIST to 0,
                ).forEach { (prop, value) -> p.changeProperty(prop, value) }

                // add start deck
                p.addCard(createHealingCard())
                p.addCard(createFireballCard())
                p.addCard(createIceShieldCard())
                p.addCard(createFireShieldCard())
            }
        }
        
        override fun beforeCardPlayed(
            card: Card,
            player: Player,
            enemyPlayer: Player,
            gameSession: GameSession
        ): Boolean {
            // do some common logic, like check of there is enough action points
            return true
        }
        
        override fun afterCardPlayed(
            card: Card,
            player: Player,
            enemyPlayer: Player,
            gameSession: GameSession
        ) {
            // do some common logic, like use action point
        }

        override fun onEndTurn(player: Player, enemyPlayer: Player, gameSession: GameSession) {
            // check if game is finished
            val status = gameSession.status
            check(status == Player_1_Turn || status == Player_2_Turn) { "Unexpected game state: $status" }
            if (enemyPlayer.getProperty(PROP_HEALTH) <= 0) {
                // current player won
                gameSession.changeStatus(if (status == Player_1_Turn) Player_1_Won else Player_2_Won)
            } else if (player.getProperty(PROP_HEALTH) <= 0) {
                // enemy player won
                gameSession.changeStatus(if (status == Player_2_Turn) Player_1_Won else Player_2_Won)
            } else {
                // deal a new card
                val newCard = when (Random.nextInt(3)) {
                    0 -> createFireShieldCard()
                    1 -> createIceShieldCard()
                    2 -> createHealingCard()
                    else -> createFireballCard()
                }
                player.addCard(newCard)

                gameSession.changeStatus(if (status == Player_1_Turn) Player_2_Turn else Player_1_Turn)
            }
        }
    }
}

private fun createHealingCard() = Card(
    name = HEALING_NAME,
    iconName = HEALING_NAME,
    description = HEALING_NAME,
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
private fun createFireballCard() = Card(
    name = FIRE_BALL_NAME,
    iconName = FIRE_BALL_NAME,
    description = FIRE_BALL_NAME,
    onApply = { _, _, e, _ ->
        e.changeProperty(PROP_HEALTH, -5)
    })


private fun createIceShieldCard() = Card(
    name = ICE_SHIELD_NAME,
    iconName = ICE_SHIELD_NAME,
    description = ICE_SHIELD_NAME,
    ttl = 2,
    onApply = { c, p, _, _ ->
        p.changePropertyTemporary(PROP_COLD_RESIST, 15, c.id)
    })

/**
 * Adds a diminishing fire resist for 2 turns: 1st turn +20%, 2nd +10%
 */
private fun createFireShieldCard() = Card(
    name = FIRE_SHIELD_NAME,
    iconName = FIRE_SHIELD_NAME,
    description = FIRE_SHIELD_NAME,
    ttl = 3,
    onTick = { c, p, _ ->
        p.changePropertyTemporary(PROP_FIRE_RESIST, c.ttl * 10, c.id)
    })

fun createDeathRayCard() = Card(
    name = DEATH_RAY,
    iconName = DEATH_RAY,
    description = DEATH_RAY,
    onApply = { _, _, e, _ ->
        e.changeProperty(PROP_HEALTH, -200)
    })
