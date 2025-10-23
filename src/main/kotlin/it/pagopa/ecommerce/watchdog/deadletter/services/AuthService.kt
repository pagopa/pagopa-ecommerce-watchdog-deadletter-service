import it.pagopa.ecommerce.watchdog.deadletter.domain.UserDetails
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidCredentialsException
import it.pagopa.ecommerce.watchdog.deadletter.repositories.UserRepository
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuthService(private val userRepository: UserRepository) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * Authenticates a user based on the provided credentials.
     *
     * **NOTE:** This method currently uses plain-text password comparison.
     * This is a temporary implementation and will be replaced with a secure
     * PasswordEncoder (e.g., BCrypt) in a dedicated future Pull Request.
     *
     * @param credentials The user's login credentials (username and password).
     * @return A [Mono] emitting [UserDetails] on success, or an [InvalidCredentialsException]
     * if the username is not found or the password does not match.
     */
    fun authenticateUser(incomingCredentials: AuthenticationCredentialsDto): Mono<UserDetails> {
        return userRepository
            .findByUsername(incomingCredentials.username)
            .flatMap { user ->
                // Plain-text password comparison (temporary)!
                if (user.password == incomingCredentials.password) {
                    logger.debug("Authentication successful for user: ${incomingCredentials.username}")
                    Mono.just(UserDetails(user.id, user.name, user.surname, user.email))
                } else {
                    logger.warn("Authentication failed for user: ${incomingCredentials.username}. Invalid password.")
                    Mono.error(InvalidCredentialsException())
                }
            }
            .switchIfEmpty(
                Mono.defer {
                    logger.warn("Authentication failed. User not found: ${incomingCredentials.username}")
                    Mono.error(InvalidCredentialsException())
                }
            )
    }
}