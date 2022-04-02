# Draft model classes

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

---

# Card examples

 * Damage: Fire, Cold, Lightning, Physical, Poison, Holy, Unholy
 * Types: Spell, Curse, Enchantment, Summon, Item
 * Creatures: Elemental, Animal, Demon, Undead, 

# Card effects api:

 * change player property
 * deal damage / heal
 * add/remove card (by target, tag or random)
 * add/remove effect (by target, tag or random)
 * change effect property

 * random property change

 * getHealth() = base_health * getIncreased() + getFlat_health()
