package org.davnokodery.rigel

import org.davnokodery.rigel.TestDataCreator.Companion.testUser1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity

import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-data")
class LoginIntegrationTest {
    @LocalServerPort
    private var port: Int = 0
    private var restTemplate = TestRestTemplate()

    private val url
        get() = "http://localhost:$port"

    @Test
    fun `login - not successful`() {
        val loginResponse = restTemplate.postForEntity<GameException>(
            "$url/login",
            LoginRequest("noone", "whatever")
        )
        assertEquals(HttpStatus.NOT_FOUND, loginResponse.statusCode)
    }

    @Disabled
    @Test
    fun `login - wrong password`() {
        val loginResponse = restTemplate.postForEntity<GameException>(
            "$url/login",
            LoginRequest(testUser1.name, "whatever")
        )
        assertEquals(HttpStatus.UNAUTHORIZED, loginResponse)
    }

    @Test
    fun `login - successful`() {
        val loginResponse = restTemplate.postForEntity<LoginResponse>(
            "$url/login",
            LoginRequest(testUser1.name, testUser1.password)
        )
        assertEquals(HttpStatus.OK, loginResponse.statusCode)
    }
}
