package it.pagopa.ecommerce.watchdog.deadletter.services.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import it.pagopa.ecommerce.watchdog.deadletter.domain.UserDetails
import it.pagopa.ecommerce.watchdog.deadletter.domain.jwt.PrivateKeyWithKid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.Date

@Service
class JwtService(
    @Value("\${auth.jwt.validityTimeMinutes}") private val jwtDuration: Int,
    @Autowired private val reactiveAzureKVSecurityKeysService: ReactiveAzureKVSecurityKeysService

) {
    companion object {
        private val PUBLIC_CLAIMS =
            setOf(
                Claims.ISSUER,
                Claims.ID,
                Claims.AUDIENCE,
                Claims.SUBJECT,
                Claims.EXPIRATION,
                Claims.ISSUED_AT,
                Claims.NOT_BEFORE,
            )

        private const val WATCHDOG_AUDIENCE = "watchdog"
        private const val JWT_ISSUER = "watchdog-deadletter-service"
    }

    private val logger = LoggerFactory.getLogger(javaClass)

    fun generateUserJwtToken(userDetails: UserDetails): Mono<String> {
        return reactiveAzureKVSecurityKeysService
            .getPrivate()
            .map {
                generateJwtToken(
                    privateClaims = userDetails.toMap(),
                    privateKey = it,
                )
            }
            .doOnNext { logger.info("Token generated successfully") }
    }

    private fun generateJwtToken(
        privateClaims: Map<String, Any>,
        privateKey: PrivateKeyWithKid,
    ): String {
        val now = Instant.now()
        val issuedAtDate = Date.from(now)
        val expiryDate = Date.from(now.plus(Duration.ofMinutes(jwtDuration.toLong())))
        val headerParams = mapOf("kid" to privateKey.kid)
        val filteredPrivateClaims = privateClaims.filterNot { PUBLIC_CLAIMS.contains(it.key) }
        val jwtBuilder =
            Jwts.builder()
                .setHeaderParams(headerParams)
                .setClaims(filteredPrivateClaims)
                .setIssuedAt(issuedAtDate) // iat
                .setExpiration(expiryDate) // exp
                .setAudience(WATCHDOG_AUDIENCE) // aud
                .setIssuer(JWT_ISSUER) // iss
                .signWith(privateKey.privateKey)
        return jwtBuilder.compact()
    }
}