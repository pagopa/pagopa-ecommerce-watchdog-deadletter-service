package it.pagopa.ecommerce.watchdog.deadletter.controllers

import it.pagopa.ecommerce.watchdog.deadletter.services.AuthService
import it.pagopa.ecommerce.watchdog.deadletter.services.JwtService
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.api.AuthenticateApi
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationOkDto
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
class AuthController(
    @Autowired private val authService: AuthService,
    @Autowired private val jwtService: JwtService,
    @Value("\${auth.login.redirectUrl}") private val loginRedirectUrl: String,
) : AuthenticateApi {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun authenticateUser(
        authenticationCredentialsDto: @Valid Mono<AuthenticationCredentialsDto>,
        exchange: ServerWebExchange?,
    ): Mono<ResponseEntity<AuthenticationOkDto>> {
        return authenticationCredentialsDto
            .flatMap(authService::authenticateUser)
            .flatMap(jwtService::generateUserJwtToken)
            .map { token ->
                ResponseEntity.ok(AuthenticationOkDto("$loginRedirectUrl#token=$token"))
            }
    }
}
