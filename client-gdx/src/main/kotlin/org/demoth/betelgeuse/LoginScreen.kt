package org.demoth.betelgeuse

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align

class LoginScreen(game: Starclient): StageScreen() {
    init {
        val table = Table(game.skin)
        table.setFillParent(true)
        table.add(Label("Name:", game.skin)).align(Align.right)
        val nameInput = TextField("", game.skin)
        table.add(nameInput).uniform()

        table.row()

        table.add(Label("Password:", game.skin)).align(Align.right)
        val passwordInput = TextField("", game.skin)
        passwordInput.setPasswordCharacter('*')
        passwordInput.isPasswordMode = true
        table.add(passwordInput)

        table.row()

        val loginButton = TextButton("Login", game.skin)
        loginButton.addListener(object: ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.login(nameInput.text, passwordInput.text)
            }
        })
        table.add(loginButton)
        val backButton = TextButton("Back", game.skin)
        backButton.addListener(object: ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = game.titleScreen
            }
        })
        table.add(backButton)

        stage.addActor(table)
    }
    
}