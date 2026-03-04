package it.pagopa.ecommerce.watchdog.deadletter.documents

import java.time.Instant
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "notes")
data class Note(
    @Id val id: String,
    val note: String,
    val transactionId: String,
    val userId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
