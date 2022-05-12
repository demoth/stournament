package org.davnokodery.rigel.model

import io.swagger.annotations.ApiModelProperty

enum class GameSessionStatus {
    Created,
    Player_1_Turn,
    Player_2_Turn,
    Player_1_Won,
    Player_2_Won;

    fun started(): Boolean {
        return this == Player_1_Turn || this == Player_2_Turn
    }

    fun finished(): Boolean {
        return this == Player_1_Won || this == Player_2_Won
    }
}

/**
 * What is sent to the client
 */
data class CardData(
    val id: String,
    val name: String,
    val iconName: String,
    val description: String,
    val tags: Set<String> = hashSetOf()
)

data class LoginRequest(
    @ApiModelProperty("User name")
    val name: String,

    @ApiModelProperty(
        value = "User password",
        notes = "FIXME: don't use plain text password"
    )
    val password: String
)

data class LoginResponse(
    @ApiModelProperty(
        value = "Json Web token",
        notes = "A valid jwt token is required for WebSocket connection initialization and for other REST calls"
    )
    val jwt: String,
    val username: String
)
