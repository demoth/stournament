package org.davnokodery.rigel

import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class LoginController(@Autowired val authService: AuthService) {

    @ApiOperation(value = "Request a jwt token")
    @PostMapping("/login")
    fun login(@RequestBody login: LoginRequest): LoginResponse {
        return authService.loginUser(login.name, login.password)
    }
}

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
