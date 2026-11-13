package it.pagopa.ecommerce.watchdog.deadletter.config

import it.pagopa.ecommerce.watchdog.deadletter.config.jwt.JwtAuthenticationConverter
import it.pagopa.ecommerce.watchdog.deadletter.controllers.v2.WatchdogDeadletterV2Controller
import it.pagopa.ecommerce.watchdog.deadletter.services.AuthService
import it.pagopa.ecommerce.watchdog.deadletter.services.DeadletterTransactionsService
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.DeadletterTransactionsWithActionsResponseDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.PageInfoDto
import java.time.LocalDate
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest(WatchdogDeadletterV2Controller::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
@Import(SecurityConfig::class)
class InternalApiSecurityTest {
    @Autowired private lateinit var webClient: WebTestClient

    @MockitoBean lateinit var deadletterTransactionsService: DeadletterTransactionsService

    @MockitoBean lateinit var authService: AuthService

    @MockitoBean lateinit var jwtDecoder: ReactiveJwtDecoder

    @MockitoBean lateinit var jwtAuthenticationConverter: JwtAuthenticationConverter

    private fun listUri(prefix: String) =
        "$prefix/deadletter-transactions?fromDate=2025-08-19&toDate=2025-08-20&pageNumber=0&pageSize=1"

    private fun mockService() {
        given(
                deadletterTransactionsService.getDeadletterTransactionsWithActionsByDateRange(
                    LocalDate.parse("2025-08-19"),
                    LocalDate.parse("2025-08-20"),
                    0,
                    1,
                )
            )
            .willReturn(
                Mono.just(
                    DeadletterTransactionsWithActionsResponseDto(emptyList(), PageInfoDto(0, 0, 0))
                )
            )
    }

    @Test
    fun `internal api should be accessible with a valid api key`() {
        mockService()
        webClient
            .get()
            .uri(listUri("/v2/internal"))
            .header(API_KEY_HEADER, "test-internal-api-key")
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun `internal api should return 401 without api key`() {
        webClient.get().uri(listUri("/v2/internal")).exchange().expectStatus().isUnauthorized
    }

    @Test
    fun `internal api should return 401 with a wrong api key`() {
        webClient
            .get()
            .uri(listUri("/v2/internal"))
            .header(API_KEY_HEADER, "wrong")
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `internal api should not accept a jwt`() {
        given(jwtDecoder.decode(any())).willReturn(Mono.error(BadJwtException("invalid")))
        webClient
            .get()
            .uri(listUri("/v2/internal"))
            .header("Authorization", "Bearer token")
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `api key should not grant access to non internal apis`() {
        webClient
            .get()
            .uri(listUri("/v2"))
            .header(API_KEY_HEADER, "test-internal-api-key")
            .exchange()
            .expectStatus()
            .isUnauthorized
    }
}
