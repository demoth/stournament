package org.demoth.betelgeuse

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener


class GamesListScreen(game: Starclient) : StageScreen() {

    private var gameList: List<String>
    init {
        val table = Table(game.skin)
        table.setFillParent(true)

        val newGameButton = TextButton("Create new game", game.skin)
        newGameButton.addListener(object: ClickListener() {
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                game.createNewGame()
            }
        })
        table.add(newGameButton)
        table.row()

        table.add(Label("Existing games:", game.skin))
        table.row()

        gameList = List<String>(game.skin)
        gameList.setItems()

        table.add(gameList)

        stage.addActor(table)
    }

    fun setGames (games: Collection<String>) {
        val gameIds = com.badlogic.gdx.utils.Array<String>(games.size)
        games.forEach { gameIds.add(it) }
        gameList.clear()
        gameList.setItems(gameIds)
    }

    fun addGame(gameId: String) {
        gameList.items.add(gameId)
        gameList.setItems(gameList.items)
    }
}
