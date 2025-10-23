package it.pagopa.ecommerce.watchdog.deadletter.config.jwt

import it.pagopa.ecommerce.watchdog.deadletter.JwtKeyGenerationTestUtils.Companion.getKeyPairRSA
import it.pagopa.ecommerce.watchdog.deadletter.domain.jwt.PublicKeyWithKid
import it.pagopa.ecommerce.watchdog.deadletter.services.jwt.ReactiveAzureKVSecurityKeysService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import reactor.core.publisher.Flux

@SpringBootTest(classes = [JwtDecoderConfig::class])
@Import(JwtDecoderConfigTest.MockReactiveAzureKVSecurityKeysServiceConfiguration::class)
class JwtDecoderConfigTest {

    @TestConfiguration
    internal class MockReactiveAzureKVSecurityKeysServiceConfiguration {

        @Bean
        fun azureKVSecurityKeysService(): ReactiveAzureKVSecurityKeysService {
            val mockService: ReactiveAzureKVSecurityKeysService = mock()
            val keyPair = getKeyPairRSA()
            val publicKeyWithKid = PublicKeyWithKid("kid", keyPair.public)
            given(mockService.getPublic()).willReturn(Flux.just(publicKeyWithKid))
            return mockService
        }
    }

    @Autowired private lateinit var jwtDecoder: ReactiveJwtDecoder

    private val kvService: ReactiveAzureKVSecurityKeysService = org.mockito.kotlin.mock()

    @Test
    fun `should load context and create jwtDecoder bean`() {
        assertThat(jwtDecoder).isNotNull()
        assertThat(jwtDecoder).isInstanceOf(NimbusReactiveJwtDecoder::class.java)
    }
}
