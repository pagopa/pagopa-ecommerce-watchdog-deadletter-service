package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidOtpException
import it.pagopa.ecommerce.watchdog.deadletter.exception.TooManyOtpAttemptsException
import it.pagopa.ecommerce.watchdog.deadletter.repositories.OperatorsRepository
import it.pagopa.ecommerce.watchdog.deadletter.utils.LogFields
import it.pagopa.ecommerce.watchdog.deadletter.utils.Totp
import it.pagopa.ecommerce.watchdog.deadletter.utils.event
import java.time.Clock
import java.time.Duration
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class TotpService(
    private val totpSecretsProvider: TotpSecretsProvider,
    private val operatorsRepository: OperatorsRepository,
    private val clock: Clock,
    @Value("\${auth.totp.max-attempts:5}") private val maxAttempts: Int = 5,
    @Value("\${auth.totp.lock-duration-minutes:15}") private val lockDurationMinutes: Long = 15,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val mongoDetails =
        mapOf("dependency" to LogFields.DEPENDENCY_MONGODB, "collection" to "operators")

    /**
     * Verifies [code] against the operator's TOTP secret and consumes it, so the same code cannot
     * be used twice. After [maxAttempts] consecutive failures the operator is locked for
     * [lockDurationMinutes]. [transactionId] is used only for log correlation.
     *
     * @return an empty [Mono] on success, an [InvalidOtpException] or a
     *   [TooManyOtpAttemptsException] error otherwise
     */
    fun verifyAndConsume(operatorId: String, code: String, transactionId: String): Mono<Void> =
        totpSecretsProvider
            .getSecret(operatorId)
            .switchIfEmpty(
                Mono.defer { reject(operatorId, transactionId, "secret_not_configured") }
            )
            .flatMap { secret ->
                reserveAttempt(operatorId, transactionId)
                    .then(Mono.defer { verify(operatorId, secret, code, transactionId) })
            }

    // Atomic: counts the attempt before verifying, so concurrent requests cannot bypass the limit.
    private fun reserveAttempt(operatorId: String, transactionId: String): Mono<Void> =
        operatorsRepository
            .reserveTotpAttempt(operatorId, clock.instant(), maxAttempts)
            .doOnError {
                logMongoError("Failed to reserve OTP attempt", operatorId, transactionId, it)
            }
            .flatMap { reserved ->
                if (reserved == 1L) {
                    Mono.empty()
                } else {
                    logger.event(
                        Level.WARN,
                        "Rejected OTP code",
                        fields(
                            operatorId,
                            transactionId,
                            LogFields.OUTCOME_FAILURE,
                            mongoDetails + ("reason" to "too_many_attempts"),
                        ),
                    )
                    Mono.error(TooManyOtpAttemptsException())
                }
            }

    private fun verify(
        operatorId: String,
        secret: String,
        code: String,
        transactionId: String,
    ): Mono<Void> {
        val step =
            try {
                Totp.matchingTimeStep(secret, code, clock.instant())
            } catch (e: IllegalArgumentException) {
                return registerFailure(operatorId, transactionId, "malformed_secret")
            } ?: return registerFailure(operatorId, transactionId, "wrong_code")
        return consumeTimeStep(operatorId, step, transactionId)
    }

    // Atomic update: succeeds only if the step is newer than the last accepted one, and resets
    // the failed attempts counter.
    private fun consumeTimeStep(operatorId: String, step: Long, transactionId: String): Mono<Void> =
        operatorsRepository
            .updateLastTotpTimeStepIfNewer(operatorId, step)
            .doOnError {
                logMongoError("Failed to consume OTP code", operatorId, transactionId, it)
            }
            .flatMap { result ->
                if (result == 1L) {
                    logger.event(
                        Level.INFO,
                        "Consumed OTP code",
                        fields(operatorId, transactionId, LogFields.OUTCOME_SUCCESS, mongoDetails),
                    )
                    Mono.empty()
                } else {
                    registerFailure(operatorId, transactionId, "code_already_used")
                }
            }

    // The attempt was already counted by reserveAttempt: lock the operator if the limit is reached.
    private fun registerFailure(
        operatorId: String,
        transactionId: String,
        reason: String,
    ): Mono<Void> {
        val lockedUntil = clock.instant().plus(Duration.ofMinutes(lockDurationMinutes))
        return operatorsRepository
            .lockTotpIfTooManyAttempts(operatorId, maxAttempts, lockedUntil)
            .doOnError { logMongoError("Failed to lock OTP", operatorId, transactionId, it) }
            .doOnNext { locked ->
                if (locked == 1L) {
                    logger.event(
                        Level.WARN,
                        "Operator locked for too many OTP attempts",
                        fields(
                            operatorId,
                            transactionId,
                            LogFields.OUTCOME_FAILURE,
                            mongoDetails + ("lockedUntil" to lockedUntil.toString()),
                        ),
                    )
                }
            }
            .then(reject(operatorId, transactionId, reason, mongoDetails))
    }

    private fun <T> reject(
        operatorId: String,
        transactionId: String,
        reason: String,
        details: Map<String, Any?> = emptyMap(),
    ): Mono<T> =
        Mono.defer {
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
            Mono.error(InvalidOtpException())
        }

    private fun logMongoError(
        message: String,
        operatorId: String,
        transactionId: String,
        error: Throwable,
    ) =
        logger.event(
            Level.ERROR,
            message,
            fields(operatorId, transactionId, LogFields.OUTCOME_FAILURE, mongoDetails),
            error,
        )

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
