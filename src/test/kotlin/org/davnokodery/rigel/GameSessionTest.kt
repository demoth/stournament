package org.davnokodery.rigel

import org.davnokodery.stournament.GameException
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import javax.websocket.Session

internal class GameSessionTest {

    lateinit var player1: SessionPlayer
    lateinit var player2: SessionPlayer
    lateinit var newGame: GameSession

    @BeforeEach
    fun setup() {
        player1 = SessionPlayer("player1")
        player2 = SessionPlayer("player2")
        newGame = GameSession(player1, player2)
    }

    @Test
    fun startGame() {
        assertEquals(GameSessionStatus.Created, newGame.status)

        newGame.startGame()

        val newStatus = newGame.status
        assertTrue(newStatus == GameSessionStatus.Player_1_Turn || newStatus == GameSessionStatus.Player_2_Turn)
        val gameUpdate = newGame.updates.poll() as? GameStatusUpdate
        assertEquals(newStatus, gameUpdate?.newStatus)
    }

    @Test
    fun `play - validate no such player`() {
        newGame.startGame()
        assertThrows(GameException::class.java) {
            newGame.play("who am i?")
        }
    }

    @Test
    fun `play - validate game state != created`() {
        assertThrows(GameException::class.java) {
            newGame.play(if (newGame.status == GameSessionStatus.Player_1_Turn) player1.name else player2.name )
        }
    }
}
