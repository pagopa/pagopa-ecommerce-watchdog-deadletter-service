package it.pagopa.ecommerce.watchdog.deadletter.config.jwt

import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.SignedJWT
import it.pagopa.ecommerce.watchdog.deadletter.services.ReactiveAzureKVSecurityKeysService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Configuration
class JwtDecoderConfig(
    @Autowired private val azureKVSecurityKeysService: ReactiveAzureKVSecurityKeysService
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {

        val cachedKey: Mono<JWK> =
            azureKVSecurityKeysService
                .getPublicJwkFromKeyStore()
                .doOnSuccess { jwk ->
                    if (jwk == null) {
                        logger.error(
                            "Failed to retrieve public JWK from Azure KV, result was null."
                        )
                    } else {
                        logger.debug("JWK DECODER initialized. KID loaded: [${jwk.keyID}]")
                    }
                }
                .cache()

        val jwkSource = { _: SignedJWT? -> Flux.from(cachedKey) }

        return NimbusReactiveJwtDecoder.withJwkSource(jwkSource)
            .jwsAlgorithm(SignatureAlgorithm.ES256)
            .build()
    }
}
