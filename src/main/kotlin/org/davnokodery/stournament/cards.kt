package org.davnokodery.stournament

import kotlin.math.min


private const val STRENGTH = "STRENGTH"

fun fireball() = Card("Fireball",
    "SpellBook01_84.PNG",
    "Deals X damage to the opponent",
    { self, target ->
        target.health -= 1
    },
    null,
    null,
    mutableMapOf("X" to 10)
).apply {
    effect = { self, target ->
        target.health -= params["X"]!!
    }
}

fun healing() = Card(
    "Healing",
    "SpellBookPage09_add_003.png",
    "Heals you for X points",
    { self, target ->
        self.health = min(self.maxHealth, self.health + 10)
    },
    { self, target ->
        if (self.health >= self.maxHealth)
            "Already at full health" else ""
    },
    null,
    mutableMapOf("X" to 10),
    2
)
