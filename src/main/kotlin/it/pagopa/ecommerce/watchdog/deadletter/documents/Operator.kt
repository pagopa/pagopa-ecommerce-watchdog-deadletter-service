package it.pagopa.ecommerce.watchdog.deadletter.documents

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "operators")
data class Operator(
    @Id val id: String,
    val password: String,
    val name: String,
    val surname: String,
    val email: String,
)
