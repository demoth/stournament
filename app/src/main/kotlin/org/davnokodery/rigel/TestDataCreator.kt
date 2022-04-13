package org.davnokodery.rigel

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component


@Component
@Profile("test-data")
class TestDataCreator(
    @Autowired val userRepository: UserRepository
) : CommandLineRunner {
    companion object {
        val testUser1 = User("julia", "hello")
        val testUser2 = User("daniil", "world")
    }
    private val logger = LoggerFactory.getLogger(TestDataCreator::class.java)

    override fun run(vararg args: String?) {
        userRepository.save(testUser1)
        logger.debug("Added test user $testUser1")
        userRepository.save(testUser2)
        logger.debug("Added test user $testUser2")
    }
}
