package org.davnokodery.rigel

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test-data")
class StournamentApplicationTests {

	@Autowired lateinit var userSessionManager: UserSessionManager

	@Test
	fun contextLoads() {

	}

}
