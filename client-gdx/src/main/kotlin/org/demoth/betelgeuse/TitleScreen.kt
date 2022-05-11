package org.demoth.betelgeuse

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextArea
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

class TitleScreen(game: Starclient): StageScreen() {

    init {
        val table = Table(game.skin)
        table.setFillParent(true)
        //table.add(Image(Texture("card-joker.png")))
        table.add(TextArea("Lorem ipsum \ndolor sit amet", game.skin)).fill()
        val loginButton = TextButton("Login", game.skin)
        loginButton.addListener(object: ClickListener(){
            override fun clicked(event: InputEvent, x: Float, y: Float) {
                game.screen = game.loginScreen
            }
        })
        table.add(loginButton)
        stage.addActor(table)
    }
}
