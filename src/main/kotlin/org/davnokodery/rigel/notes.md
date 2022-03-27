card
 * name
 * image
 * description
 * properties
 * tags
 * onApply()
 * onTick()
 * onExpire()

sessionPlayer:
 * cards
 * properties  
 * effects
 * changeProperty(name, value) -> notify client about updates
 * invoke(cardId, target)

persistentPlayer
 * properties
 * cards

GameSession - receives the commands from clients and updates the clients with responses
 * players
 * winCondition()


HTTP API

 * post /user/login
 * post /user/register
 * post /game
 * post /game/{gameid}/join
 * post /game/{gameid}/start

WebSocket API

commands:
 * playCard(jwt, cardId, target)
 * skipTurn(jwt)
 * invite(session)

updates:
 * playerPropertyUpdate(player, propertyName, delta)
 * playerTurnChange
 * gameFinished(standings)
 * inviteUpdate(joinSession)
