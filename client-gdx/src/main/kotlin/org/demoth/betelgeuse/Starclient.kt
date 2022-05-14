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
import org.springframework.web.socket.client.WebSocketClient

// create title screen - show MOTD + news: Image, text area, login button
// create login screen - usual login details
// create game list screen - list of games + descriptions, create new game button
// create game configuration screen - currently nothing, button create that sends create game
// create ingame screen - opponent cards(invisible), opponent effects, my effects, my cards
// create endgame screen - show score, return to game list screen

data class LoginRequest(val name: String, val password: String)

private const val SERVER_URL = "http://localhost:8080/login"

class Starclient : Game() {
    val jsonType = "application/json; charset=utf-8".toMediaTypeOrNull()
    private val mapper: ObjectMapper = jacksonObjectMapper()
    private val restClient = OkHttpClient()

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
        // todo: move to another thread
        println("Logging in with $username / $password")
        val loginRequest = LoginRequest(username, password)
        val request = Request.Builder()
            .post(mapper.writeValueAsString(loginRequest).toRequestBody(jsonType))
            .url(SERVER_URL)
            .build()
        val response = restClient.newCall(request).execute()
        val responseBody = response.body?.string()
        println("Response: $responseBody")
        if (response.isSuccessful) {
            val loginResponse: LoginResponse = mapper.readValue(responseBody!!)
            jwt = loginResponse.jwt
            this.username = loginResponse.username
            notify("Logged in as $username")
            println("Jwt token: $jwt")
            val wsclient = WsClient()
            wsclient.jwt = jwt
            wsclient.login()
            setScreen(gamesListScreen)
        } else {
            notify("Could not login")
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
