package org.davnokodery.stournament

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class GameController {
    val gameState = GameState()

    @GetMapping("/state")
    fun getState(): GameState {
        return gameState;
    }

    @PostMapping("/action")
    fun postAction(
        @RequestParam("player") player: Int,
        @RequestParam("card") cardId: String
    ): GameState {
        gameState.performAction(player, cardId)
        return gameState
    }
}
