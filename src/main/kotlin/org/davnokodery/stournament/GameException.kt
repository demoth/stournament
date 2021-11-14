package org.davnokodery.stournament

import org.springframework.http.HttpStatus

class GameException(override val message: String, val httpStatus: HttpStatus = HttpStatus.BAD_REQUEST) : Exception(message)
