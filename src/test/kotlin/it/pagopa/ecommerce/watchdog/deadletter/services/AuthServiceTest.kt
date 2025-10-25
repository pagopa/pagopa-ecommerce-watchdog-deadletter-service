package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.documents.Operator
import it.pagopa.ecommerce.watchdog.deadletter.domain.UserDetails
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidCredentialsException
import it.pagopa.ecommerce.watchdog.deadletter.repositories.OperatorsRepository
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@TestPropertySource(locations = ["classpath:application.test.properties"])
class AuthServiceTest {
    private val operatorsRepository: OperatorsRepository = mock()
    private val authService: AuthService = AuthService(operatorsRepository)

    @Test
    fun `should authenticate user`() {
        // pre-requisites
        val credentials = AuthenticationCredentialsDto("user", "password")
        val userDetails = UserDetails("12345", "Mario", "Rossi", "mock@email.com")

        val mockUserEntity =
            mock<Operator> {
                on(it.id).thenReturn(userDetails.id)
                on(it.name).thenReturn(userDetails.name)
                on(it.surname).thenReturn(userDetails.surname)
                on(it.email).thenReturn(userDetails.email)
                on(it.password).thenReturn(credentials.password) // Password corretta
            }

        whenever(operatorsRepository.findById(credentials.username))
            .thenReturn(Mono.just(mockUserEntity))

        // test
        StepVerifier.create(authService.authenticateUser(credentials))
            .expectNext(userDetails)
            .verifyComplete()
    }

    @Test
    fun `should return authenticated user id`() {
        // pre-requisites
        val userDetails = UserDetails("12345", "Mario", "Rossi", "mock@email.com")
        val auth = UsernamePasswordAuthenticationToken(userDetails, null, null)
        val context = SecurityContextImpl(auth)
        val result =
            authService
                .getAuthenticatedUserId()
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)))

        // test
        StepVerifier.create(result).expectNext("12345").verifyComplete()
    }

    fun `should fail authentication for invalid password`() {
        // pre-requisites
        val credentials = AuthenticationCredentialsDto("user", "wrong_password")

        val mockUserEntity = mock<Operator> { on(it.password).thenReturn("correct_password") }

        whenever(operatorsRepository.findById(credentials.username))
            .thenReturn(Mono.just(mockUserEntity))

        // test
        StepVerifier.create(authService.authenticateUser(credentials))
            .expectError(InvalidCredentialsException::class.java)
            .verify()
    }

    @Test
    fun `should fail authentication for user not found`() {
        // pre-requisites
        val credentials = AuthenticationCredentialsDto("unknown_user", "password")

        whenever(operatorsRepository.findById(credentials.username)).thenReturn(Mono.empty())

        // test
        StepVerifier.create(authService.authenticateUser(credentials))
            .expectError(InvalidCredentialsException::class.java)
            .verify()
    }
}
