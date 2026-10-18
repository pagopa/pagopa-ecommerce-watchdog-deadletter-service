package it.pagopa.ecommerce.watchdog.deadletter.utils

import org.slf4j.Logger
import org.slf4j.MDC
import org.slf4j.event.Level

/** Structured, milestone-driven logging helpers (fields are emitted by the ECS encoder via MDC). */
object LogFields {
    const val OUTCOME_SUCCESS = "success"
    const val OUTCOME_FAILURE = "failure"

    const val ACTION_GET_TRANSACTION = "GET /v1/transactions/{transactionId}"
    const val ACTION_GET_TRANSACTION_STATUS = "GET /v1/transactions/{transactionId}/status"
    const val ACTION_REFUND_TRANSACTION = "POST /v1/transactions/{transactionId}/refund"

    const val DEPENDENCY_HELPDESK = "ecommerce-helpdesk"
    const val DEPENDENCY_HELPDESK_COMMANDS = "ecommerce-helpdesk-commands"
    const val DEPENDENCY_NODO = "nodo-technical-support"
    const val DEPENDENCY_MONGODB = "watchdog-mongodb"

    fun of(
        action: String,
        outcome: String? = null,
        transactionId: String? = null,
        userId: String? = null,
        details: Map<String, Any?> = emptyMap(),
    ): Map<String, Any?> =
        mapOf(
            "event_action" to action,
            "event_outcome" to outcome,
            "ctx_transaction_id" to transactionId,
            "ctx_user_id" to userId,
        ) + details.mapKeys { "ctx_details.${it.key}" }
}

/** Emits a single structured log line; null field values are omitted. */
fun Logger.event(
    level: Level,
    message: String,
    fields: Map<String, Any?>,
    error: Throwable? = null,
) {
    val entries =
        fields
            .filterValues { it != null }
            .map { (key, value) -> MDC.putCloseable(key, value.toString()) }
    try {
        atLevel(level).setCause(error).log(message)
    } finally {
        entries.forEach { it.close() }
    }
}
