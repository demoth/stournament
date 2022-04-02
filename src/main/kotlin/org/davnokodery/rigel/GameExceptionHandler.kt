package org.davnokodery.rigel

import org.davnokodery.stournament.ErrorResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GameExceptionHandler {
    @ExceptionHandler(GameException::class)
    fun handleAllExceptions(e: GameException): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ErrorResponse(e.message), e.httpStatus)
    }
}
