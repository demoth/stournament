package org.demoth.betelgeuse

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

// create title screen - show MOTD + news: Image, text area, login button
// create login screen - usual login details
// create game list screen - list of games + descriptions, create new game button
// create game configuration screen - currently nothing, button create that sends create game
// create ingame screen - opponent cards(invisible), opponent effects, my effects, my cards
// create endgame screen - show score, return to game list screen

data class LoginRequest(val name: String, val password: String)

class Starclient : Game() {
    val jsonType = "application/json; charset=utf-8"
    val mapper = jacksonObjectMapper()

    lateinit var skin: Skin
    lateinit var titleScreen: TitleScreen
    lateinit var loginScreen: LoginScreen
    lateinit var gamesListScreen: GamesListScreen

    var jwt = ""

    override fun create() {
        skin = Skin(Gdx.files.classpath("uiskin.json"))

        titleScreen = TitleScreen(this)
        loginScreen = LoginScreen(this)
        gamesListScreen = GamesListScreen(this)

        setScreen(titleScreen)
    }

    fun login(username: String, password: String) {
        println("Logging in with $username / $password")
        val loginRequest = LoginRequest(username, password)
        val client = OkHttpClient()
        val request = Request.Builder()
            .post(mapper.writeValueAsString(loginRequest)
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .url("http://localhost:8080/login")
            .build()
        val response = client.newCall(request).execute()
        println("Response: ${response.body?.string()}")
        if (response.isSuccessful) {
            setScreen(gamesListScreen)
        }
    }

    fun createNewGame() {
        println("New game created")
    }
}
