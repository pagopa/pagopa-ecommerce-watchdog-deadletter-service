package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.domain.UserDetails
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidCredentialsException
import it.pagopa.ecommerce.watchdog.deadletter.repositories.OperatorsRepository
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import org.slf4j.LoggerFactory
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
     * @param credentials The user's login credentials (username and password).
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
}
