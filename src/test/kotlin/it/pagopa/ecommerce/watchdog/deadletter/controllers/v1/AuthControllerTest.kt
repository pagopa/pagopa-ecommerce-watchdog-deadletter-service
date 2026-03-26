package it.pagopa.ecommerce.watchdog.deadletter.controllers.v1

import it.pagopa.ecommerce.watchdog.deadletter.config.TestSecurityConfig
import it.pagopa.ecommerce.watchdog.deadletter.domain.UserDetails
import it.pagopa.ecommerce.watchdog.deadletter.exception.UserUnauthorizedException
import it.pagopa.ecommerce.watchdog.deadletter.services.AuthService
import it.pagopa.ecommerce.watchdog.deadletter.services.JwtService
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest(AuthController::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
@Import(TestSecurityConfig::class)
class AuthControllerTest {
    @Autowired private lateinit var webClient: WebTestClient

    @MockitoBean lateinit var authService: AuthService

    @MockitoBean lateinit var jwtService: JwtService

    @Test
    fun `should return 200 Ok with redirectUrl`() {
        // pre-requisites
        val request = AuthenticationCredentialsDto("user", "password")
        val redirectUrl = "http://mock#token=123"

        given(authService.authenticateUser(request))
            .willReturn(Mono.just(UserDetails("id", "Name", "Surname", "test@email.com")))

        given(jwtService.generateUserJwtToken(any())).willReturn(Mono.just("123"))

        // test
        webClient
            .post()
            .uri("/authenticate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .json("{urlRedirect: \"$redirectUrl\"}")
    }

    @Test
    fun `should return 400 Bad Request when body field is missing`() {
        // pre-requisites
        val malformedJson =
            """
                {
                    "password": "somePassword"
                }
            """
                .trimIndent()

        // test
        webClient
            .post()
            .uri("/authenticate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(malformedJson)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `should return 401 Unauthorized on UserUnauthorizedException`() {
        // pre-requisites
        val request = AuthenticationCredentialsDto("test_unauthorized", "password")

        given(authService.authenticateUser(request))
            .willReturn(Mono.error(UserUnauthorizedException()))

        // test
        webClient
            .post()
            .uri("/authenticate")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isUnauthorized
    }
}
