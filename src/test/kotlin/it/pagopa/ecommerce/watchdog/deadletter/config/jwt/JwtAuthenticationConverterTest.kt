package it.pagopa.ecommerce.watchdog.deadletter.config.jwt

import it.pagopa.ecommerce.watchdog.deadletter.domain.UserDetails
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.given
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class JwtAuthenticationConverterTest {
    private val jwtAuthenticationConverter = JwtAuthenticationConverter()

    @Test
    fun `should correctly convert a Jwt into an AuthenticationToken`() {
        // pre-requisites
        val mockJwt: Jwt = mock()
        val claims =
            mapOf(
                "id" to "12345",
                "name" to "Mario",
                "surname" to "Rossi",
                "email" to "mock@email.com",
            )
        val tokenValue = "mock-jwt-token-string"
        given(mockJwt.claims).willReturn(claims)
        given(mockJwt.tokenValue).willReturn(tokenValue)

        val expectedUser =
            UserDetails(id = "12345", name = "Mario", surname = "Rossi", email = "mock@email.com")

        val resultMono: Mono<AbstractAuthenticationToken> =
            jwtAuthenticationConverter.convert(mockJwt)!!

        // test
        StepVerifier.create(resultMono)
            .assertNext { authenticationToken ->
                assertThat(authenticationToken.principal).isEqualTo(expectedUser)
                assertThat(authenticationToken.credentials).isEqualTo(tokenValue)
                assertThat(authenticationToken)
                    .isInstanceOf(UsernamePasswordAuthenticationToken::class.java)
            }
            .verifyComplete()
    }
}
