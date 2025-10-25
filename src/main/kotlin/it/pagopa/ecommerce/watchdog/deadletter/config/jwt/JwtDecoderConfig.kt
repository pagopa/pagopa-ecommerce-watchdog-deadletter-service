package it.pagopa.ecommerce.watchdog.deadletter.config.jwt

import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.SignedJWT
import it.pagopa.ecommerce.watchdog.deadletter.services.jwt.ReactiveAzureKVSecurityKeysService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import reactor.core.publisher.Flux

@Configuration
class JwtDecoderConfig(
    @Autowired private val azureKVSecurityKeysService: ReactiveAzureKVSecurityKeysService
) {

    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {
        val jwk =
            azureKVSecurityKeysService.getPublicJwkFromKeyStore().block()
                ?: throw IllegalStateException(
                    "Failed to retrieve public JWK from Azure KV, result was null."
                )

        return NimbusReactiveJwtDecoder.withJwkSource { _: SignedJWT? -> Flux.just<JWK?>(jwk) }
            .build()
    }
}
