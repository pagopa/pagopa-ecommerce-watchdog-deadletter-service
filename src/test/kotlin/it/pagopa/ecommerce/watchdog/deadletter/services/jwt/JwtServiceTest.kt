package it.pagopa.ecommerce.watchdog.deadletter.services.jwt

import it.pagopa.ecommerce.watchdog.deadletter.JwtKeyGenerationTestUtils.Companion.getKeyPairEC
import it.pagopa.ecommerce.watchdog.deadletter.domain.UserDetails
import it.pagopa.ecommerce.watchdog.deadletter.domain.jwt.PrivateKeyWithKid
import it.pagopa.ecommerce.watchdog.deadletter.utils.jwt.JwtUtils
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class JwtServiceTest {
    private val kvService: ReactiveAzureKVSecurityKeysService = mock()
    private val jwtUtils: JwtUtils = mock()
    private val jwtService: JwtService = JwtService(kvService, jwtUtils)

    @Test
    fun `Should generate user token successfully`() {
        // pre-conditions
        val privateClaims = mapOf("key" to "value")
        val userDetails = UserDetails("id", "name", "surname", "test@email.com")
        val token = "jwtToken"
        val privateKey = getKeyPairEC()
        val privateKeyWithKid = PrivateKeyWithKid("kid", privateKey.private)
        given(kvService.getPrivate()).willReturn(Mono.just(privateKeyWithKid))
        given(jwtUtils.generateJwtToken(any(), any())).willReturn(token)

        // test
        StepVerifier.create(jwtService.generateUserJwtToken(userDetails))
            .expectNext(token)
            .verifyComplete()
        verify(jwtUtils, times(1)).generateJwtToken(privateClaims, privateKeyWithKid)
    }
}
