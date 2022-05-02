package org.davnokodery.rigel

import io.swagger.annotations.Api
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid
import javax.validation.constraints.Min

@RestController()
@CrossOrigin
@Api("User controller", basePath = "/user")
@RequestMapping("/user")
class UserController(
    @Autowired val authService: AuthService,
    @Autowired val userRepository: UserRepository
    ) {

    @ApiOperation(value = "Request a jwt token")
    @PostMapping("/login")
    fun login(@Valid @RequestBody login: LoginRequest): LoginResponse {
        return authService.loginUser(login.name!!, login.password)
    }

/*
    @ApiOperation(value = "Register a new user")
    @PostMapping("/register")
    fun register(@RequestBody request: LoginRequest): ResponseEntity<String> {
        return if (userRepository.existsById(request.name)) {
            ResponseEntity("User already exists", HttpStatus.BAD_REQUEST)
        } else {
            userRepository.save(User(request.name, request.password))
            ResponseEntity("Created", HttpStatus.CREATED)
        }
    }
*/
}

data class LoginRequest(
    @ApiModelProperty("User name")
    @field:Min(5)
    val name: String?,
    
    @ApiModelProperty(value = "User password")
    val password: String
)

data class LoginResponse(
    @ApiModelProperty(
        value = "Json Web token",
        notes = "A valid jwt token is required for WebSocket connection initialization and for other REST calls"
    )
    val jwt: String
)
