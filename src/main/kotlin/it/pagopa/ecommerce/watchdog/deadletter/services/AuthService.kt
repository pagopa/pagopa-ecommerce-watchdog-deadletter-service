package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationOkDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuthService(@Value("\${login.redirectUrl}") private val loginRedirectUrl: String) {

    fun authenticateUser(credentials: AuthenticationCredentialsDto): Mono<AuthenticationOkDto> {
        return Mono.just(AuthenticationOkDto("$loginRedirectUrl#token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6ImlkIiwibmFtZSI6Ik1hcmlvIiwic3VybmFtZSI6IlJvc3NpIiwiZW1haWwiOiJtYXJpby5yb3NzaUBtb2NrLmNvbSIsImlhdCI6MTc1OTc1Mjg3NCwiZXhwIjoxNzU5OTMxOTMzLCJhdWQiOiJ3YXRjaGRvZyIsImlzcyI6IndhdGNoZG9nLWRlYWRsZXR0ZXItc2VydmljZSJ9.PXQlFZYtWo5eb_XuhEvJC3x4vmjtVYKHb01FZ2QIVFw"))
    }
}
