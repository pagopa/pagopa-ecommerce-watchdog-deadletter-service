package utils.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import it.pagopa.ecommerce.watchdog.deadletter.JwtKeyGenerationTestUtils.Companion.getKeyPairEC
import it.pagopa.ecommerce.watchdog.deadletter.domain.jwt.PrivateKeyWithKid
import it.pagopa.ecommerce.watchdog.deadletter.utils.jwt.JwtUtils
import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class JwtUtilsTest {
    private val jwtUtils = JwtUtils(1)

    @Test
    fun `Should generate token successfully filtering out public claims from input private ones`() {
        // pre conditions
        val audience = "watchdog"
        val privateKey = getKeyPairEC()
        val privateKeyWithKid = PrivateKeyWithKid("kid", privateKey.private)
        // public reserver claims that will be filtered out by app code
        val illegalPrivateClaims =
            mapOf(
                Claims.ISSUER to "OVERRIDDEN",
                Claims.ID to "OVERRIDDEN",
                Claims.AUDIENCE to "OVERRIDDEN",
                Claims.SUBJECT to "OVERRIDDEN",
                Claims.EXPIRATION to "OVERRIDDEN",
                Claims.ISSUED_AT to "OVERRIDDEN",
                Claims.NOT_BEFORE to "OVERRIDDEN",
            )
        // application custom claims
        val legitPrivateClaims =
            mapOf("testClaimKey1" to "testClaimValue1", "testClaimKey2" to "testClaimValue2")
        val privateClaims = illegalPrivateClaims + legitPrivateClaims

        // test
        val generatedToken =
            jwtUtils.generateJwtToken(privateClaims = privateClaims, privateKey = privateKeyWithKid)
        val parsedToken =
            Jwts.parserBuilder()
                .setSigningKey(privateKey.public)
                .build()
                .parse(generatedToken.block())
        val header = parsedToken.header
        val body = parsedToken.body as Claims
        // verify header claims
        assertEquals(privateKeyWithKid.kid, header["kid"])
        assertEquals("ES256", header["alg"])
        // verify body claims
        val expirationClaim = body[Claims.EXPIRATION] as Int
        val issuedAtClaim = body[Claims.ISSUED_AT] as Int
        val expirationInstant = Instant.ofEpochMilli(expirationClaim * 1000L)
        val issuedAtInstant = Instant.ofEpochMilli(issuedAtClaim * 1000L)
        assertEquals(Duration.ofMinutes(1), Duration.between(issuedAtInstant, expirationInstant))
        assertEquals(audience, body[Claims.AUDIENCE])
        assertEquals("watchdog-deadletter-service", body[Claims.ISSUER])
        assertNull(body[Claims.SUBJECT])
        assertNull(body[Claims.NOT_BEFORE])
        legitPrivateClaims.forEach {
            assertEquals(
                it.value,
                body[it.key],
                "legit claim with key: [${it.key}] not found into generated token",
            )
        }
        illegalPrivateClaims.forEach {
            assertNotEquals(
                it.value,
                body[it.key],
                "public claim with key: [${it.key}] have been not filtered out",
            )
        }
    }
}
