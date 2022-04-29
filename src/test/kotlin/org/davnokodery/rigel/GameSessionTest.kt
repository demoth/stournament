package org.davnokodery.rigel

import org.davnokodery.rigel.TestDataCreator.Companion.testUser1
import org.davnokodery.rigel.TestDataCreator.Companion.testUser2
import org.davnokodery.rigel.model.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


internal class GameSessionTest {

    private lateinit var player1: SessionPlayer
    private lateinit var player2: SessionPlayer
    private lateinit var newGame: GameSession
    private val messages = mutableListOf<ServerWsMessage>()
    private val sender = MessageSender { messages.add(it) }
    
    @BeforeEach
    fun setup() {
        player1 = createTestPlayer(testUser1.name)
        player2 = createTestPlayer(testUser2.name)
        newGame = GameSession("id", player1, sender, provingGroundsRules())
        newGame.player2 = player2
        messages.clear()
    }

    private fun createTestPlayer(name: String) = SessionPlayer(sessionId = "$name id", name = name, sender = sender)

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
        assertTrue(newStatus.started())
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
        newGame.play(currentPlayer.sessionId, currentPlayer.findCardByName(FIRE_BALL_NAME)?.id, "wherever")
        val gameUpdate = messages.last() as? GameMessageUpdate
        assertEquals("Target not found!", gameUpdate?.message)
    }

    @Test
    fun `play - card could not be played due to validation`() {
        newGame.startGame()
        newGame.status = GameSessionStatus.Player_1_Turn
        // skip turn
        messages.clear()
        newGame.play(player1.sessionId, player1.findCardByName(HEALING_NAME)?.id)
        val playerUpdate = messages.last() as? GameMessageUpdate
        assertEquals("Health is already full", playerUpdate?.message)
    }

    @Test
    fun `play - play a spell card`() {
        newGame.startGame()
        newGame.status = GameSessionStatus.Player_2_Turn
        messages.clear()
        val fireballId = player2.findCardByName(FIRE_BALL_NAME)?.id
        newGame.play(player2.sessionId, fireballId)
        assertEquals(95, player1.getProperty(PROP_HEALTH))
        val cardPlayed = messages.last() as? CardPlayed
        assertEquals(fireballId, cardPlayed?.cardId)
        assertEquals(true, cardPlayed?.discarded)
    }

    @Test
    fun `play - play a card with lasting effect`() {
        newGame.startGame()
        newGame.status = GameSessionStatus.Player_1_Turn
        player1.changeProperty(PROP_HEALTH, -50)
        val healingId = player1.findCardByName(HEALING_NAME)?.id
        newGame.play(player1.sessionId, healingId)
        newGame.play(player1.sessionId) //end turn
        newGame.play(player2.sessionId) //end turn
        newGame.play(player1.sessionId) //end turn

        assertEquals(52, player1.getProperty(PROP_HEALTH))
        assertNotNull(
            messages.find { it is CardPlayed && it.cardId == healingId && it.discarded },
            "Card did not expire"
        )
    }

    @Test
    fun `play - play a card with temporary property change effect`() {
        newGame.startGame()
        newGame.status = GameSessionStatus.Player_1_Turn
        newGame.play(player1.sessionId, player1.findCardByName(ICE_SHIELD_NAME)?.id)
        assertEquals(15, player1.getProperty(PROP_COLD_RESIST))
        newGame.play(player1.sessionId) //end turn 1
        newGame.play(player2.sessionId) //end turn 1
        assertEquals(15, player1.getProperty(PROP_COLD_RESIST))
        newGame.play(player1.sessionId) //end turn 2
        assertEquals(0, player1.getProperty(PROP_COLD_RESIST))
    }

    @Test
    fun `play - play a card with temporary property change effect update each turn`() {
        newGame.startGame()
        newGame.status = GameSessionStatus.Player_1_Turn
        newGame.play(player1.sessionId, player1.findCardByName(FIRE_SHIELD_NAME)?.id)
        // no immediate change
        assertEquals(0, player1.getProperty(PROP_FIRE_RESIST))
        newGame.play(player1.sessionId) //end turn 1 for player 1
        // effect is applied
        assertEquals(30, player1.getProperty(PROP_FIRE_RESIST))

        newGame.play(player2.sessionId) //end turn 1 for player 2

        newGame.play(player1.sessionId) //end turn 2 for player 1
        // effect diminishes
        assertEquals(20, player1.getProperty(PROP_FIRE_RESIST))
        newGame.play(player2.sessionId) //end turn 2 for player 2

        newGame.play(player1.sessionId) //end turn 3 for player 1
        // effect expired
        assertEquals(0, player1.getProperty(PROP_FIRE_RESIST))

    }
    
}
