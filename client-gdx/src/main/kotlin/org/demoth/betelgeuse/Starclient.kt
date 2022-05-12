package org.demoth.betelgeuse

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.davnokodery.rigel.model.LoginResponse

// create title screen - show MOTD + news: Image, text area, login button
// create login screen - usual login details
// create game list screen - list of games + descriptions, create new game button
// create game configuration screen - currently nothing, button create that sends create game
// create ingame screen - opponent cards(invisible), opponent effects, my effects, my cards
// create endgame screen - show score, return to game list screen

data class LoginRequest(val name: String, val password: String)

class Starclient : Game() {
    val jsonType = "application/json; charset=utf-8"
    private val mapper: ObjectMapper = jacksonObjectMapper()

    lateinit var skin: Skin
    lateinit var titleScreen: TitleScreen
    lateinit var loginScreen: LoginScreen
    lateinit var gamesListScreen: GamesListScreen

    var jwt = ""
    var username = ""

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
            val loginResponse: LoginResponse = mapper.readValue(response.body?.string()!!)
            jwt = loginResponse.jwt
            setScreen(gamesListScreen)
        }
    }

    fun createNewGame() {
        println("New game created")
    }
    
    fun notify(text: String) {
        // todo: implement notifications (for example: https://github.com/wentsa/Toast-LibGDX)
        println(text)
    }
}
