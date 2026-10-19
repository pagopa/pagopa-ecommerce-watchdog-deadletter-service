package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidOtpException
import it.pagopa.ecommerce.watchdog.deadletter.exception.TooManyOtpAttemptsException
import it.pagopa.ecommerce.watchdog.deadletter.repositories.OperatorsRepository
import it.pagopa.ecommerce.watchdog.deadletter.utils.Totp
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class TotpServiceTest {

    private val secret = "JBSWY3DPEHPK3PXP"
    private val now = Instant.ofEpochSecond(1_700_000_000)
    private val maxAttempts = 5
    private val lockMinutes = 15L
    private val lockedUntil = now.plus(Duration.ofMinutes(lockMinutes))
    private val secretsProvider: TotpSecretsProvider = mock()
    private val operatorsRepository: OperatorsRepository = mock()
    private val service =
        TotpService(
            secretsProvider,
            operatorsRepository,
            Clock.fixed(now, ZoneOffset.UTC),
            maxAttempts,
            lockMinutes,
        )

    private val validCode = Totp.generate(Totp.decodeBase32(secret), Totp.timeStep(now))
    private val wrongCode = if (validCode == "000000") "111111" else "000000"

    @BeforeEach
    fun setUp() {
        whenever(secretsProvider.getSecret("op")).thenReturn(Mono.just(secret))
        givenAttemptReserved(true)
    }

    private fun givenAttemptReserved(reserved: Boolean) {
        whenever(operatorsRepository.reserveTotpAttempt("op", now, maxAttempts, lockedUntil))
            .thenReturn(Mono.just(if (reserved) 1L else 0L))
    }

    private fun givenUpdateModifies(count: Long) {
        whenever(operatorsRepository.updateLastTotpTimeStepIfNewer(any(), any()))
            .thenReturn(Mono.just(count))
    }

    private fun expectError(code: String, error: Class<out Throwable>, operatorId: String = "op") {
        StepVerifier.create(service.verifyAndConsume(operatorId, code, "tx"))
            .expectError(error)
            .verify()
    }

    @Test
    fun `accepts a valid code and stores its time step`() {
        givenUpdateModifies(1)

        StepVerifier.create(service.verifyAndConsume("op", validCode, "tx")).verifyComplete()
        verify(operatorsRepository).reserveTotpAttempt("op", now, maxAttempts, lockedUntil)
        verify(operatorsRepository).updateLastTotpTimeStepIfNewer("op", Totp.timeStep(now))
    }

    @Test
    fun `rejects a code that was already used`() {
        givenUpdateModifies(0)

        expectError(validCode, InvalidOtpException::class.java)
    }

    @Test
    fun `rejects a wrong code, the attempt is already counted and the time step is not consumed`() {
        expectError(wrongCode, InvalidOtpException::class.java)
        verify(operatorsRepository).reserveTotpAttempt("op", now, maxAttempts, lockedUntil)
        verify(operatorsRepository, never()).updateLastTotpTimeStepIfNewer(any(), any())
    }

    @Test
    fun `rejects any code without verifying it when the operator is locked`() {
        givenAttemptReserved(false)
        whenever(operatorsRepository.existsById("op")).thenReturn(Mono.just(true))

        expectError(validCode, TooManyOtpAttemptsException::class.java)
        verify(operatorsRepository, never()).updateLastTotpTimeStepIfNewer(any(), any())
    }

    @Test
    fun `rejects as invalid, not as locked, an operator that does not exist`() {
        givenAttemptReserved(false)
        whenever(operatorsRepository.existsById("op")).thenReturn(Mono.just(false))

        expectError(validCode, InvalidOtpException::class.java)
        verify(operatorsRepository, never()).updateLastTotpTimeStepIfNewer(any(), any())
    }

    @Test
    fun `rejects when the operator has no secret or a malformed one`() {
        whenever(secretsProvider.getSecret("missing")).thenReturn(Mono.empty())
        expectError(validCode, InvalidOtpException::class.java, "missing")
        verify(operatorsRepository, never()).reserveTotpAttempt(any(), any(), any(), any())

        whenever(secretsProvider.getSecret("op")).thenReturn(Mono.just("not-base32!"))
        expectError(validCode, InvalidOtpException::class.java)
    }

    @Test
    fun `propagates database errors`() {
        whenever(operatorsRepository.reserveTotpAttempt("op", now, maxAttempts, lockedUntil))
            .thenReturn(Mono.error(RuntimeException("mongo down")))

        expectError(validCode, RuntimeException::class.java)
    }
}
