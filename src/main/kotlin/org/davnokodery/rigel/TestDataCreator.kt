package org.davnokodery.rigel

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class TestDataCreator(
    @Autowired val userRepository: UserRepository
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        userRepository.save(User("julia", "hello"))
        userRepository.save(User("daniil", "world"))
    }
}
