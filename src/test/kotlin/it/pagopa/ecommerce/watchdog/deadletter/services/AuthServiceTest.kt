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
                authenticationOkDto.urlRedirect ==
                    "$loginRedirectUrl#token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6ImlkIiwibmFtZSI6Ik1hcmlvIiwic3VybmFtZSI6IlJvc3NpIiwiZW1haWwiOiJtYXJpby5yb3NzaUBtb2NrLmNvbSIsImlhdCI6MTc1OTc1Mjg3NCwiZXhwIjoxNzU5OTMxOTMzLCJhdWQiOiJ3YXRjaGRvZyIsImlzcyI6IndhdGNoZG9nLWRlYWRsZXR0ZXItc2VydmljZSJ9.PXQlFZYtWo5eb_XuhEvJC3x4vmjtVYKHb01FZ2QIVFw"
            }
            .verifyComplete()
    }
}
