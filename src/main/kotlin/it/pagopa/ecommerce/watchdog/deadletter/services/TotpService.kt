package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidOtpException
import it.pagopa.ecommerce.watchdog.deadletter.repositories.OperatorsRepository
import it.pagopa.ecommerce.watchdog.deadletter.utils.LogFields
import it.pagopa.ecommerce.watchdog.deadletter.utils.Totp
import it.pagopa.ecommerce.watchdog.deadletter.utils.event
import java.time.Clock
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class TotpService(
    private val totpSecretsProvider: TotpSecretsProvider,
    private val operatorsRepository: OperatorsRepository,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Verifies [code] against the operator's TOTP secret and consumes it, so the same code cannot
     * be used twice. [transactionId] is used only for log correlation.
     *
     * @return an empty [Mono] on success, an [InvalidOtpException] error otherwise
     */
    fun verifyAndConsume(operatorId: String, code: String, transactionId: String): Mono<Void> =
        totpSecretsProvider
            .getSecret(operatorId)
            .switchIfEmpty(
                Mono.defer { reject(operatorId, transactionId, "secret_not_configured") }
            )
            .flatMap { secret ->
                val step =
                    try {
                        Totp.matchingTimeStep(secret, code, clock.instant())
                    } catch (e: IllegalArgumentException) {
                        return@flatMap reject(operatorId, transactionId, "malformed_secret")
                    } ?: return@flatMap reject(operatorId, transactionId, "wrong_code")
                consumeTimeStep(operatorId, step, transactionId)
            }

    // Atomic update: succeeds only if the step is newer than the last accepted one.
    private fun consumeTimeStep(operatorId: String, step: Long, transactionId: String): Mono<Void> {
        val details =
            mapOf("dependency" to LogFields.DEPENDENCY_MONGODB, "collection" to "operators")
        return operatorsRepository
            .updateLastTotpTimeStepIfNewer(operatorId, step)
            .doOnError {
                logger.event(
                    Level.ERROR,
                    "Failed to consume OTP code",
                    fields(operatorId, transactionId, LogFields.OUTCOME_FAILURE, details),
                    it,
                )
            }
            .flatMap { result ->
                if (result == 1L) {
                    logger.event(
                        Level.INFO,
                        "Consumed OTP code",
                        fields(operatorId, transactionId, LogFields.OUTCOME_SUCCESS, details),
                    )
                    Mono.empty()
                } else {
                    reject(operatorId, transactionId, "code_already_used", details)
                }
            }
    }

    private fun <T> reject(
        operatorId: String,
        transactionId: String,
        reason: String,
        details: Map<String, Any?> = emptyMap(),
    ): Mono<T> {
        logger.event(
            Level.WARN,
            "Rejected OTP code",
            fields(
                operatorId,
                transactionId,
                LogFields.OUTCOME_FAILURE,
                details + ("reason" to reason),
            ),
        )
        return Mono.error(InvalidOtpException())
    }

    private fun fields(
        operatorId: String,
        transactionId: String,
        outcome: String,
        details: Map<String, Any?>,
    ) =
        LogFields.of(
            LogFields.ACTION_REFUND_TRANSACTION,
            outcome,
            transactionId,
            operatorId,
            details,
        )
}
