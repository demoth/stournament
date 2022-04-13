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

 * getHealth() = base_health * getIncreased(1 + 0.01 + 0.02 + (-0.01)) + getFlat_health()


    //
    //val fireballEffect = CardAction { card, owner, enemy, targetEffect ->
    //    enemy.changeProperty(PlayerProperty.Health, -5) // todo: damage calculation, // add delta instead of direct manipulation // add card id
    //    enemy.getProperty(PlayerProperty.Health)
    //}
    ///*
    //on expire -> enemy.expirePropertyChange(card.id)
    // */
    //
    //Player {
    //    fun applyDamage(amount: Int) {
    //        // apply damage?
    //        listeners.forEach {it.notify(damage.amount)}
    //
    //    }
    //}
    //
    //val coldShieldEffect = CardAction { card, owner, enemy, targetEffect ->
    //    owner.changeTemporaryProperty(PlayerProperty.ColdResist, +15, ttl: 3)
    //    owner.applyEffect(card)
    //    owner.addListener(
    //        //broadcastDamage()
    //    )
    //}
    //
    //Card {
    //    name = "leech",
    //    playEffect = ...,
    //
    //}
    //
    //val leechEffect = CardAction { card, owner, enemy, targetEffect ->
    //    enemy.addPropertyListener(
    //        PlayerProperty.Health,
    //        health, delta ->
    //            if (delta > 0) {
    //                owner.permanentlyChangeProperty(PlayerProperty.Health, delta / 2, card.id)
    //            }
    //
    //    )
    //    owner.changeTemporaryProperty(PlayerProperty.ColdResist, +15, ttl: 3)
    //    owner.applyEffect(card)
    //    owner.addListener(
    //        //broadcastDamage()
    //    )
    //}
    //fun getSeed(): Long {
    ////    if (mode == Test){
    ////        return 42
    ////    }
    //    return Random().nextLong()
    //}
    //
    //fun myRandom() = Random(getSeed())
    //
    //val lightningStrike = CardAction { card, owner, enemy, targetEffect ->
    //    enemy.changeProperty(PlayerProperty.Health, myRandom().nextInt(-10, -1))
    //}
    //
