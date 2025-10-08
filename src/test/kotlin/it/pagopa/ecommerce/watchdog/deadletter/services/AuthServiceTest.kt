package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.domain.UserDetails
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import org.junit.jupiter.api.Test
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
}
