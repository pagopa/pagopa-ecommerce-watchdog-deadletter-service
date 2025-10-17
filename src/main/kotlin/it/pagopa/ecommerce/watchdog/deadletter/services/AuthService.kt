package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.domain.UserDetails
import it.pagopa.ecommerce.watchdog.deadletter.exception.UserUnauthorizedException
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuthService() {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun authenticateUser(credentials: AuthenticationCredentialsDto): Mono<UserDetails> {
        return if (credentials.username == "test_unauthorized")
            Mono.error(UserUnauthorizedException())
        else Mono.just(UserDetails("12345", "Mario", "Rossi", "mock@email.com"))
        // TODO: Get user by username from DB
        // TODO: Validate user password
    }

    fun getAuthenticatedUserId(): Mono<String> {
        return ReactiveSecurityContextHolder.getContext().flatMap { securityContext ->
            when (val principal = securityContext.authentication.principal) {
                is UserDetails -> Mono.just(principal.id)
                else ->
                    Mono.error(IllegalStateException("User details not found in security context"))
            }
        }
    }
}
