package it.pagopa.ecommerce.watchdog.deadletter.utils

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.event.Level

class StructuredLoggingTest {

    private val logger = LoggerFactory.getLogger("structured-logging-test") as Logger
    private val appender =
        object : ListAppender<ILoggingEvent>() {
            override fun append(eventObject: ILoggingEvent) {
                eventObject.prepareForDeferredProcessing()
                super.append(eventObject)
            }
        }.also {
            it.start()
            logger.addAppender(it)
        }

    @Test
    fun `emits fields as MDC entries only for the log call`() {
        val error = IllegalStateException("boom")

        logger.event(
            Level.WARN,
            "Rejected OTP code",
            LogFields.of(
                LogFields.ACTION_REFUND_TRANSACTION,
                LogFields.OUTCOME_FAILURE,
                "tx",
                details = mapOf("reason" to "wrong_code", "ignored" to null),
            ),
            error,
        )

        val event = appender.list.single()
        assertEquals("Rejected OTP code", event.formattedMessage)
        assertEquals(ch.qos.logback.classic.Level.WARN, event.level)
        assertEquals(
            "POST /v1/transactions/{transactionId}/refund",
            event.mdcPropertyMap["event_action"],
        )
        assertEquals("failure", event.mdcPropertyMap["event_outcome"])
        assertEquals("tx", event.mdcPropertyMap["ctx_transaction_id"])
        assertEquals("wrong_code", event.mdcPropertyMap["ctx_details.reason"])
        assertEquals(false, event.mdcPropertyMap.containsKey("ctx_user_id"))
        assertEquals(false, event.mdcPropertyMap.containsKey("ctx_details.ignored"))
        assertEquals("boom", event.throwableProxy.message)
        assertNull(MDC.get("event_action"))
    }
}
