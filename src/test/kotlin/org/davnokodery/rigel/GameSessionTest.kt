package org.davnokodery.rigel

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

internal class GameSessionTest {

    lateinit var player1: SessionPlayer
    lateinit var player2: SessionPlayer
    lateinit var newGame: GameSession

    @BeforeEach
    fun setup() {

        val testCard1 = Card(
            id = "invalid1",
            name = "test1",
            iconName = "test1 icon",
            description = "test1 description",
            validator = Validator { _, _, _, _ ->
                "Not allowed"
            }
        )
        val testCard2 = Card(id = "valid2", name = "test2", iconName = "test2 icon", description = "test2 description")
        player1 = SessionPlayer("player1").apply {
            cards[testCard1.id] = testCard1
        }
        player2 = SessionPlayer("player2").apply {
            cards[testCard2.id] = testCard2

        }
        newGame = GameSession(player1, player2)
    }

    @AfterEach
    fun printUpdates() {
        newGame.updates.forEach {
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
    fun `play - skip turn`() {
        newGame.startGame()
        val oldStatus = newGame.status
        val currentPlayerName = if (oldStatus == GameSessionStatus.Player_1_Turn) player1.name else player2.name
        // skip turn
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
    fun `play - card could not be played`() {
        newGame.startGame()
        newGame.status = GameSessionStatus.Player_1_Turn
        // skip turn
        newGame.play(player1.name, "invalid1")
        val gameUpdate = player1.updates.poll() as? GameMessageUpdate
        assertEquals("Not allowed", gameUpdate?.message)
    }


}
