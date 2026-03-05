package it.pagopa.ecommerce.watchdog.deadletter.controllers.v1

import it.pagopa.ecommerce.watchdog.deadletter.config.TestSecurityConfig
import it.pagopa.ecommerce.watchdog.deadletter.documents.Action
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
    fun `add action to deadletter-transaction return '201 Created'`() {

        val deadletterTransactionId: String = "00000000"
        val userId: String = "test-user"
        val deadletterTransactionActionInputDto =
            DeadletterTransactionActionInputDto("testActionValue")

        val action = ActionTypeDto("testActionValue", ActionTypeDto.TypeEnum.NOT_FINAL)

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
        val actionType = ActionTypeDto("testActionValue", ActionTypeDto.TypeEnum.NOT_FINAL)
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
    fun `update an existing note will respond with 204 correct status`() {
        val transactionId = "transactionId"
        val noteId = "noteId"
        val noteInputDto = NoteInputDto("noteText")

        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("userId"))
        given(deadletterTransactionsService.updateNote(noteId, noteInputDto.note))
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
        given(deadletterTransactionsService.updateNote(noteId, noteInputDto.note))
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
        given(deadletterTransactionsService.deleteNote("noteId")).willReturn(Mono.just(Unit))

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
        given(deadletterTransactionsService.deleteNote("noteId"))
            .willReturn(Mono.error(InvalidNoteId()))

        webClient
            .delete()
            .uri("/deadletter-transactions/$deadletterTransactionId/notes/$noteId")
            .exchange()
            .expectStatus()
            .isNotFound
    }
}
