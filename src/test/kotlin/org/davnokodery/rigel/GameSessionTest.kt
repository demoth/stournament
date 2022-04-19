package org.davnokodery.rigel

import org.davnokodery.rigel.model.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class GameSessionTest {

    private lateinit var player1: SessionPlayer
    private lateinit var player2: SessionPlayer
    private lateinit var newGame: GameSession
    private val messages = mutableListOf<ServerWsMessage>()
    private val sender = MessageSender { messages.add(it) }

    private fun addTestCards(player: SessionPlayer, playerId: String) {
        createHealingCard(playerId).let { player.cards[it.id] = it }
        createFireballCard(playerId).let { player.cards[it.id] = it }
        createIceShieldCard(playerId).let { player.cards[it.id] = it }
        createFireShieldCard(playerId).let { player.cards[it.id] = it }
    }

    private fun createHealingCard(player: String) = Card(
        id = "healing$player",
        name = "healing$player",
        iconName = "healing$player",
        description = "heal 1 hp for 3 turns",
        validator = { _, p, _, _ ->
            if (p.getProperty(PlayerProperty.Health) >= p.getProperty(PlayerProperty.MaxHealth))
                "Health is already full"
            else
                null
        },
        onTick = { _, p, _ ->
            if (p.getProperty(PlayerProperty.Health) < p.getProperty(PlayerProperty.MaxHealth)) {
                p.changeProperty(PlayerProperty.Health, 1)
            }
        },
        ttl = 2
    )

    // todo: use damage function instead of direct property manipulation
    private fun createFireballCard(player: String) = Card(
        id = "fireball$player",
        name = "fireball$player",
        iconName = "fireball$player",
        description = "fireball$player",
        onApply = { _, _, e, _ ->
            e.changeProperty(PlayerProperty.Health, -5)
        })

    private fun createIceShieldCard(player: String) = Card(
        id = "iceShield$player",
        name = "iceShield$player",
        iconName = "iceShield$player",
        description = "iceShield$player",
        ttl = 2,
        onApply = { _, p, _, _ ->
            p.changePropertyTemporary(PlayerProperty.ColdResist, 15, "iceShield$player")
        })

    /**
     * Adds a diminishing fire resist for 2 turns: 1st turn +20%, 2nd +10%
     */
    private fun createFireShieldCard(player: String) = Card(
        id = "fireShield$player",
        name = "fireShield$player",
        iconName = "fireShield$player",
        description = "fireShield$player",
        ttl = 3,
        onTick = { c, p, _ ->
            p.changePropertyTemporary(PlayerProperty.FireResist, c.ttl * 10, "fireShield$player")
        })

    @BeforeEach
    fun setup() {
        player1 = createTestPlayer("1")
        player2 = createTestPlayer("2")
        newGame = GameSession("id", player1, sender)
        newGame.player2 = player2
        messages.clear()
    }

    private fun createTestPlayer(id: String) =
        SessionPlayer(
            sessionId = "player$id", name = "$id name", sender = sender, properties = EnumMap(
                mapOf(
                    PlayerProperty.Health to 100,
                    PlayerProperty.MaxHealth to 100,
                    PlayerProperty.ColdResist to 0,
                    PlayerProperty.FireResist to 0,
                )
            )
        ).apply {
            addTestCards(this, id)
        }

    @AfterEach
    fun printUpdates() {
        messages.forEach {
            println(it)
        }
    }

    @Test
    fun `play - game status is changed when the game is started`() {
        assertEquals(GameSessionStatus.Created, newGame.status)

        newGame.startGame()

        val newStatus = newGame.status
        assertTrue(newStatus == GameSessionStatus.Player_1_Turn || newStatus == GameSessionStatus.Player_2_Turn)
        val gameUpdate = messages.last() as? GameStatusUpdate
        assertEquals(newStatus, gameUpdate?.newStatus)
    }

    @Test
    fun `play - validate no such player`() {
        newGame.startGame()
        newGame.play("who am i?")
        assertNull(messages.find { it is GameMessageUpdate })
    }

    @Test
    fun `play - game is not started`() {
        val currentPlayerId = if (newGame.status == GameSessionStatus.Player_1_Turn) player1.sessionId else player2.sessionId
        newGame.play(currentPlayerId)
        val gameUpdate = messages.last() as? GameMessageUpdate
        assertEquals("Game is not started yet", gameUpdate?.message)

    }

    @Test
    fun `play - end turn`() {
        newGame.startGame()
        val oldStatus = newGame.status
        val currentPlayerId = if (oldStatus == GameSessionStatus.Player_1_Turn) player1.sessionId else player2.sessionId
        // end turn
        newGame.play(currentPlayerId)
        assertNotEquals(oldStatus, newGame.status)
    }

    @Test
    fun `play - game is finished`() {
        newGame.startGame()
        val oldStatus = newGame.status
        val currentPlayerId = if (oldStatus == GameSessionStatus.Player_1_Turn) player1.sessionId else player2.sessionId
        newGame.status = GameSessionStatus.Player_2_Won
        messages.clear()

        newGame.play(currentPlayerId)
        val gameUpdate = messages.last() as? GameMessageUpdate
        assertEquals("Game is finished, start a new game", gameUpdate?.message)
    }

    @Test
    fun `play - not in turn`() {
        newGame.startGame()
        val oldStatus = newGame.status
        val currentPlayer = if (oldStatus == GameSessionStatus.Player_1_Turn) player1 else player2
        val wrongPlayer = if (oldStatus != GameSessionStatus.Player_1_Turn) player1 else player2
        messages.clear()

        newGame.play(wrongPlayer.sessionId)
        val gameUpdate = messages.last() as? GameMessageUpdate
        assertEquals("It is ${currentPlayer.name}'s turn!", gameUpdate?.message)
    }

    @Test
    fun `play - card does not exist`() {
        newGame.startGame()
        val oldStatus = newGame.status
        val currentPlayer = if (oldStatus == GameSessionStatus.Player_1_Turn) player1 else player2
        messages.clear()
        newGame.play(currentPlayer.sessionId, "whatever")
        val gameUpdate = messages.last() as? GameMessageUpdate
        assertEquals("Error! No such card!", gameUpdate?.message)
    }

    @Test
    fun `play - target not found`() {
        newGame.startGame()
        val oldStatus = newGame.status
        val currentPlayer = if (oldStatus == GameSessionStatus.Player_1_Turn) player1 else player2
        // skip turn
        messages.clear()
        newGame.play(currentPlayer.sessionId, currentPlayer.cards.values.first().id, "wherever")
        val gameUpdate = messages.last() as? GameMessageUpdate
        assertEquals("Target not found!", gameUpdate?.message)
    }

    @Test
    fun `play - card could not be played due to validation`() {
        newGame.startGame()
        newGame.status = GameSessionStatus.Player_1_Turn
        // skip turn
        messages.clear()
        newGame.play(player1.sessionId, "healing1")
        val playerUpdate = messages.last() as? GameMessageUpdate
        assertEquals("Health is already full", playerUpdate?.message)
    }

    @Test
    fun `play - play a spell card`() {
        newGame.startGame()
        newGame.status = GameSessionStatus.Player_2_Turn
        messages.clear()
        newGame.play(player2.sessionId, "fireball2")
        assertEquals(95, player1.getProperty(PlayerProperty.Health))
        val cardPlayed = messages.last() as? CardPlayed
        assertEquals("fireball2", cardPlayed?.cardId)
        assertEquals(true, cardPlayed?.discarded)
    }

    @Test
    fun `play - play a card with lasting effect`() {
        newGame.startGame()
        newGame.status = GameSessionStatus.Player_1_Turn
        player1.changeProperty(PlayerProperty.Health, -50)
        newGame.play(player1.sessionId, "healing1")
        newGame.play(player1.sessionId) //end turn
        newGame.play(player2.sessionId) //end turn
        newGame.play(player1.sessionId) //end turn

        assertEquals(52, player1.getProperty(PlayerProperty.Health))
        assertNotNull(
            messages.find { it is CardPlayed && it.cardId == "healing1" && it.discarded },
            "Card did not expire"
        )
    }

    @Test
    fun `play - play a card with temporary property change effect`() {
        newGame.startGame()
        newGame.status = GameSessionStatus.Player_1_Turn
        newGame.play(player1.sessionId, "iceShield1")
        assertEquals(15, player1.getProperty(PlayerProperty.ColdResist))
        newGame.play(player1.sessionId) //end turn 1
        newGame.play(player2.sessionId) //end turn 1
        assertEquals(15, player1.getProperty(PlayerProperty.ColdResist))
        newGame.play(player1.sessionId) //end turn 2
        assertEquals(0, player1.getProperty(PlayerProperty.ColdResist))
    }

    @Test
    fun `play - play a card with temporary property change effect update each turn`() {
        newGame.startGame()
        newGame.status = GameSessionStatus.Player_1_Turn
        newGame.play(player1.sessionId, "fireShield1")
        // no immediate change
        assertEquals(0, player1.getProperty(PlayerProperty.FireResist))
        newGame.play(player1.sessionId) //end turn 1 for player 1
        // effect is applied
        assertEquals(30, player1.getProperty(PlayerProperty.FireResist))

        newGame.play(player2.sessionId) //end turn 1 for player 2

        newGame.play(player1.sessionId) //end turn 2 for player 1
        // effect diminishes
        assertEquals(20, player1.getProperty(PlayerProperty.FireResist))
        newGame.play(player2.sessionId) //end turn 2 for player 2

        newGame.play(player1.sessionId) //end turn 3 for player 1
        // effect expired
        assertEquals(0, player1.getProperty(PlayerProperty.FireResist))

    }
}
