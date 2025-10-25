package it.pagopa.ecommerce.watchdog.deadletter.utils.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import it.pagopa.ecommerce.watchdog.deadletter.services.jwt.ReactiveAzureKVSecurityKeysService // Importa
import java.time.Duration
import java.time.Instant
import java.util.Date
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JwtUtils(
    @Value("\${auth.jwt.validityTimeMinutes}") private val jwtDuration: Int,
    private val keyService: ReactiveAzureKVSecurityKeysService
) {
    companion object {
        private const val WATCHDOG_AUDIENCE = "watchdog"
        private const val JWT_ISSUER = "watchdog-deadletter-service"
    }

    fun generateJwtToken(privateClaims: Map<String, Any>): String {
        val ecSigningKey = keyService.getSignerJwk().block()
            ?: throw IllegalStateException("Failed to load signing key from KV")

        val signer = ECDSASigner(ecSigningKey)

        val header = JWSHeader.Builder(JWSAlgorithm.ES256)
            .keyID(ecSigningKey.keyID)
            .build()

        val now = Instant.now()
        val expiryDate = now.plus(Duration.ofMinutes(jwtDuration.toLong()))

        val claimsSetBuilder = JWTClaimsSet.Builder()
            .issuer(JWT_ISSUER)
            .audience(WATCHDOG_AUDIENCE)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiryDate))

        privateClaims.forEach { (key, value) ->
            claimsSetBuilder.claim(key, value)
        }

        val signedJWT = SignedJWT(header, claimsSetBuilder.build())
        signedJWT.sign(signer)

        return signedJWT.serialize()
    }
}