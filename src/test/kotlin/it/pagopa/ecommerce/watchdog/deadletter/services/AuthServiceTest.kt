package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.documents.User
import it.pagopa.ecommerce.watchdog.deadletter.domain.UserDetails
import it.pagopa.ecommerce.watchdog.deadletter.repositories.UserRepository
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@TestPropertySource(locations = ["classpath:application.test.properties"])
class AuthServiceTest {
    private val userRepository: UserRepository = mock()
    private val authService: AuthService = AuthService(userRepository)

    @Test
    fun `should authenticate user`() {
        // pre-requisites
        val credentials = AuthenticationCredentialsDto("user", "password")
        val userDetails = UserDetails("12345", "Mario", "Rossi", "mock@email.com")

        val mockUserEntity =
            mock<User> {
                on(it.id).thenReturn(userDetails.id)
                on(it.name).thenReturn(userDetails.name)
                on(it.surname).thenReturn(userDetails.surname)
                on(it.email).thenReturn(userDetails.email)
                on(it.password).thenReturn(credentials.password) // Password corretta
            }

        whenever(userRepository.findByUsername(credentials.username))
            .thenReturn(Mono.just(mockUserEntity))

        // test
        StepVerifier.create(authService.authenticateUser(credentials))
            .expectNext(userDetails)
            .verifyComplete()
    }
}
