package it.pagopa.ecommerce.watchdog.deadletter.config.jwt

import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.SignedJWT
import it.pagopa.ecommerce.watchdog.deadletter.services.jwt.ReactiveAzureKVSecurityKeysService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import reactor.core.publisher.Flux

@Configuration
class JwtDecoderConfig(
    @Autowired private val azureKVSecurityKeysService: ReactiveAzureKVSecurityKeysService
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {
        val jwk =
            azureKVSecurityKeysService.getPublicJwkFromKeyStore().block()
                ?: throw IllegalStateException(
                    "Failed to retrieve public JWK from Azure KV, result was null."
                )

        logger.warn("JWK alg: ${jwk.algorithm}, curve: ${(jwk as ECKey).curve}")

        logger.warn("JWK DECODER init. KID loaded: [${jwk.keyID}]")

        val decoder =
            NimbusReactiveJwtDecoder.withJwkSource { _: SignedJWT? -> Flux.just<JWK?>(jwk) }.build()

        decoder.setJwtValidator { jwt -> OAuth2TokenValidatorResult.success() }

        return decoder
    }
}
