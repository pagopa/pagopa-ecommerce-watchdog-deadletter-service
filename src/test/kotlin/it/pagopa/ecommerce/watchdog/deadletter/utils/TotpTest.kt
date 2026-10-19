package it.pagopa.ecommerce.watchdog.deadletter.utils

import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TotpTest {

    // RFC 6238 Appendix B SHA256 secret ("12345678901234567890123456789012") encoded in Base32
    private val rfcSecret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA===="

    @ParameterizedTest
    @CsvSource(
        "59, 119246",
        "1111111109, 084774",
        "1111111111, 062674",
        "1234567890, 819424",
        "2000000000, 698825",
    )
    fun `generates RFC 6238 SHA256 codes truncated to 6 digits`(
        epochSeconds: Long,
        expected: String,
    ) {
        val step = Totp.timeStep(Instant.ofEpochSecond(epochSeconds))

        assertEquals(expected, Totp.generate(Totp.decodeBase32(rfcSecret), step))
    }

    @Test
    fun `accepts codes within one step of drift`() {
        val now = Instant.ofEpochSecond(1111111111)
        val previousStepCode = Totp.generate(Totp.decodeBase32(rfcSecret), Totp.timeStep(now) - 1)

        assertEquals(
            Totp.timeStep(now) - 1,
            Totp.matchingTimeStep(rfcSecret, previousStepCode, now),
        )
    }

    @Test
    fun `rejects codes outside the window and malformed codes`() {
        val now = Instant.ofEpochSecond(1111111111)
        val oldCode = Totp.generate(Totp.decodeBase32(rfcSecret), Totp.timeStep(now) - 3)

        assertNull(Totp.matchingTimeStep(rfcSecret, oldCode, now))
        assertNull(Totp.matchingTimeStep(rfcSecret, "12345", now))
        assertNull(Totp.matchingTimeStep(rfcSecret, "12a456", now))
    }

    @Test
    fun `decodes Base32 ignoring case, spaces and padding`() {
        assertEquals("foobar", String(Totp.decodeBase32("mzxw 6ytb oi======")))
    }

    @Test
    fun `rejects invalid Base32 secrets`() {
        assertThrows<IllegalArgumentException> { Totp.decodeBase32("not-base32!") }
    }
}
