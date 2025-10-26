package it.pagopa.ecommerce.watchdog.deadletter.utils.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import it.pagopa.ecommerce.watchdog.deadletter.domain.jwt.PrivateKeyWithKid
import it.pagopa.ecommerce.watchdog.deadletter.services.ReactiveAzureKVSecurityKeysService
import java.time.Duration
import java.time.Instant
import java.util.Date
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono // <-- Importa questo
import reactor.core.scheduler.Schedulers

@Component
class JwtUtils(
    @Value("\${auth.jwt.validityTimeMinutes}") private val jwtDuration: Int,
    private val keyService: ReactiveAzureKVSecurityKeysService,
) {
    companion object {
        private const val WATCHDOG_AUDIENCE = "watchdog"
        private const val JWT_ISSUER = "watchdog-deadletter-service"
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
    }

    fun generateJwtToken(
        privateClaims: Map<String, Any>,
        privateKey: PrivateKeyWithKid,
    ): Mono<String> {

        return Mono.fromCallable {
                val now = Instant.now()
                val issuedAtDate = Date.from(now)
                val expiryDate = Date.from(now.plus(Duration.ofMinutes(jwtDuration.toLong())))
                val headerParams = mapOf("kid" to privateKey.kid)
                val filteredPrivateClaims =
                    privateClaims.filterNot { PUBLIC_CLAIMS.contains(it.key) }

                Jwts.builder()
                    .setHeaderParams(headerParams)
                    .setClaims(filteredPrivateClaims)
                    .setIssuedAt(issuedAtDate)
                    .setExpiration(expiryDate)
                    .setAudience(WATCHDOG_AUDIENCE)
                    .setIssuer(JWT_ISSUER)
                    .signWith(privateKey.privateKey)
                    .compact()
            }
            .subscribeOn(Schedulers.boundedElastic())
    }
}
