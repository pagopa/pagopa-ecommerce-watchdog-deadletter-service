package it.pagopa.ecommerce.watchdog.deadletter.config.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import it.pagopa.ecommerce.watchdog.deadletter.JwtKeyGenerationTestUtils.Companion.getKeyPairEC
import it.pagopa.ecommerce.watchdog.deadletter.config.jwt.JwtDecoderConfig.*
import it.pagopa.ecommerce.watchdog.deadletter.services.ReactiveAzureKVSecurityKeysService
import java.security.interfaces.ECPublicKey
import java.util.Date
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.Mockito.mock
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

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

    @Test
    fun `azureKVSecurityKeysService should return a Mono JWK and cache work`() {

        // Mocked azureKVSecurityKeysService
        val azureKVSecurityKeysService: ReactiveAzureKVSecurityKeysService = mock()
        val jwkMock: JWK = mock()
        val jwkMono: Mono<JWK> = Mono.just(jwkMock)

        given(azureKVSecurityKeysService.getPublicJwkFromKeyStore()).willReturn(jwkMono)

        val refreshableCachedJwkSource = RefreshableCachedJwkSource(azureKVSecurityKeysService, 10)

        val resultMono = refreshableCachedJwkSource.get()

        // Assert that the jwk returned is the mocked one
        StepVerifier.create(resultMono).expectNext(jwkMock).verifyComplete()

        // Verify the cache activation
        resultMono.block()
        resultMono.block()

        // Verify that the method is called only one time because of the cache value of JWK
        verify(azureKVSecurityKeysService, times(1)).getPublicJwkFromKeyStore()
    }

    @Test
    fun `azureKVSecurityKeysService would be refreshed`() {

        // Mocked azureKVSecurityKeysService
        val azureKVSecurityKeysService: ReactiveAzureKVSecurityKeysService = mock()
        val jwkMock: JWK = mock()
        val jwkMono: Mono<JWK> = Mono.just(jwkMock)

        given(azureKVSecurityKeysService.getPublicJwkFromKeyStore()).willReturn(jwkMono)

        val refreshableCachedJwkSource = RefreshableCachedJwkSource(azureKVSecurityKeysService, 10)

        // Assert that the jwk created is the same
        StepVerifier.create(refreshableCachedJwkSource.get()).assertNext { jwk ->
            assertEquals(jwk, jwkMock)
        }

        val jwkMockNew: JWK = mock()
        val jwkMonoNew: Mono<JWK> = Mono.just(jwkMockNew)
        given(azureKVSecurityKeysService.getPublicJwkFromKeyStore()).willReturn(jwkMonoNew)

        refreshableCachedJwkSource.refresh()

        // Assert that the jwk returned is the new one created by the refresh function
        StepVerifier.create(refreshableCachedJwkSource.get()).assertNext { jwk ->
            assertEquals(jwk, jwkMockNew)
        }
    }

    @Test
    fun `Test the error case of create JWK`() {

        // Mocked azureKVSecurityKeysService
        val azureKVSecurityKeysService: ReactiveAzureKVSecurityKeysService = mock()

        val exception = RuntimeException("Error JWK creation")
        given(azureKVSecurityKeysService.getPublicJwkFromKeyStore())
            .willReturn(Mono.error(exception))

        val refreshableCachedJwkSource = RefreshableCachedJwkSource(azureKVSecurityKeysService, 10)

        val resultMono = refreshableCachedJwkSource.get()

        // Verify that an exception is returned after 2 subscription
        StepVerifier.create(resultMono).expectError(RuntimeException::class.java).verify()

        StepVerifier.create(resultMono).expectError(RuntimeException::class.java).verify()

        // Verify that the method is called 2 times because no cache value is present
        verify(azureKVSecurityKeysService, times(1)).getPublicJwkFromKeyStore()
    }

    @Test
    fun `jwtDecoder would refresh the token when an old one is cached without error`() {
        // Generate a old key and a updated key to test the decoding of the token, first time with
        // the old one and after the refresh with the new one
        // This key is the expired one
        val oldEcKey: ECKey = ECKeyGenerator(Curve.P_256).keyID("kid-old").generate()
        // This is the new one
        val realEcKey: ECKey = ECKeyGenerator(Curve.P_256).keyID("kid-new").generate()

        // Create a signed token with the update key
        val signer = ECDSASigner(realEcKey)
        val claimsSet =
            JWTClaimsSet.Builder()
                .subject("test-user")
                .expirationTime(Date(System.currentTimeMillis() + 60 * 1000))
                .build()

        val signedJWT =
            SignedJWT(
                JWSHeader.Builder(JWSAlgorithm.ES256).keyID(realEcKey.keyID).build(),
                claimsSet,
            )
        signedJWT.sign(signer)
        val signedToken = signedJWT.serialize()

        // Generate the mock for the ReactiveAzureKVSecurityKeysService
        val azureKVSecurityKeysService: ReactiveAzureKVSecurityKeysService = mock()
        // The first time the mock will return the expired key, then the correct one
        Mockito.`when`(azureKVSecurityKeysService.getPublicJwkFromKeyStore())
            .thenReturn(Mono.just(oldEcKey))
            .thenReturn(Mono.just(realEcKey))

        val jwtDecoderConfig: JwtDecoderConfig = JwtDecoderConfig(azureKVSecurityKeysService, 1)
        val jwtDecoder = jwtDecoderConfig.jwtDecoder()
        val decodResult = jwtDecoder.decode(signedToken)

        StepVerifier.create(decodResult)
            .assertNext { jwt ->
                assertEquals("test-user", jwt.subject)
                assertEquals("kid-new", jwt.headers["kid"])
            }
            .verifyComplete()

        // Verify that the method is called 2 times because it is refreshed
        verify(azureKVSecurityKeysService, times(2)).getPublicJwkFromKeyStore()
    }

    @Test
    fun `jwtDecoder would refresh the token when an old one is cached but will receive an old token and generate an error`() {
        // Generate a old key and a updated key to test the decoding of the token, first time with
        // the old one and after the refresh with the new one
        // This key is the expired one
        val oldEcKey: ECKey = ECKeyGenerator(Curve.P_256).keyID("kid-old").generate()
        // This is the new one
        val realEcKey: ECKey = ECKeyGenerator(Curve.P_256).keyID("kid-new").generate()

        // Create a signed token with the update key
        val signer = ECDSASigner(realEcKey)
        val claimsSet =
            JWTClaimsSet.Builder()
                .subject("test-user")
                .expirationTime(Date(System.currentTimeMillis() + 60 * 1000))
                .build()

        val signedJWT =
            SignedJWT(
                JWSHeader.Builder(JWSAlgorithm.ES256).keyID(realEcKey.keyID).build(),
                claimsSet,
            )
        signedJWT.sign(signer)
        val signedToken = signedJWT.serialize()

        // Generate the mock for the ReactiveAzureKVSecurityKeysService
        val azureKVSecurityKeysService: ReactiveAzureKVSecurityKeysService = mock()
        // In both of the call the key would be not correct
        Mockito.`when`(azureKVSecurityKeysService.getPublicJwkFromKeyStore())
            .thenReturn(Mono.just(oldEcKey))
            .thenReturn(Mono.just(oldEcKey))

        val jwtDecoderConfig: JwtDecoderConfig = JwtDecoderConfig(azureKVSecurityKeysService, 1)
        val jwtDecoder = jwtDecoderConfig.jwtDecoder()
        val decodResult = jwtDecoder.decode(signedToken)

        StepVerifier.create(decodResult).expectError(BadJwtException::class.java).verify()

        // Verify that the method is called 2 times because it is refreshed
        verify(azureKVSecurityKeysService, times(2)).getPublicJwkFromKeyStore()
    }
}
