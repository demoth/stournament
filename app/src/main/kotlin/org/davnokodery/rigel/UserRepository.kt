package org.davnokodery.rigel

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class User(
    @Id var name: String,
    var password: String
)

@Repository
interface UserRepository : CrudRepository<User, String>
