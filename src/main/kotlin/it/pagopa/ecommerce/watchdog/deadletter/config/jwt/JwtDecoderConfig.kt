package it.pagopa.ecommerce.watchdog.deadletter.config.jwt

import it.pagopa.ecommerce.watchdog.deadletter.services.jwt.ReactiveAzureKVSecurityKeysService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import java.security.interfaces.RSAPublicKey

@Configuration
class JwtDecoderConfig(
    @Autowired private val azureKVSecurityKeysService: ReactiveAzureKVSecurityKeysService
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {
        val publicKey = azureKVSecurityKeysService.getPublic().next().block()
        return NimbusReactiveJwtDecoder.withPublicKey(publicKey!!.publicKey as RSAPublicKey?)
            .build()
    }
}