package it.pagopa.ecommerce.watchdog.deadletter.controllers

import it.pagopa.ecommerce.watchdog.deadletter.config.TestSecurityConfig
import it.pagopa.ecommerce.watchdog.deadletter.documents.DeadletterTransactionAction
import it.pagopa.ecommerce.watchdog.deadletter.services.AuthService
import it.pagopa.ecommerce.watchdog.deadletter.services.DeadletterTransactionsService
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.DeadletterTransactionActionInputDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.DeadletterTransactionDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ListDeadletterTransactions200ResponseDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.PageInfoDto
import java.time.Instant
import java.time.LocalDate
import java.util.ArrayList
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@WebFluxTest(WatchdogDeadletterController::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
@Import(TestSecurityConfig::class)
class WatchdogDeadletterControllerTest {
    @Autowired private lateinit var webClient: WebTestClient

    @MockitoBean lateinit var deadletterTransactionsService: DeadletterTransactionsService

    @MockitoBean lateinit var authService: AuthService

    @Test
    fun `add action to deadletter-transaction return '202 Accepted'`() {

        val deadletterTransactionId: String = "00000000"
        val userId: String = "test-user"
        val deadletterTransactionActionInputDto =
            DeadletterTransactionActionInputDto("testActionValue")

        given(
                deadletterTransactionsService.addActionToDeadletterTransaction(
                    deadletterTransactionId,
                    userId,
                    deadletterTransactionActionInputDto.value,
                )
            )
            .willReturn(
                Mono.just(
                    DeadletterTransactionAction(
                        "test-id",
                        deadletterTransactionId,
                        userId,
                        deadletterTransactionActionInputDto.value,
                        Instant.now(),
                    )
                )
            )
        given(authService.getAuthenticatedUserId()).willReturn(Mono.just(userId))

        webClient
            .post()
            .uri("/deadletter-transactions/$deadletterTransactionId/actions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(deadletterTransactionActionInputDto)
            .exchange()
            .expectStatus()
            .isAccepted
    }

    @Test
    fun `add action to deadletter-transaction return '400 BAD REQUEST' missing body`() {

        val deadletterTransactionId: String = "00000000"
        val userId: String = "test-user"

        given(authService.getAuthenticatedUserId()).willReturn(Mono.just(userId))

        webClient
            .post()
            .uri("/deadletter-transactions/$deadletterTransactionId/actions")
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `add action to deadletter-transaction should return '400 BAD REQUEST' malformed body`() {

        val deadletterTransactionId: String = "00000000"
        val userId: String = "test-user"
        val malformedBody =
            """ 
            {
                "wrongparam": 
            }
        """
                .trimIndent()

        given(authService.getAuthenticatedUserId()).willReturn(Mono.just(userId))

        webClient
            .post()
            .uri("/deadletter-transactions/$deadletterTransactionId/actions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(malformedBody)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `list deadletter transaction should return '200 OKAY' with the list of deadletter transactions with pagination`() {
        val date: LocalDate = LocalDate.parse("2025-08-19")
        val pageNumber: Int = 0
        val pageSize: Int = 1
        val deadletterTransactions: List<DeadletterTransactionDto> =
            ArrayList<DeadletterTransactionDto>()
        val page: PageInfoDto = PageInfoDto(0, 1, 1)

        given(deadletterTransactionsService.getDeadletterTransactions(date, pageNumber, pageSize))
            .willReturn(
                Mono.just(ListDeadletterTransactions200ResponseDto(deadletterTransactions, page))
            )

        webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/deadletter-transactions")
                    .queryParam("date", date)
                    .queryParam("pageNumber", pageNumber)
                    .queryParam("pageSize", pageSize)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
    }

    @Test
    fun `list deadletter transaction should return '400 BAD REQUEST' because of constrain violation of pageNumber`() {
        var date: LocalDate = LocalDate.parse("2025-08-19")
        var pageNumber: Int = -1
        var pageSize: Int = 20

        webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/deadletter-transactions")
                    .queryParam("date", date)
                    .queryParam("pageNumber", pageNumber)
                    .queryParam("pageSize", pageSize)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `list deadletter transaction should return '400 BAD REQUEST' because of constrain violation of pageSize`() {
        var date: LocalDate = LocalDate.parse("2025-08-19")
        var pageNumber: Int = 1
        var pageSize: Int = -20

        webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/deadletter-transactions")
                    .queryParam("date", date)
                    .queryParam("pageNumber", pageNumber)
                    .queryParam("pageSize", pageSize)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `list deadletter transaction should return '400 BAD REQUEST' missing parameter`() {
        var date: LocalDate = LocalDate.parse("2025-08-19")
        var pageNumber: Int = 0

        webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/deadletter-transactions")
                    .queryParam("date", date)
                    .queryParam("pageNumber", pageNumber)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `list deadletter transaction should return '400 BAD REQUEST' wrong date format`() {
        var date: String = "2025-08-19EWR222"
        var pageNumber: Int = 0

        webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/deadletter-transactions")
                    .queryParam("date", date)
                    .queryParam("pageNumber", pageNumber)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `list actions for deadletter transaction should return '200 OKAY' and return the list of action in the body`() {
        val deadletterTransactionId: String = "00000000"
        val userId: String = "test-user"
        val deadletterTransactionAction: DeadletterTransactionAction =
            DeadletterTransactionAction(
                "test-id",
                deadletterTransactionId,
                userId,
                "testvalue",
                Instant.now(),
            )

        given(
                deadletterTransactionsService.listActionsForDeadletterTransaction(
                    deadletterTransactionId,
                    userId,
                )
            )
            .willReturn(Flux.just<DeadletterTransactionAction>(deadletterTransactionAction))

        given(authService.getAuthenticatedUserId()).willReturn(Mono.just(userId))

        webClient
            .get()
            .uri("/deadletter-transactions/$deadletterTransactionId/actions")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
    }

    @Test
    fun `list actions for deadletter transaction should return '404 NOT FOUND' because the deadletterTransaction doesn't exist`() {
        // TO DO
    }
}
