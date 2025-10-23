package it.pagopa.ecommerce.watchdog.deadletter.config.jwt

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.SignedJWT
import it.pagopa.ecommerce.watchdog.deadletter.services.jwt.ReactiveAzureKVSecurityKeysService
import java.security.interfaces.ECPublicKey
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
        val publicKey =
            azureKVSecurityKeysService.getPublic().next().block()
                ?: throw IllegalStateException(
                    "Failed to retrieve public key from Azure KV, result was null."
                )

        val jwk: JWK =
            ECKey.Builder(Curve.P_256, publicKey.publicKey as ECPublicKey?)
                .keyID(publicKey.kid)
                .build()

        return NimbusReactiveJwtDecoder.withJwkSource { _: SignedJWT? -> Flux.just<JWK?>(jwk) }
            .build()
    }
}
