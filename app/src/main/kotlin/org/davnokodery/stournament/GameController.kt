package org.davnokodery.stournament

import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
class GameController {
    var gameState = GameState()

    @GetMapping("/state")
    fun getState(): GameState {
        return gameState;
    }

    @PostMapping("/action")
    fun postAction(
        @RequestParam("player") player: Int,
        @RequestParam("card") cardId: String?
    ): GameState {
        gameState.performAction(player, cardId)
        return gameState
    }
}
