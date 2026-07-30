package it.pagopa.ecommerce.watchdog.deadletter.documents

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "notes")
data class DayStats(
    @Id val date: String,
    val finalized: Int,
    val notFinalized: Int,
    val notAnalyzed: Int?,
    @Version val version: Int,
)
