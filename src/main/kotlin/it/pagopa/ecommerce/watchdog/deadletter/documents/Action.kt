package it.pagopa.ecommerce.watchdog.deadletter.documents

import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ActionTypeDto
import java.time.Instant
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "actions")
data class Action(
    @Id val id: String,
    val transactionId: String,
    val userId: String,
    val action: ActionTypeDto,
    val timestamp: Instant,
)
