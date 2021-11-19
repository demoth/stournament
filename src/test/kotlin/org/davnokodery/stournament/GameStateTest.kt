package org.davnokodery.stournament

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

internal class GameStateTest {

    lateinit var testState: GameState

    @BeforeEach
    fun setup() {
        val player1 = Player("player1").apply {
            repeat(10) {
                fireball().let { cards[it.id] = it }
            }

        }

        val player2 = Player("player2").apply {
            cards.clear()
            repeat(10) {
                healing().let { cards[it.id] = it }
            }
        }
        testState = GameState(player1 = player1, player2 = player2)
    }

    @Test
    fun `play a card`() {
        testState.performAction(1, testState.player1.cards.keys.first())
        assertEquals(0, testState.player1.effects.size)
        assertEquals(GameStatus.PLAYER_2_TURN, testState.status)
    }

    @Test
    fun `play a long lasting effect`() {
        testState.performAction(1, testState.player1.cards.keys.first())
        testState.performAction(2, testState.player2.cards.keys.first())

        assertEquals(1, testState.player2.effects.size)
        assertEquals(GameStatus.PLAYER_1_TURN, testState.status)
    }

    @Test
    fun `skip a turn`() {
        testState.performAction(1, null)
        assertEquals(GameStatus.PLAYER_2_TURN, testState.status)
    }

    @Test
    fun `not your turn`() {
        testState.performAction(2, null)
        assertEquals(GameStatus.PLAYER_1_TURN, testState.status)
        assertEquals("Not your turn", testState.message)
    }

    @Test
    fun `no such card`() {
        testState.performAction(1, "whatever")
        assertEquals(GameStatus.PLAYER_1_TURN, testState.status)
        assertEquals("No such card", testState.message)
    }
}
