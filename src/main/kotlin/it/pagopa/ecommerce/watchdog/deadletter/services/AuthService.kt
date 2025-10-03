package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationOkDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuthService(@Value("\${login.redirectUrl}") private val loginRedirectUrl: String) {

    fun authenticateUser(credentials: AuthenticationCredentialsDto): Mono<AuthenticationOkDto> {
        return Mono.just(AuthenticationOkDto("$loginRedirectUrl#token=123"))
    }
}
