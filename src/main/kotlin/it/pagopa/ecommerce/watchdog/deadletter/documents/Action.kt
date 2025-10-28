package it.pagopa.ecommerce.watchdog.deadletter.documents

import java.time.Instant
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "actions")
data class Action(
    @Id val id: String,
    val transactionId: String,
    val userId: String,
    val value: String,
    val timestamp: Instant,
)
