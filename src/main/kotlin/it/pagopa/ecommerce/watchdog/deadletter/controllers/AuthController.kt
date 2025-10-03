package it.pagopa.ecommerce.watchdog.deadletter.controllers

import it.pagopa.ecommerce.watchdog.deadletter.services.AuthService
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.api.AuthenticateApi
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationOkDto
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
class AuthController(@Autowired private val authService: AuthService) : AuthenticateApi {

    override fun authenticateUser(
        authenticationCredentialsDto: @Valid Mono<AuthenticationCredentialsDto>,
        exchange: ServerWebExchange?,
    ): Mono<ResponseEntity<AuthenticationOkDto>> {
        return authenticationCredentialsDto
            .flatMap { credentials -> authService.authenticateUser(credentials) }
            .map { response -> ResponseEntity.ok(response) }
    }
}
