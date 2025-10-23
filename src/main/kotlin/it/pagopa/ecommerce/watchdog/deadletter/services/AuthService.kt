package it.pagopa.ecommerce.watchdog.deadletter.services

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

    fun authenticateUser(credentials: AuthenticationCredentialsDto): Mono<UserDetails> {
        return userRepository
            .findByUsername(credentials.username)
            .flatMap { user ->
                if (user.password == credentials.password)
                    Mono.just(UserDetails(user.id, user.name, user.surname, user.email))
                else Mono.error(InvalidCredentialsException())
            }
            .switchIfEmpty(Mono.error(InvalidCredentialsException()))
        // TODO: Validate user password
    }
}
