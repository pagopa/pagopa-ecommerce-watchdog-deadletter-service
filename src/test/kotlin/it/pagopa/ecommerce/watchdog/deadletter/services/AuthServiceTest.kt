package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.domain.UserDetails
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class AuthServiceTest {
    private val authService: AuthService = AuthService()

    @Test
    fun `should authenticate user`() {
        // pre-requisites
        val credentials = AuthenticationCredentialsDto("user", "password")
        val userDetails = UserDetails("12345", "Mario", "Rossi", "mock@email.com")

        // test
        StepVerifier.create(authService.authenticateUser(credentials))
            .expectNext(userDetails)
            .verifyComplete()
    }

    @Test
    fun `should return authenticated user id`() {
        // pre-requisites
        val userDetails = UserDetails("12345", "Mario", "Rossi", "mock@email.com")
        val auth = UsernamePasswordAuthenticationToken(userDetails, null, null)
        val context = SecurityContextImpl(auth)
        val result =
            authService
                .getAuthenticatedUserId()
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)))

        // test
        StepVerifier.create(result).expectNext("12345").verifyComplete()
    }
}
