package org.davnokodery.rigel

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import org.davnokodery.rigel.model.LoginResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuthService(@Autowired private val userRepository: UserRepository) {
    private val ISSUER = "rigel"

    private val JWT_SECRET = System.getenv("JWT_SECRET") ?: UUID.randomUUID().toString()

    private val VERIFIER: JWTVerifier = JWT.require(Algorithm.HMAC256(JWT_SECRET)).withIssuer(ISSUER).build()

    /**
     * validates jwt token and returns user associated with this token
     */
    fun validateToken(authHeader: String): User {
        val (type, token) = authHeader.split(" ")
        if (type != "Bearer")
            throw GameException("Authentication $type is not supported", HttpStatus.BAD_REQUEST)
        val userName: String = try {
            VERIFIER.verify(token).subject
        } catch (e: Exception) {
            throw GameException("Invalid token", HttpStatus.UNAUTHORIZED)
        }
        return userRepository.findById(userName).orElseGet {
            throw GameException("No such user", HttpStatus.NOT_FOUND)
        }
    }

    /**
     * @return token after successful identification and authentication
     */
    fun loginUser(name: String, password: String): LoginResponse {
        val user = userRepository.findById(name).orElseThrow {
            throw GameException("No such user", HttpStatus.NOT_FOUND)
        }

        if (user.password != password) {
            throw GameException("Wrong password", HttpStatus.UNAUTHORIZED)
        }

        try {
            return LoginResponse(JWT.create()
                .withIssuer(ISSUER)
                .withSubject(user.name)
                .sign(Algorithm.HMAC256(JWT_SECRET)), user.name)
        } catch (e: Exception) {
            throw GameException("Could not authenticate", HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}
