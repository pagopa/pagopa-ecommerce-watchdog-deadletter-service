package it.pagopa.ecommerce.watchdog.deadletter.controllers

import it.pagopa.ecommerce.watchdog.deadletter.services.AuthService
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationCredentialsDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.AuthenticationOkDto
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest(AuthController::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class AuthControllerTest {
    @Autowired private lateinit var webClient: WebTestClient

    @MockitoBean lateinit var authService: AuthService

    @Test
    fun `should return 200 Ok with redirectUrl`() {
        // pre-requisites
        val request = AuthenticationCredentialsDto("user", "password")
        val redirectUrl = "http://localhost/#token=123"

        given(authService.authenticateUser(request))
            .willReturn(Mono.just(AuthenticationOkDto(redirectUrl)))

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
}
