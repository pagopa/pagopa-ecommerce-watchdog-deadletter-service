package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.domain.UserDetails
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidCredentialsException
import it.pagopa.ecommerce.watchdog.deadletter.repositories.OperatorsRepository
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuthService(private val userRepository: OperatorsRepository) {

    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val encoder = BCryptPasswordEncoder(16)

    /**
     * Authenticates a user based on the provided credentials using Spring Security PasswordEncoder
     * (BCrypt).
     *
     * @param incomingCredentials The user's login credentials (username and password).
     * @return A [Mono] emitting [UserDetails] on success, or an [InvalidCredentialsException] if
     *   the username is not found or the password does not match.
     */
    fun authenticateUser(incomingCredentials: AuthenticationCredentialsDto): Mono<UserDetails> {
        return userRepository
            .findById(incomingCredentials.username)
            .flatMap { user ->
                // Using spring security encoder to verify
                if (encoder.matches(incomingCredentials.password, user.password)) {
                    logger.debug(
                        "Authentication successful for user: ${incomingCredentials.username}"
                    )
                    Mono.just(UserDetails(user.id, user.name, user.surname, user.email))
                } else {
                    logger.warn(
                        "Authentication failed for user: ${incomingCredentials.username}. Invalid password."
                    )
                    Mono.error(InvalidCredentialsException())
                }
            }
            .switchIfEmpty(
                Mono.defer {
                    logger.warn(
                        "Authentication failed. User not found: ${incomingCredentials.username}"
                    )
                    Mono.error(InvalidCredentialsException())
                }
            )
    }

    /**
     * Returns the current authenticated user's id.
     *
     * @return A [Mono] emitting the user's id on success, or an [IllegalStateException] if a valid
     *   [UserDetails] authentication principal is not found in Spring SecurityContext.
     */
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
