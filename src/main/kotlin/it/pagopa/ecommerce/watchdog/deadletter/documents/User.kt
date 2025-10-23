package it.pagopa.ecommerce.watchdog.deadletter.documents

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "users")
data class User(
    @Id val id: String,
    val username: String,
    val password: String,
    val name: String,
    val surname: String,
    val email: String,
)
