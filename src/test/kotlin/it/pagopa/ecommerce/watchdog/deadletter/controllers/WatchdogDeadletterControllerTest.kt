package it.pagopa.ecommerce.watchdog.deadletter.controllers

import it.pagopa.ecommerce.watchdog.deadletter.documents.DeadletterTransactionAction
import it.pagopa.ecommerce.watchdog.deadletter.services.DeadletterTransactionsService
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.DeadletterTransactionActionInputDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.DeadletterTransactionDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ListDeadletterTransactions200ResponseDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.PageInfoDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.context.bean.override.mockito.MockitoBean
import reactor.core.publisher.Mono
import java.time.Instant
import org.mockito.BDDMockito.given
import java.time.LocalDate
import java.util.ArrayList;


@WebFluxTest(WatchdogDeadletterController::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class WatchdogDeadletterControllerTest {
    @Autowired
    private lateinit var webClient: WebTestClient

    @MockitoBean lateinit var deadletterTransactionsService: DeadletterTransactionsService


    @Test
    fun `add action to deadletter-transaction return '202 Accepted'` () {

        val deadletterTransactionId : String = "00000000"
        val xUserId : String = "test-user"
        val deadletterTransactionActionInputDto = DeadletterTransactionActionInputDto("testActionValue")

        given(deadletterTransactionsService.addActionToDeadletterTransaction(deadletterTransactionId, xUserId, deadletterTransactionActionInputDto.value))
            .willReturn(Mono.just(DeadletterTransactionAction("test-id",deadletterTransactionId,xUserId,deadletterTransactionActionInputDto.value,Instant.now())))

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
    fun `add action to deadletter-transaction return '400 BAD REQUEST' missing header user` () {

        val deadletterTransactionId : String = "00000000"
        val xUserId : String = "test-user"
        val deadletterTransactionActionInputDto = DeadletterTransactionActionInputDto("testActionValue")

        given(deadletterTransactionsService.addActionToDeadletterTransaction(deadletterTransactionId, xUserId, deadletterTransactionActionInputDto.value))
            .willReturn(Mono.just(DeadletterTransactionAction("test-id",deadletterTransactionId,xUserId,deadletterTransactionActionInputDto.value,Instant.now())))

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
    fun `add action to deadletter-transaction return '400 BAD REQUEST' missing body` () {

        val deadletterTransactionId : String = "00000000"
        val xUserId : String = "test-user"
        val deadletterTransactionActionInputDto = DeadletterTransactionActionInputDto("testActionValue")

        given(deadletterTransactionsService.addActionToDeadletterTransaction(deadletterTransactionId, xUserId, deadletterTransactionActionInputDto.value))
            .willReturn(Mono.just(DeadletterTransactionAction("test-id",deadletterTransactionId,xUserId,deadletterTransactionActionInputDto.value,Instant.now())))

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
    fun `add action to deadletter-transaction return '400 BAD REQUEST' malformed body` () {

        val deadletterTransactionId : String = "00000000"
        val xUserId : String = "test-user"
        val deadletterTransactionActionInputDto = DeadletterTransactionActionInputDto("testActionValue")
        val malformedBody = """ 
            {
                "wrongparam": 
            }
        """.trimIndent()

        given(deadletterTransactionsService.addActionToDeadletterTransaction(deadletterTransactionId, xUserId, deadletterTransactionActionInputDto.value))
            .willReturn(Mono.just(DeadletterTransactionAction("test-id",deadletterTransactionId,xUserId,deadletterTransactionActionInputDto.value,Instant.now())))

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
    fun `list deadletter transaction return '200 OKAY' with the list of deadletter transactions with pagination` () {
        var date: LocalDate = LocalDate.parse("2025-08-19")
        var pageNumber: Int = 0
        var pageSize: Int = 1
        var deadletterTransactions: List<DeadletterTransactionDto> = ArrayList<DeadletterTransactionDto>()
        var page : PageInfoDto = PageInfoDto(0,1,1)
        val xUserId : String = "test-user"

        given(deadletterTransactionsService.getDeadletterTransactions(date, pageNumber, pageSize))
            .willReturn(Mono.just(ListDeadletterTransactions200ResponseDto(deadletterTransactions, page)))

        webClient
            .get()
            .uri{uriBuilder ->
                    uriBuilder.path("/deadletter-transactions")
                        .queryParam("date",date)
                        .queryParam("pageNumber", pageNumber)
                        .queryParam("pageSize",pageSize)
                        .build()
            }
            .header("x-user-id", xUserId)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
    }


    @Test
    fun `list deadletter transaction return '400 BAD REQUEST' because of a wrong parameter` () {
        var date: LocalDate = LocalDate.parse("2025-08-19")
        var pageNumber: Int = 0
        var pageSize: Int = 1
        var deadletterTransactions: List<DeadletterTransactionDto> = ArrayList<DeadletterTransactionDto>()
        var page : PageInfoDto = PageInfoDto(0,1,1)
        val xUserId : String = "test-user"

        given(deadletterTransactionsService.getDeadletterTransactions(date, pageNumber, pageSize))
            .willReturn(Mono.just(ListDeadletterTransactions200ResponseDto(deadletterTransactions, page)))

        webClient
            .get()
            .uri{uriBuilder ->
                uriBuilder.path("/deadletter-transactions")
                    .queryParam("date","")
                    .queryParam("pageNumber", pageNumber)
                    .queryParam("pageSize",pageSize)
                    .build()
            }
            .header("x-user-id", xUserId)
            .exchange()
            .expectStatus()
            .isBadRequest

    }
}