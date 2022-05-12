package org.davnokodery.rigel

import io.swagger.annotations.ApiOperation
import org.davnokodery.rigel.model.LoginRequest
import org.davnokodery.rigel.model.LoginResponse
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
