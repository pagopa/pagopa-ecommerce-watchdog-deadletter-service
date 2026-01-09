package it.pagopa.ecommerce.watchdog.deadletter.config.jwt

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import it.pagopa.ecommerce.watchdog.deadletter.JwtKeyGenerationTestUtils.Companion.getKeyPairEC
import it.pagopa.ecommerce.watchdog.deadletter.services.ReactiveAzureKVSecurityKeysService
import java.security.interfaces.ECPublicKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Mono

@SpringBootTest(classes = [JwtDecoderConfig::class])
@Import(JwtDecoderConfigTest.MockReactiveAzureKVSecurityKeysServiceConfiguration::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class JwtDecoderConfigTest {

    @TestConfiguration
    internal class MockReactiveAzureKVSecurityKeysServiceConfiguration {

        @Bean
        fun azureKVSecurityKeysService(): ReactiveAzureKVSecurityKeysService {
            val mockService: ReactiveAzureKVSecurityKeysService = mock()
            val keyPair = getKeyPairEC()

            val jwk: JWK =
                ECKey.Builder(Curve.P_256, keyPair.public as ECPublicKey)
                    .keyID("mock-test-kid")
                    .build()

            given(mockService.getPublicJwkFromKeyStore()).willReturn(Mono.just(jwk))

            return mockService
        }
    }

    @Autowired private lateinit var jwtDecoder: ReactiveJwtDecoder

    @Test
    fun `should load context and create jwtDecoder bean`() {
        assertThat(jwtDecoder).isNotNull()
        assertThat(jwtDecoder).isInstanceOf(ReactiveJwtDecoder::class.java)
    }
}
