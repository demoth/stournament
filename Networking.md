# Stournament networking (WIP)

Communication between client and server happens with both synchronous (REST interface) and asynchronous (Websocket) messaging

## Login

POST `/login`
response: jwt token used in both websocket & rest requests

After logging in clients establish a websocket on `/web-socket` url and should send JwtMessage with the jwt token as the first message.

## New game creation

### POST `/newgame`
response: game id + configuration parameters for the game.

Client can switch to the `GameConfigurationScreen`.

Other clients receive a `NewGameCreated(gameId)` websocket message.

### POST `/join`
response: game id + configuration parameters for the game.
Client can switch to the new `GameConfigurationScreen`.

The other player receives `PlayerJoined(...)` websocket message


### Game configuration screen

POST `/game-config`
At the `GameConfigurationScreen` players can change some parameters.
All updates are communicated with a websocket message `GameConfigurationUpdate(todo: send whole config map or only 1 change?)`

### POST `/ready`
The other player receives `PlayerReady(player, status)` websocket message

Both players should ready themselves before the game can be started.
At any game configuration change players ready status is reset.
