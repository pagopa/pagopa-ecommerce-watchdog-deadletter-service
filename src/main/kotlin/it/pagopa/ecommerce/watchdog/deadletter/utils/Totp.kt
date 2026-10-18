package it.pagopa.ecommerce.watchdog.deadletter.utils

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** TOTP (RFC 6238) with HMAC-SHA1, 6 digits and 30 seconds steps, as used by Authenticator apps. */
object Totp {
    const val STEP_SECONDS = 30L
    private const val DIGITS = 6
    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun timeStep(instant: Instant): Long = instant.epochSecond / STEP_SECONDS

    fun generate(secret: ByteArray, timeStep: Long): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(secret, "HmacSHA1"))
        val hash = mac.doFinal(ByteBuffer.allocate(8).putLong(timeStep).array())
        val offset = hash.last().toInt() and 0x0f
        val binary =
            ((hash[offset].toInt() and 0x7f) shl 24) or
                ((hash[offset + 1].toInt() and 0xff) shl 16) or
                ((hash[offset + 2].toInt() and 0xff) shl 8) or
                (hash[offset + 3].toInt() and 0xff)
        return (binary % 1_000_000).toString().padStart(DIGITS, '0')
    }

    /**
     * Returns the time step matching [code] within ±[window] steps around [now], or null when the
     * code is not valid.
     */
    fun matchingTimeStep(base32Secret: String, code: String, now: Instant, window: Int = 1): Long? {
        if (code.length != DIGITS || !code.all { it.isDigit() }) return null
        val secret = decodeBase32(base32Secret)
        val current = timeStep(now)
        return (current - window..current + window).firstOrNull { step ->
            MessageDigest.isEqual(generate(secret, step).toByteArray(), code.toByteArray())
        }
    }

    fun decodeBase32(value: String): ByteArray {
        val clean = value.uppercase().filterNot { it == '=' || it.isWhitespace() }
        val output = ByteArray(clean.length * 5 / 8)
        var buffer = 0
        var bits = 0
        var index = 0
        for (char in clean) {
            val digit = BASE32_ALPHABET.indexOf(char)
            require(digit >= 0) { "Invalid Base32 character" }
            buffer = (buffer shl 5) or digit
            bits += 5
            if (bits >= 8) {
                output[index++] = (buffer shr (bits - 8)).toByte()
                bits -= 8
            }
        }
        return output
    }
}
