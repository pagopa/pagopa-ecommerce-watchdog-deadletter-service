package it.pagopa.ecommerce.watchdog.deadletter.controllers.v2

import it.pagopa.ecommerce.watchdog.deadletter.config.TestSecurityConfig
import it.pagopa.ecommerce.watchdog.deadletter.services.AuthService
import it.pagopa.ecommerce.watchdog.deadletter.services.DeadletterTransactionsService
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.DeadletterTransactionDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.ListDeadletterTransactions200ResponseDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.PageInfoDto
import java.time.LocalDate
import java.util.ArrayList
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest(WatchdogDeadletterV2Controller::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
@Import(TestSecurityConfig::class)
class WatchdogDeadletterV2ControllerTest {
    @Autowired private lateinit var webClient: WebTestClient

    @MockitoBean lateinit var deadletterTransactionsService: DeadletterTransactionsService

    @MockitoBean lateinit var authService: AuthService

    @Test
    fun `list deadletter transaction should return '200 OKAY' with the list of deadletter transactions with pagination`() {
        val fromDate: LocalDate = LocalDate.parse("2025-08-19")
        val toDate: LocalDate = LocalDate.parse("2025-08-20")
        val pageNumber: Int = 0
        val pageSize: Int = 1
        val deadletterTransactions: List<DeadletterTransactionDto> =
            ArrayList<DeadletterTransactionDto>()
        val page: PageInfoDto = PageInfoDto(0, 1, 1)

        given(
                deadletterTransactionsService.getDeadletterTransactionsByDateRange(
                    fromDate,
                    toDate,
                    pageNumber,
                    pageSize,
                )
            )
            .willReturn(
                Mono.just(ListDeadletterTransactions200ResponseDto(deadletterTransactions, page))
            )

        webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/v2/deadletter-transactions")
                    .queryParam("fromDate", fromDate)
                    .queryParam("toDate", toDate)
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
        val fromDate: LocalDate = LocalDate.parse("2025-08-19")
        val toDate: LocalDate = LocalDate.parse("2025-08-20")
        var pageNumber: Int = -1
        var pageSize = 21

        webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/v2/deadletter-transactions")
                    .queryParam("fromDate", fromDate)
                    .queryParam("toDate", toDate)
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
        val fromDate: LocalDate = LocalDate.parse("2025-08-19")
        val toDate: LocalDate = LocalDate.parse("2025-08-20")
        var pageNumber: Int = 1
        var pageSize: Int = 21

        webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/v2/deadletter-transactions")
                    .queryParam("fromDate", fromDate)
                    .queryParam("toDate", toDate)
                    .queryParam("pageNumber", pageNumber)
                    .queryParam("pageSize", pageSize)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `list deadletter transaction should return '400 BAD REQUEST' wrong date format`() {
        val fromDate = "2025-08-19EWR222"
        val toDate: LocalDate = LocalDate.parse("2025-08-20")
        var pageNumber: Int = 0

        webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/v2/deadletter-transactions")
                    .queryParam("fromDate", fromDate)
                    .queryParam("toDate", toDate)
                    .queryParam("pageNumber", pageNumber)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isBadRequest
    }
}
