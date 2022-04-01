package org.davnokodery.rigel

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

internal class GameSessionTest {

    private lateinit var player1: SessionPlayer
    private lateinit var player2: SessionPlayer
    private lateinit var newGame: GameSession

    var cardPlayed = false

    private fun initPlayer(player: SessionPlayer, playerId: Int) {
        player.properties[PlayerProperty.Health] = 100
        player.properties[PlayerProperty.MaxHealth] = 100

        createHealingCard(playerId).let { player.cards[it.id] = it }
        createFireballCard(playerId).let { player.cards[it.id] = it }
    }

    private fun createHealingCard(player: Int) = Card(
        id = "healing$player",
        name = "healing$player",
        iconName = "healing$player",
        description = "heal 1 hp for 3 turns",
        validator = { _, p, _, _ ->
            if (p.properties[PlayerProperty.Health]!! >= p.properties[PlayerProperty.MaxHealth]!!)
                "Health is already full"
            else
                null
        },
        onTick = { _, p, _ ->
            if (p.properties[PlayerProperty.Health]!! < p.properties[PlayerProperty.MaxHealth]!!) {
                p.changeProperty(PlayerProperty.Health, 1)
            }
        },
        ttl = 2
    )

    // todo: use damage function instead of direct property manipulation
    private fun createFireballCard(player: Int) = Card(
        id = "fireball$player",
        name = "fireball$player",
        iconName = "fireball$player",
        description = "fireball$player",
        onApply = { _, _, e, _ ->
            e.changeProperty(PlayerProperty.Health, -5)
        })

    @BeforeEach
    fun setup() {

        player1 = SessionPlayer("player1").apply {
            initPlayer(this, 1)
        }

        player2 = SessionPlayer("player2").apply {
            initPlayer(this, 2)
        }
        newGame = GameSession(player1, player2)
    }

    @AfterEach
    fun printUpdates() {
        newGame.updates.forEach {
            println(it)
        }
        player1.updates.forEach {
            println(it)
        }
        player2.updates.forEach {
            println(it)
        }
    }

    @Test
    fun `play - game status is changed when the game is started`() {
        assertEquals(GameSessionStatus.Created, newGame.status)

        newGame.startGame()

        val newStatus = newGame.status
        assertTrue(newStatus == GameSessionStatus.Player_1_Turn || newStatus == GameSessionStatus.Player_2_Turn)
        val gameUpdate = newGame.updates.poll() as? GameStatusUpdate
        assertEquals(newStatus, gameUpdate?.newStatus)
    }

    @Test
    fun `play - validate no such player`() {
        newGame.play("who am i?")
        assertTrue(newGame.updates.isEmpty())
    }

    @Test
    fun `play - game is not started`() {
        val currentPlayerName = if (newGame.status == GameSessionStatus.Player_1_Turn) player1.name else player2.name
        newGame.play(currentPlayerName)
        val gameUpdate = newGame.updates.poll() as? GameMessageUpdate
        assertEquals("Game is not started yet", gameUpdate?.message)

    }

    @Test
    fun `play - end turn`() {
        newGame.startGame()
        val oldStatus = newGame.status
        val currentPlayerName = if (oldStatus == GameSessionStatus.Player_1_Turn) player1.name else player2.name
        // end turn
        newGame.play(currentPlayerName)
        assertNotEquals(oldStatus, newGame.status)
    }

    @Test
    fun `play - game is finished`() {
        newGame.startGame()
        val oldStatus = newGame.status
        val currentPlayerName = if (oldStatus == GameSessionStatus.Player_1_Turn) player1.name else player2.name
        newGame.status = GameSessionStatus.Player_2_Won
        newGame.updates.clear()

        newGame.play(currentPlayerName)
        val gameUpdate = newGame.updates.poll() as? GameMessageUpdate
        assertEquals("Game is finished, start a new game", gameUpdate?.message)
    }

    @Test
    fun `play - not in turn`() {
        newGame.startGame()
        val oldStatus = newGame.status
        val currentPlayerName = if (oldStatus == GameSessionStatus.Player_1_Turn) player1.name else player2.name
        val wrongPlayer = if (oldStatus != GameSessionStatus.Player_1_Turn) player1 else player2
        newGame.updates.clear()

        newGame.play(wrongPlayer.name)
        val gameUpdate = wrongPlayer.updates.poll() as? GameMessageUpdate
        assertEquals("It is $currentPlayerName's turn!", gameUpdate?.message)
    }

    @Test
    fun `play - card does not exist`() {
        newGame.startGame()
        val oldStatus = newGame.status
        val currentPlayer = if (oldStatus == GameSessionStatus.Player_1_Turn) player1 else player2
        // skip turn
        newGame.play(currentPlayer.name, "whatever")
        val gameUpdate = currentPlayer.updates.poll() as? GameMessageUpdate
        assertEquals("Error! No such card!", gameUpdate?.message)
    }

    @Test
    fun `play - target not found`() {
        newGame.startGame()
        val oldStatus = newGame.status
        val currentPlayer = if (oldStatus == GameSessionStatus.Player_1_Turn) player1 else player2
        // skip turn
        newGame.play(currentPlayer.name, currentPlayer.cards.values.first().id, "wherever")
        val gameUpdate = currentPlayer.updates.poll() as? GameMessageUpdate
        assertEquals("Target not found!", gameUpdate?.message)
    }

    @Test
    fun `play - card could not be played due to validation`() {
        newGame.startGame()
        newGame.status = GameSessionStatus.Player_1_Turn
        // skip turn
        newGame.play(player1.name, "healing1")
        val playerUpdate = player1.updates.poll() as? GameMessageUpdate
        assertEquals("Health is already full", playerUpdate?.message)
    }

    @Test
    fun `play - play a spell card`() {
        newGame.startGame()
        newGame.status = GameSessionStatus.Player_2_Turn
        newGame.updates.clear()
        newGame.play(player2.name, "fireball2")
        assertTrue(player2.updates.isEmpty(), "Unexpected updates: ${player2.updates}")
        assertEquals(95, player1.properties[PlayerProperty.Health])
        val cardPlayed = newGame.updates.poll() as? CardPlayed
        assertEquals("fireball2", cardPlayed?.cardId)
        assertEquals(true, cardPlayed?.discarded)
    }

    @Test
    fun `play - play a card with lasting effect`() {
        newGame.startGame()
        newGame.status = GameSessionStatus.Player_1_Turn
        player1.changeProperty(PlayerProperty.Health, -50)
        newGame.play(player1.name, "healing1")
        newGame.play(player1.name) //end turn
        newGame.play(player2.name) //end turn
        newGame.play(player1.name) //end turn

        assertEquals(52, player1.properties[PlayerProperty.Health])
        assertNotNull(newGame.updates.find { it is CardPlayed && it.cardId == "healing1" && it.discarded }, "Card did not expire")
    }

}
