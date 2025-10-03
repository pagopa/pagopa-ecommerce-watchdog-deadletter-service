package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

class AuthServiceTest {
    private val loginRedirectUrl = "http://localhost/"

    private val authService: AuthService = AuthService(loginRedirectUrl)

    @Test
    fun `should authenticate user`() {
        // pre-requisites
        val credentials = AuthenticationCredentialsDto("user", "password")

        // test
        StepVerifier.create(authService.authenticateUser(credentials))
            .expectNextMatches { authenticationOkDto ->
                authenticationOkDto.urlRedirect == "$loginRedirectUrl#token=123"
            }
            .verifyComplete()
    }
}
