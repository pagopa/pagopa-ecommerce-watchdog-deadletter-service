package it.pagopa.ecommerce.watchdog.deadletter.config.jwt

import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.SignedJWT
import it.pagopa.ecommerce.watchdog.deadletter.services.ReactiveAzureKVSecurityKeysService
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Configuration
class JwtDecoderConfig(
    @Autowired private val azureKVSecurityKeysService: ReactiveAzureKVSecurityKeysService,
    @Value("\${auth.jwt.secret.cache.ttl}") private val cacheSecretTtl: Long,
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    class RefreshableCachedJwkSource(
        private val azureKVSecurityKeysService: ReactiveAzureKVSecurityKeysService,
        val cacheTtl: Long,
    ) {
        private val logger = LoggerFactory.getLogger(this.javaClass)

        @Volatile private var currentCachedMono: Mono<JWK>

        init {
            // Initialize the value of the current cache value
            currentCachedMono = createCache()
        }

        // Generate a new JWT
        private fun createCache(): Mono<JWK> {
            return azureKVSecurityKeysService
                .getPublicJwkFromKeyStore()
                .doOnSuccess { jwk ->
                    if (jwk == null) {
                        logger.error(
                            "Failed to retrieve public JWK from Azure KV, result was null."
                        )
                    } else {
                        logger.info("JWK DECODER initialized. KID loaded: [${jwk.keyID}]")
                    }
                }
                .doOnError { err ->
                    logger.error("Error during retrieve of JWK from Azure KV")
                    logger.error(err.stackTraceToString())
                }
                .cache(
                    { _ -> Duration.ofMinutes(cacheTtl) },
                    { _ -> Duration.ZERO }, // No cache if there is an error, instant retry
                    { Duration.ZERO }, // No cache if is empty
                )
        }

        fun get(): Mono<JWK> {
            return currentCachedMono
        }

        fun refresh(): Mono<JWK> {
            currentCachedMono = createCache()
            return currentCachedMono
        }
    }

    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {

        val cachedJwtResource =
            RefreshableCachedJwkSource(azureKVSecurityKeysService, cacheSecretTtl)

        val jwkSource = { _: SignedJWT? -> Flux.from(cachedJwtResource.get()) }

        val nimbusDelegate =
            NimbusReactiveJwtDecoder.withJwkSource(jwkSource)
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .build()

        return ReactiveJwtDecoder { token ->
            nimbusDelegate.decode(token).onErrorResume(BadJwtException::class.java) { _ ->
                logger.warn("Error during the validation of the token, possible expired key")

                // Refresh the cached key
                cachedJwtResource.refresh().flatMap { _ ->
                    nimbusDelegate.decode(token).onErrorMap(BadJwtException::class.java) { retryEx
                        ->
                        logger.error("Validation failed after the refresh of the key!")
                        retryEx
                    }
                }
            }
        }
    }
}
