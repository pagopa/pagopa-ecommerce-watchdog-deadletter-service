package it.pagopa.ecommerce.watchdog.deadletter.controllers

import it.pagopa.ecommerce.watchdog.deadletter.documents.DeadletterTransactionAction
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
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@WebFluxTest(WatchdogDeadletterController::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class WatchdogDeadletterControllerTest {
    @Autowired private lateinit var webClient: WebTestClient

    @MockitoBean lateinit var deadletterTransactionsService: DeadletterTransactionsService

    @Test
    fun `add action to deadletter-transaction return '202 Accepted'`() {

        val deadletterTransactionId: String = "00000000"
        val xUserId: String = "test-user"
        val deadletterTransactionActionInputDto =
            DeadletterTransactionActionInputDto("testActionValue")

        given(
                deadletterTransactionsService.addActionToDeadletterTransaction(
                    deadletterTransactionId,
                    xUserId,
                    deadletterTransactionActionInputDto.value,
                )
            )
            .willReturn(
                Mono.just(
                    DeadletterTransactionAction(
                        "test-id",
                        deadletterTransactionId,
                        xUserId,
                        deadletterTransactionActionInputDto.value,
                        Instant.now(),
                    )
                )
            )

        webClient
            .post()
            .uri("/deadletter-transactions/$deadletterTransactionId/actions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-user-id", xUserId)
            .bodyValue(deadletterTransactionActionInputDto)
            .exchange()
            .expectStatus()
            .isAccepted
    }

    @Test
    fun `add action to deadletter-transaction return '400 BAD REQUEST' missing missing x-user-id in the header`() {

        val deadletterTransactionId: String = "00000000"
        val deadletterTransactionActionInputDto =
            DeadletterTransactionActionInputDto("testActionValue")

        webClient
            .post()
            .uri("/deadletter-transactions/$deadletterTransactionId/actions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(deadletterTransactionActionInputDto)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `add action to deadletter-transaction return '400 BAD REQUEST' missing body`() {

        val deadletterTransactionId: String = "00000000"
        val xUserId: String = "test-user"

        webClient
            .post()
            .uri("/deadletter-transactions/$deadletterTransactionId/actions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-user-id", xUserId)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `add action to deadletter-transaction should return '400 BAD REQUEST' malformed body`() {

        val deadletterTransactionId: String = "00000000"
        val xUserId: String = "test-user"
        val malformedBody =
            """ 
            {
                "wrongparam": 
            }
        """
                .trimIndent()

        webClient
            .post()
            .uri("/deadletter-transactions/$deadletterTransactionId/actions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-user-id", xUserId)
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
        val xUserId: String = "test-user"

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
            .header("x-user-id", xUserId)
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
        val xUserId: String = "test-user"

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
            .header("x-user-id", xUserId)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `list deadletter transaction should return '400 BAD REQUEST' because of constrain violation of pageSize`() {
        var date: LocalDate = LocalDate.parse("2025-08-19")
        var pageNumber: Int = 1
        var pageSize: Int = -20
        val xUserId: String = "test-user"

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
            .header("x-user-id", xUserId)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `list deadletter transaction should return '400 BAD REQUEST' missing parameter`() {
        var date: LocalDate = LocalDate.parse("2025-08-19")
        var pageNumber: Int = 0
        val xUserId: String = "test-user"

        webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/deadletter-transactions")
                    .queryParam("date", date)
                    .queryParam("pageNumber", pageNumber)
                    .build()
            }
            .header("x-user-id", xUserId)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `list deadletter transaction should return '400 BAD REQUEST' wrong date format`() {
        var date: String = "2025-08-19EWR222"
        var pageNumber: Int = 0
        val xUserId: String = "test-user"

        webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/deadletter-transactions")
                    .queryParam("date", date)
                    .queryParam("pageNumber", pageNumber)
                    .build()
            }
            .header("x-user-id", xUserId)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `list deadletter transaction should return '400 BAD REQUEST' because of missing x-user-id in the header`() {
        var date: LocalDate = LocalDate.parse("2025-08-19")
        var pageNumber: Int = 0
        var pageSize: Int = 1
        var deadletterTransactions: List<DeadletterTransactionDto> =
            ArrayList<DeadletterTransactionDto>()
        var page: PageInfoDto = PageInfoDto(0, 1, 1)

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
            .isBadRequest
    }

    @Test
    fun `list actions for deadletter transaction should return '200 OKAY' and return the list of action in the body`() {
        val deadletterTransactionId: String = "00000000"
        val xUserId: String = "test-user"
        val deadletterTransactionAction: DeadletterTransactionAction =
            DeadletterTransactionAction(
                "test-id",
                deadletterTransactionId,
                xUserId,
                "testvalue",
                Instant.now(),
            )

        given(
                deadletterTransactionsService.listActionsForDeadletterTransaction(
                    deadletterTransactionId,
                    xUserId,
                )
            )
            .willReturn(Flux.just<DeadletterTransactionAction>(deadletterTransactionAction))

        webClient
            .get()
            .uri("/deadletter-transactions/$deadletterTransactionId/actions")
            .header("x-user-id", xUserId)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
    }

    @Test
    fun `list actions for deadletter transaction should return '400 BAD REQUEST' because missing x-user-id`() {
        val deadletterTransactionId: String = "00000000"

        webClient
            .get()
            .uri("/deadletter-transactions/$deadletterTransactionId/actions")
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `list actions for deadletter transaction should return '404 NOT FOUND' because the deadletterTransaction doesn't exist`() {
        // TO DO
    }
}
