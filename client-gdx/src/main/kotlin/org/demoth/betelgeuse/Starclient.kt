package org.demoth.betelgeuse

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import org.davnokodery.rigel.*
import org.davnokodery.rigel.model.LoginRequest

// create title screen - show MOTD + news: Image, text area, login button
// create login screen - usual login details
// create game list screen - list of games + descriptions, create new game button
// create game configuration screen - currently nothing, button create that sends create game
// create ingame screen - opponent cards(invisible), opponent effects, my effects, my cards
// create endgame screen - show score, return to game list screen

class Starclient : Game() {

    lateinit var skin: Skin
    lateinit var titleScreen: TitleScreen
    lateinit var loginScreen: LoginScreen
    lateinit var gamesListScreen: GamesListScreen

    private val restClient = RestClient()
    private var wsClient: WsClient? = null

    override fun create() {
        skin = Skin(Gdx.files.classpath("uiskin.json"))

        titleScreen = TitleScreen(this)
        loginScreen = LoginScreen(this)
        gamesListScreen = GamesListScreen(this)

        setScreen(titleScreen)
    }

    fun login(username: String, password: String) {
        // todo: move to another thread
        println("Logging in as $username...")
        restClient.login(LoginRequest(username, password)) {
            wsClient?.close()
            // todo: save jwt token
            wsClient = WsClient(it.jwt) {
                // run in the main rendering frame
                Gdx.app.postRunnable {
                    handleServerMessage(it)
                }
            }
            // todo: change screen only after successful login
            setScreen(gamesListScreen)
        }
    }
    private fun handleServerMessage(msg: ServerWsMessage) {
        when(msg) {
            is CardPlayed -> TODO()
            is GameMessageUpdate -> TODO()
            is GameStatusUpdate -> println("Game status: ${msg.newStatus}")
            is GamesListResponse -> gamesListScreen.setGames(msg.games)
            is NewCard -> TODO()
            is NewGameCreated -> gamesListScreen.addGame(msg.gameId)
            is PlayerPropertyChange -> TODO()
        }
    }

    fun createNewGame() {
        println("New game created")
        wsClient?.send(CreateGameMessage())
    }
    
    fun notify(text: String) {
        // todo: implement notifications (for example: https://github.com/wentsa/Toast-LibGDX)
        println(text)
    }
}
