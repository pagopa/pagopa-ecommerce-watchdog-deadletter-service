package it.pagopa.ecommerce.watchdog.deadletter.controllers.v1

import it.pagopa.ecommerce.watchdog.deadletter.config.JacksonConfig
import it.pagopa.ecommerce.watchdog.deadletter.config.TestSecurityConfig
import it.pagopa.ecommerce.watchdog.deadletter.documents.Action
import it.pagopa.ecommerce.watchdog.deadletter.documents.ActionType
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidNoteId
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidTransactionId
import it.pagopa.ecommerce.watchdog.deadletter.exception.NotesLimitException
import it.pagopa.ecommerce.watchdog.deadletter.services.AuthService
import it.pagopa.ecommerce.watchdog.deadletter.services.DeadletterTransactionsService
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.openapitools.jackson.nullable.JsonNullable
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
@Import(TestSecurityConfig::class, JacksonConfig::class)
class WatchdogDeadletterControllerTest {
    @Autowired private lateinit var webClient: WebTestClient

    @MockitoBean lateinit var deadletterTransactionsService: DeadletterTransactionsService

    @MockitoBean lateinit var authService: AuthService

    @Test
    fun `add action to deadletter-transaction return '201 Created'`() {

        val deadletterTransactionId: String = "00000000"
        val userId: String = "test-user"
        val deadletterTransactionActionInputDto =
            DeadletterTransactionActionInputDto("testActionValue")

        val action = ActionType("testActionValue", ActionType.Type.NOT_FINAL)

        given(
                deadletterTransactionsService.addActionToDeadletterTransaction(
                    deadletterTransactionId,
                    userId,
                    deadletterTransactionActionInputDto.value,
                )
            )
            .willReturn(
                Mono.just(Action("test-id", deadletterTransactionId, userId, action, Instant.now()))
            )
        given(authService.getAuthenticatedUserId()).willReturn(Mono.just(userId))

        webClient
            .post()
            .uri("/deadletter-transactions/$deadletterTransactionId/actions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(deadletterTransactionActionInputDto)
            .exchange()
            .expectStatus()
            .isCreated
    }

    @Test
    fun `add action to deadletter-transaction return '404 NOT FOUND' transaction doesn't exist`() {

        val deadletterTransactionId: String = "00000000"
        val userId: String = "test-user"
        val deadletterTransactionActionInputDto =
            DeadletterTransactionActionInputDto("Nessuna azione richiesta")

        given(authService.getAuthenticatedUserId()).willReturn(Mono.just(userId))
        given(
                deadletterTransactionsService.addActionToDeadletterTransaction(
                    deadletterTransactionId,
                    userId,
                    deadletterTransactionActionInputDto.value,
                )
            )
            .willReturn(Mono.error(InvalidTransactionId()))

        webClient
            .post()
            .uri("/deadletter-transactions/$deadletterTransactionId/actions")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(deadletterTransactionActionInputDto)
            .exchange()
            .expectStatus()
            .isNotFound
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
    fun `add action to deadletter transactions return '201 Created'`() {

        val deadletterTransactionIds = listOf("00000000", "00000001")
        val userId = "test-user"
        val action = ActionType("Nessuna azione richiesta", ActionType.Type.FINAL)
        val input = DeadletterTransactionsActionInputDto(deadletterTransactionIds, action.value)

        given(deadletterTransactionsService.addActionToDeadletterTransactions(input, userId))
            .willReturn(
                Mono.just(
                    listOf(
                        Action(
                            "test-id",
                            deadletterTransactionIds[0],
                            userId,
                            action,
                            Instant.now(),
                        ),
                        Action(
                            "test-id",
                            deadletterTransactionIds[1],
                            userId,
                            action,
                            Instant.now(),
                        ),
                    )
                )
            )

        given(authService.getAuthenticatedUserId()).willReturn(Mono.just(userId))

        webClient
            .post()
            .uri("/deadletter-transactions/actions/bulk")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(input)
            .exchange()
            .expectStatus()
            .isCreated
    }

    @Test
    fun `add action to deadletter transactions return '404 NOT FOUND' when a single transaction doesn't exist`() {

        val deadletterTransactionIds = listOf("00000000", "00000001")
        val userId = "test-user"
        val input =
            DeadletterTransactionsActionInputDto(
                deadletterTransactionIds,
                "Nessuna azione richiesta",
            )

        given(authService.getAuthenticatedUserId()).willReturn(Mono.just(userId))
        given(deadletterTransactionsService.addActionToDeadletterTransactions(input, userId))
            .willReturn(Mono.error(InvalidTransactionId()))

        webClient
            .post()
            .uri("/deadletter-transactions/actions/bulk")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(input)
            .exchange()
            .expectStatus()
            .isNotFound
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
        val actionType = ActionType("testActionValue", ActionType.Type.NOT_FINAL)
        val action: Action =
            Action("test-id", deadletterTransactionId, userId, actionType, Instant.now())

        given(
                deadletterTransactionsService.listActionsForDeadletterTransaction(
                    deadletterTransactionId,
                    userId,
                )
            )
            .willReturn(Flux.just<Action>(action))

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
    fun `list actions for deadletter transaction should return '404 NOT FOUND' because the deadletterTransaction doesn't exist`() {}

    @Test
    fun `add a new note to a transaction`() {

        val noteInputDto = NoteInputDto("noteText")
        val noteDto =
            NoteDto(
                "noteText",
                "noteId",
                "transactionId",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                "userId",
            )

        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("userId"))
        given(
                deadletterTransactionsService.addNoteToDeadLetterTransaction(
                    "noteText",
                    "userId",
                    "transactionId",
                )
            )
            .willReturn(Mono.just(noteDto))

        val deadletterTransactionId = "transactionId"

        webClient
            .post()
            .uri("/deadletter-transactions/$deadletterTransactionId/notes")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(noteInputDto)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody(noteInputDto::class.java)
    }

    @Test
    fun `add a new note to a transaction should return a error 404 because the transaction doesnt exist`() {

        val noteInputDto = NoteInputDto("noteText")

        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("userId"))
        given(
                deadletterTransactionsService.addNoteToDeadLetterTransaction(
                    "noteText",
                    "userId",
                    "transactionId",
                )
            )
            .willReturn(Mono.error(InvalidTransactionId()))

        val deadletterTransactionId = "transactionId"

        webClient
            .post()
            .uri("/deadletter-transactions/$deadletterTransactionId/notes")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(noteInputDto)
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `add a new note to a transaction should return a error 422 because there are too many notes`() {

        val noteInputDto = NoteInputDto("noteText")

        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("userId"))
        given(
                deadletterTransactionsService.addNoteToDeadLetterTransaction(
                    "noteText",
                    "userId",
                    "transactionId",
                )
            )
            .willReturn(Mono.error(NotesLimitException()))

        val deadletterTransactionId = "transactionId"

        webClient
            .post()
            .uri("/deadletter-transactions/$deadletterTransactionId/notes")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(noteInputDto)
            .exchange()
            .expectStatus()
            .isEqualTo(422)
    }

    @Test
    fun `add a new note to multiple transactions`() {

        val transactionIds = listOf("transactionId1", "transactionId2")

        val notesInputDto = NotesInputDto(transactionIds, "noteText")
        val notesDto =
            Flux.just(
                NoteDto(
                    "noteText",
                    "noteId",
                    "transactionId1",
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    "userId",
                ),
                NoteDto(
                    "noteText",
                    "noteId",
                    "transactionId2",
                    OffsetDateTime.now(),
                    OffsetDateTime.now(),
                    "userId",
                ),
            )

        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("userId"))
        given(
                deadletterTransactionsService.addNoteToDeadLetterTransactions(
                    "noteText",
                    "userId",
                    transactionIds,
                )
            )
            .willReturn(notesDto)

        webClient
            .post()
            .uri("/deadletter-transactions/notes/bulk")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(notesInputDto)
            .exchange()
            .expectStatus()
            .isCreated
    }

    @Test
    fun `add a new note to multiple transaction should return a error 404 because the transaction doesnt exist`() {

        val notesInputDto = NotesInputDto(listOf("transactionId1", "transactionId2"), "noteText")

        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("userId"))
        given(
                deadletterTransactionsService.addNoteToDeadLetterTransactions(
                    "noteText",
                    "userId",
                    listOf("transactionId1", "transactionId2"),
                )
            )
            .willReturn(Flux.error(InvalidTransactionId()))

        webClient
            .post()
            .uri("/deadletter-transactions/notes/bulk")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(notesInputDto)
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `add a new note to multiple transactions should return a error 422 because there are too many notes`() {

        val notesInputDto = NotesInputDto(listOf("transactionId1", "transactionId2"), "noteText")

        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("userId"))
        given(
                deadletterTransactionsService.addNoteToDeadLetterTransactions(
                    "noteText",
                    "userId",
                    listOf("transactionId1", "transactionId2"),
                )
            )
            .willReturn(Flux.error(NotesLimitException()))

        webClient
            .post()
            .uri("/deadletter-transactions/notes/bulk")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(notesInputDto)
            .exchange()
            .expectStatus()
            .isEqualTo(422)
    }

    @Test
    fun `update an existing note will respond with 204 correct status`() {
        val transactionId = "transactionId"
        val noteId = "noteId"
        val noteInputDto = NoteInputDto("noteText")

        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("userId"))
        given(deadletterTransactionsService.updateNote(noteId, noteInputDto.note, "userId"))
            .willReturn(Mono.just(1L))

        webClient
            .put()
            .uri("/deadletter-transactions/$transactionId/notes/$noteId")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(noteInputDto)
            .exchange()
            .expectStatus()
            .isNoContent
    }

    @Test
    fun `update an existing note will respond with 404 because the transaction or the note doent exist`() {
        val transactionId = "transactionId"
        val noteId = "noteId"
        val noteInputDto = NoteInputDto("noteText")

        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("userId"))
        given(deadletterTransactionsService.updateNote(noteId, noteInputDto.note, "userId"))
            .willReturn(Mono.error(InvalidNoteId()))

        webClient
            .put()
            .uri("/deadletter-transactions/$transactionId/notes/$noteId")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(noteInputDto)
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `get all the notes of a given list of transactionId`() {
        val transactionIds = ArrayList<String>()
        transactionIds.add("testId")
        val notesRequestDto = NotesRequestDto(transactionIds)

        val noteList = ArrayList<NoteDto>()
        noteList.add(
            NoteDto(
                "noteText",
                "noteId",
                "transactionId",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                "userId",
            )
        )
        val transactionNotesDto = TransactionNotesDto("testId", noteList)

        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("userId"))
        given(deadletterTransactionsService.getAllNotesByTransactionIdList(transactionIds))
            .willReturn(Flux.just(transactionNotesDto))

        webClient
            .post()
            .uri("/deadletter-transactions/notes")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(notesRequestDto)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
    }

    @Test
    fun `delete a note`() {
        val deadletterTransactionId = "transactionId"
        val noteId = "noteId"
        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("userId"))
        given(deadletterTransactionsService.deleteNote("noteId", "userId"))
            .willReturn(Mono.just(Unit))

        webClient
            .delete()
            .uri("/deadletter-transactions/$deadletterTransactionId/notes/$noteId")
            .exchange()
            .expectStatus()
            .isNoContent
    }

    @Test
    fun `delete a note will return error 404 because the note or the transaction doesnt exist`() {
        val deadletterTransactionId = "transactionId"
        val noteId = "noteId"
        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("userId"))
        given(deadletterTransactionsService.deleteNote("noteId", "userId"))
            .willReturn(Mono.error(InvalidNoteId()))

        webClient
            .delete()
            .uri("/deadletter-transactions/$deadletterTransactionId/notes/$noteId")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `get stats should return '200 OKAY' with the stats of the month`() {
        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("userId"))
        given(deadletterTransactionsService.getDailyStats(any(), any()))
            .willReturn(Mono.just(MonthStatsResponseDto()))

        webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/deadletter-transactions/stats")
                    .queryParam("year", 2026)
                    .queryParam("month", 7)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
    }

    @Test
    fun `get stats should return '400 BAD REQUEST' when required params are missing`() {
        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("userId"))

        webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder.path("/deadletter-transactions/stats").queryParam("month", 7).build()
            }
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `get stats should return '400 BAD REQUEST' when required params are out of range`() {
        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("userId"))
        given(deadletterTransactionsService.getDailyStats(any(), any()))
            .willReturn(Mono.just(MonthStatsResponseDto()))

        webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/deadletter-transactions/stats")
                    .queryParam("year", 3026)
                    .queryParam("month", 13)
                    .build()
            }
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `POST stats should return the updated stats when a correct payload is passed`() {

        val objResponse =
            MonthStatsResponseDto().apply {
                stats =
                    listOf(
                        MonthStatsResponseStatsInnerDto().apply {
                            date = LocalDate.parse("2026-08-05")
                            finalized = 1
                            notFinalized = 1
                            notAnalyzed = JsonNullable.of(10)
                        },
                        MonthStatsResponseStatsInnerDto().apply {
                            date = LocalDate.parse("2026-08-06")
                            finalized = 0
                            notFinalized = 2
                            notAnalyzed = JsonNullable.of(0)
                        },
                    )
            }
        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("userId"))
        given(deadletterTransactionsService.updateHistoricStats(any(), any()))
            .willReturn(Mono.just(objResponse))

        webClient
            .post()
            .uri { uriBuilder -> uriBuilder.path("/deadletter-transactions/stats").build() }
            .bodyValue(
                UpdateStatsRequestDto().apply {
                    from = LocalDate.parse("2026-08-04")
                    to = LocalDate.parse("2026-08-07")
                }
            )
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.stats[0].notAnalyzed")
            .isNumber
            .jsonPath("$.stats[1].notAnalyzed")
            .isNumber
    }
}
