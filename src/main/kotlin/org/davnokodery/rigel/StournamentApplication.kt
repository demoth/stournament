package org.davnokodery.rigel

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StournamentApplication

fun main(args: Array<String>) {
	runApplication<StournamentApplication>(*args)
}
