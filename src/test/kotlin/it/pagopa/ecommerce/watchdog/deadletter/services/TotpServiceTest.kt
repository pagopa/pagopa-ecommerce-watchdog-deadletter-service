package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidOtpException
import it.pagopa.ecommerce.watchdog.deadletter.repositories.OperatorsRepository
import it.pagopa.ecommerce.watchdog.deadletter.utils.Totp
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
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
    private val secretsProvider: TotpSecretsProvider = mock()
    private val operatorsRepository: OperatorsRepository = mock()
    private val service =
        TotpService(secretsProvider, operatorsRepository, Clock.fixed(now, ZoneOffset.UTC))

    private val validCode = Totp.generate(Totp.decodeBase32(secret), Totp.timeStep(now))

    private fun givenUpdateModifies(count: Long) {
        whenever(operatorsRepository.updateLastTotpTimeStepIfNewer(any(), any()))
            .thenReturn(Mono.just(count))
    }

    private fun expectInvalid(code: String, operatorId: String = "op") {
        StepVerifier.create(service.verifyAndConsume(operatorId, code, "tx"))
            .expectError(InvalidOtpException::class.java)
            .verify()
    }

    @Test
    fun `accepts a valid code and stores its time step`() {
        whenever(secretsProvider.getSecret("op")).thenReturn(Mono.just(secret))
        givenUpdateModifies(1)

        StepVerifier.create(service.verifyAndConsume("op", validCode, "tx")).verifyComplete()
        verify(operatorsRepository).updateLastTotpTimeStepIfNewer("op", Totp.timeStep(now))
    }

    @Test
    fun `rejects a code that was already used`() {
        whenever(secretsProvider.getSecret("op")).thenReturn(Mono.just(secret))
        givenUpdateModifies(0)

        expectInvalid(validCode)
    }

    @Test
    fun `rejects a wrong code without touching the database`() {
        whenever(secretsProvider.getSecret("op")).thenReturn(Mono.just(secret))

        expectInvalid(if (validCode == "000000") "111111" else "000000")
        verify(operatorsRepository, never()).updateLastTotpTimeStepIfNewer(any(), any())
    }

    @Test
    fun `rejects when the operator has no secret or a malformed one`() {
        whenever(secretsProvider.getSecret("missing")).thenReturn(Mono.empty())
        expectInvalid(validCode, "missing")

        whenever(secretsProvider.getSecret("op")).thenReturn(Mono.just("not-base32!"))
        expectInvalid(validCode)
    }
}
