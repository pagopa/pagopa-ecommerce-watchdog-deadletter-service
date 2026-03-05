package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.clients.EcommerceHelpdeskServiceClient
import it.pagopa.ecommerce.watchdog.deadletter.clients.NodoTechnicalSupportClient
import it.pagopa.ecommerce.watchdog.deadletter.config.ActionTypeConfig
import it.pagopa.ecommerce.watchdog.deadletter.documents.Action
import it.pagopa.ecommerce.watchdog.deadletter.documents.Note
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidActionValue
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidNoteId
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidTransactionId
import it.pagopa.ecommerce.watchdog.deadletter.exception.NotesLimitException
import it.pagopa.ecommerce.watchdog.deadletter.repositories.DeadletterTransactionActionRepository
import it.pagopa.ecommerce.watchdog.deadletter.repositories.DeadletterTransactionNoteRepository
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterEventDto
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterTransactionInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.PageInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.PaymentDetailInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.PaymentInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchDeadLetterEventResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchNpgOperationsResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionResultDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
import it.pagopa.generated.ecommerce.helpdesk.model.UserInfoDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ActionTypeDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ListDeadletterTransactions200ResponseDto
import it.pagopa.generated.nodo.support.model.TransactionResponseDto
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.mockito.kotlin.argumentCaptor
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class DeadletterTransactionServiceTest {

    private val noteNumLimit: Long = 10

    private val ecommerceHelpdeskServiceV1: EcommerceHelpdeskServiceClient = mock()
    private val nodoTechnicalSupportClient: NodoTechnicalSupportClient = mock()
    private val deadletterTransactionActionRepository: DeadletterTransactionActionRepository =
        mock()
    private val actionConfig: ActionTypeConfig = ActionTypeConfig()
    private val deadletterTransactionsNoteRepo: DeadletterTransactionNoteRepository = mock()
    private val deadletterTransactionsService: DeadletterTransactionsService =
        DeadletterTransactionsService(
            ecommerceHelpdeskServiceV1,
            nodoTechnicalSupportClient,
            deadletterTransactionActionRepository,
            deadletterTransactionsNoteRepo,
            actionConfig,
            noteNumLimit,
        )

    @Test
    fun `getDeadletterTransactions should return a ListDeadletterTransactions200ResponseDto with an element`() {
        val date: LocalDate = LocalDate.parse("2025-08-19")
        val pageNumber = 0
        val pageSize = 1

        val transactionInfo = DeadLetterTransactionInfoDto()
        transactionInfo.transactionId = "testTransactionId"
        transactionInfo.paymentGateway = "NPG"

        val deadletterEventDto = DeadLetterEventDto()
        deadletterEventDto.data = "2025-08-19"
        deadletterEventDto.transactionInfo = transactionInfo

        val deadletterEvents: ArrayList<DeadLetterEventDto> = ArrayList<DeadLetterEventDto>()
        deadletterEvents.add(deadletterEventDto)

        val searchDeadLetterEventResponseDto = SearchDeadLetterEventResponseDto()
        searchDeadLetterEventResponseDto.deadLetterEvents = deadletterEvents
        val pageInfoDto = PageInfoDto()
        pageInfoDto.current = 0
        pageInfoDto.total = 0
        searchDeadLetterEventResponseDto.page = pageInfoDto

        val transactionResultDto = TransactionResultDto()
        val paymentInfoDto = PaymentInfoDto()
        paymentInfoDto.origin = "CHECKOUT"
        val paymentDetailInfoDto = PaymentDetailInfoDto()
        paymentDetailInfoDto.rptId = "00000000000000"
        val details = ArrayList<PaymentDetailInfoDto>()
        details.add(paymentDetailInfoDto)
        paymentInfoDto.details = details
        val transactionInfoDto = TransactionInfoDto()
        transactionInfoDto.creationDate = OffsetDateTime.MIN

        transactionResultDto.transactionInfo = transactionInfoDto
        transactionResultDto.paymentInfo = paymentInfoDto

        val searchTransactionResponseDto = SearchTransactionResponseDto()
        searchTransactionResponseDto.transactions.add(transactionResultDto)

        whenever(ecommerceHelpdeskServiceV1.getDeadletterTransactionsByFilter(any(), any(), any()))
            .thenReturn(Mono.just(searchDeadLetterEventResponseDto))

        whenever(ecommerceHelpdeskServiceV1.searchTransactions(any()))
            .thenReturn(Mono.just(searchTransactionResponseDto))

        whenever(ecommerceHelpdeskServiceV1.searchNpgOperations(any()))
            .thenReturn(Mono.just(SearchNpgOperationsResponseDto()))

        /*
        whenever(
                nodoTechnicalSupportClient.searchNodoGivenNoticeNumberAndFiscalCode(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(Mono.just(TransactionResponseDto()))
        */

        StepVerifier.create(
                deadletterTransactionsService.getDeadletterTransactions(date, pageNumber, pageSize)
            )
            .expectNextMatches { response ->
                response.javaClass == ListDeadletterTransactions200ResponseDto::class.java &&
                    response.deadletterTransactions.isNotEmpty() &&
                    response.deadletterTransactions[0].transactionId == "testTransactionId" &&
                    response.page.current == 0 &&
                    response.page.total == 0
            }
            .verifyComplete()

        verify(ecommerceHelpdeskServiceV1)
            .getDeadletterTransactionsByFilter(date, pageSize, pageNumber)
        verify(ecommerceHelpdeskServiceV1).searchTransactions("testTransactionId")
        verify(ecommerceHelpdeskServiceV1).searchNpgOperations("testTransactionId")
        // verify(nodoTechnicalSupportClient)
        //    .searchNodoGivenNoticeNumberAndFiscalCode(any(), any(), any(), any())
    }

    @Test
    fun `getDeadletterTransactions should return a ListDeadletterTransactions200ResponseDto with an empty deadletterTransaction`() {
        val date: LocalDate = LocalDate.parse("2025-08-19")
        val pageNumber = 0
        val pageSize = 1

        whenever(ecommerceHelpdeskServiceV1.getDeadletterTransactionsByFilter(any(), any(), any()))
            .thenReturn(Mono.empty<SearchDeadLetterEventResponseDto>())

        StepVerifier.create(
                deadletterTransactionsService.getDeadletterTransactions(date, pageNumber, pageSize)
            )
            .expectNextMatches { response ->
                response.javaClass == ListDeadletterTransactions200ResponseDto::class.java &&
                    response.deadletterTransactions.isEmpty() &&
                    response.page.current == 0 &&
                    response.page.total == 0 &&
                    response.page.results == 0
            }
            .verifyComplete()
        verify(ecommerceHelpdeskServiceV1)
            .getDeadletterTransactionsByFilter(date, pageSize, pageNumber)
    }

    @Test
    fun `getDeadletterTransactions should return an empty ListDeadletterTransactions200ResponseDto because of the getDeadletterTransactionsByFilter error`() {
        val date: LocalDate = LocalDate.parse("2025-08-19")
        val pageNumber: Int = 0
        val pageSize: Int = 1

        val transactionInfo: DeadLetterTransactionInfoDto = DeadLetterTransactionInfoDto()
        transactionInfo.transactionId = "testTransactionId"

        val deadletterEventDto: DeadLetterEventDto = DeadLetterEventDto()
        deadletterEventDto.data = "2025-08-19"
        deadletterEventDto.transactionInfo = transactionInfo

        val deadletterEvents: ArrayList<DeadLetterEventDto> = ArrayList<DeadLetterEventDto>()
        deadletterEvents.add(deadletterEventDto)

        val searchDeadLetterEventResponseDto: SearchDeadLetterEventResponseDto =
            SearchDeadLetterEventResponseDto()
        searchDeadLetterEventResponseDto.deadLetterEvents = deadletterEvents
        val pageInfoDto: PageInfoDto = PageInfoDto()
        pageInfoDto.current = 0
        pageInfoDto.total = 0
        searchDeadLetterEventResponseDto.page = pageInfoDto

        val transactionResultDto: TransactionResultDto = TransactionResultDto()
        val paymentInfoDto: PaymentInfoDto = PaymentInfoDto()
        paymentInfoDto.origin = "CHECKOUT"
        val paymentDetailInfoDto: PaymentDetailInfoDto = PaymentDetailInfoDto()
        paymentDetailInfoDto.rptId = "00000000000000"
        val details = ArrayList<PaymentDetailInfoDto>()
        details.add(paymentDetailInfoDto)
        paymentInfoDto.details = details
        val transactionInfoDto: TransactionInfoDto = TransactionInfoDto()
        transactionInfoDto.creationDate = OffsetDateTime.MIN

        transactionResultDto.transactionInfo = transactionInfoDto
        transactionResultDto.paymentInfo = paymentInfoDto

        val searchTransactionResponseDto: SearchTransactionResponseDto =
            SearchTransactionResponseDto()
        searchTransactionResponseDto.transactions.add(transactionResultDto)

        val expectedError: Exception = Exception("Error during the fetch")

        whenever(ecommerceHelpdeskServiceV1.getDeadletterTransactionsByFilter(any(), any(), any()))
            .thenReturn(Mono.error(expectedError))

        whenever(ecommerceHelpdeskServiceV1.searchTransactions(any()))
            .thenReturn(Mono.just(searchTransactionResponseDto))

        whenever(ecommerceHelpdeskServiceV1.searchNpgOperations(any()))
            .thenReturn(Mono.just(SearchNpgOperationsResponseDto()))

        whenever(
                nodoTechnicalSupportClient.searchNodoGivenNoticeNumberAndFiscalCode(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(Mono.just(TransactionResponseDto()))

        StepVerifier.create(
                deadletterTransactionsService.getDeadletterTransactions(date, pageNumber, pageSize)
            )
            .expectNextMatches { response ->
                response.javaClass == ListDeadletterTransactions200ResponseDto::class.java &&
                    response.deadletterTransactions.isEmpty() &&
                    // response.deadletterTransactions[0].geteCommerceDetails() == null &&
                    // response.deadletterTransactions[0].nodoDetails == null &&
                    // response.deadletterTransactions[0].geteCommerceStatus() == null &&
                    // response.deadletterTransactions[0].gatewayAuthorizationStatus == null &&
                    // response.deadletterTransactions[0].npgDetails == null &&
                    response.page.current == 0 &&
                    response.page.total == 0
            }
            .verifyComplete()
    }

    @Test
    fun `getDeadletterTransactions should return an empty ListDeadletterTransactions200ResponseDto because of the searchTransactions error`() {
        val date: LocalDate = LocalDate.parse("2025-08-19")
        val pageNumber: Int = 0
        val pageSize: Int = 1

        val transactionInfo: DeadLetterTransactionInfoDto = DeadLetterTransactionInfoDto()
        transactionInfo.transactionId = "testTransactionId"

        val deadletterEventDto: DeadLetterEventDto = DeadLetterEventDto()
        deadletterEventDto.data = "2025-08-19"
        deadletterEventDto.transactionInfo = transactionInfo

        val deadletterEvents: ArrayList<DeadLetterEventDto> = ArrayList<DeadLetterEventDto>()
        deadletterEvents.add(deadletterEventDto)

        val searchDeadLetterEventResponseDto: SearchDeadLetterEventResponseDto =
            SearchDeadLetterEventResponseDto()
        searchDeadLetterEventResponseDto.deadLetterEvents = deadletterEvents
        val pageInfoDto: PageInfoDto = PageInfoDto()
        pageInfoDto.current = 0
        pageInfoDto.total = 0
        searchDeadLetterEventResponseDto.page = pageInfoDto

        val transactionResultDto: TransactionResultDto = TransactionResultDto()
        val paymentInfoDto: PaymentInfoDto = PaymentInfoDto()
        paymentInfoDto.origin = "CHECKOUT"
        val paymentDetailInfoDto: PaymentDetailInfoDto = PaymentDetailInfoDto()
        paymentDetailInfoDto.rptId = "00000000000000"
        val details = ArrayList<PaymentDetailInfoDto>()
        details.add(paymentDetailInfoDto)
        paymentInfoDto.details = details
        val transactionInfoDto: TransactionInfoDto = TransactionInfoDto()
        transactionInfoDto.creationDate = OffsetDateTime.MIN

        transactionResultDto.transactionInfo = transactionInfoDto
        transactionResultDto.paymentInfo = paymentInfoDto

        val searchTransactionResponseDto: SearchTransactionResponseDto =
            SearchTransactionResponseDto()
        searchTransactionResponseDto.transactions.add(transactionResultDto)

        val expectedError: Exception = Exception("Error during the fetch")

        whenever(ecommerceHelpdeskServiceV1.getDeadletterTransactionsByFilter(any(), any(), any()))
            .thenReturn(Mono.just(searchDeadLetterEventResponseDto))

        whenever(ecommerceHelpdeskServiceV1.searchTransactions(any()))
            .thenReturn(Mono.error(expectedError))

        whenever(ecommerceHelpdeskServiceV1.searchNpgOperations(any()))
            .thenReturn(Mono.just(SearchNpgOperationsResponseDto()))

        whenever(
                nodoTechnicalSupportClient.searchNodoGivenNoticeNumberAndFiscalCode(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(Mono.just(TransactionResponseDto()))

        StepVerifier.create(
                deadletterTransactionsService.getDeadletterTransactions(date, pageNumber, pageSize)
            )
            .expectNextMatches { response ->
                response.javaClass == ListDeadletterTransactions200ResponseDto::class.java &&
                    response.deadletterTransactions.isEmpty() &&
                    // response.deadletterTransactions[0].getNodoDetails() == null &&
                    // response.deadletterTransactions[0].geteCommerceDetails() == null &&
                    // response.deadletterTransactions[0].transactionId == "testTransactionId" &&
                    response.page.current == 0 &&
                    response.page.total == 0
            }
            .verifyComplete()
    }

    @Test
    fun `getDeadletterTransactions should return an empy ListDeadletterTransactions200ResponseDto because of the searchNpgOperations error`() {
        val date: LocalDate = LocalDate.parse("2025-08-19")
        val pageNumber: Int = 0
        val pageSize: Int = 1

        val transactionInfo: DeadLetterTransactionInfoDto = DeadLetterTransactionInfoDto()
        transactionInfo.transactionId = "testTransactionId"

        val deadletterEventDto: DeadLetterEventDto = DeadLetterEventDto()
        deadletterEventDto.data = "2025-08-19"
        deadletterEventDto.transactionInfo = transactionInfo

        val deadletterEvents: ArrayList<DeadLetterEventDto> = ArrayList<DeadLetterEventDto>()
        deadletterEvents.add(deadletterEventDto)

        val searchDeadLetterEventResponseDto: SearchDeadLetterEventResponseDto =
            SearchDeadLetterEventResponseDto()
        searchDeadLetterEventResponseDto.deadLetterEvents = deadletterEvents
        val pageInfoDto: PageInfoDto = PageInfoDto()
        pageInfoDto.current = 0
        pageInfoDto.total = 0
        searchDeadLetterEventResponseDto.page = pageInfoDto

        val transactionResultDto: TransactionResultDto = TransactionResultDto()
        val paymentInfoDto: PaymentInfoDto = PaymentInfoDto()
        paymentInfoDto.origin = "CHECKOUT"
        val paymentDetailInfoDto: PaymentDetailInfoDto = PaymentDetailInfoDto()
        paymentDetailInfoDto.rptId = "00000000000000"
        val details = ArrayList<PaymentDetailInfoDto>()
        details.add(paymentDetailInfoDto)
        paymentInfoDto.details = details
        val transactionInfoDto: TransactionInfoDto = TransactionInfoDto()
        transactionInfoDto.creationDate = OffsetDateTime.MIN

        transactionResultDto.transactionInfo = transactionInfoDto
        transactionResultDto.paymentInfo = paymentInfoDto

        val searchTransactionResponseDto: SearchTransactionResponseDto =
            SearchTransactionResponseDto()
        searchTransactionResponseDto.transactions.add(transactionResultDto)

        val expectedError: Exception = Exception("Error during the fetch")

        whenever(ecommerceHelpdeskServiceV1.getDeadletterTransactionsByFilter(any(), any(), any()))
            .thenReturn(Mono.just(searchDeadLetterEventResponseDto))

        whenever(ecommerceHelpdeskServiceV1.searchTransactions(any()))
            .thenReturn(Mono.just(searchTransactionResponseDto))

        whenever(ecommerceHelpdeskServiceV1.searchNpgOperations(any()))
            .thenReturn(Mono.error(expectedError))

        whenever(
                nodoTechnicalSupportClient.searchNodoGivenNoticeNumberAndFiscalCode(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(Mono.just(TransactionResponseDto()))

        StepVerifier.create(
                deadletterTransactionsService.getDeadletterTransactions(date, pageNumber, pageSize)
            )
            .expectNextMatches { response ->
                response.javaClass == ListDeadletterTransactions200ResponseDto::class.java &&
                    response.deadletterTransactions.isEmpty() &&
                    response.page.current == 0 &&
                    response.page.total == 0
            }
            .verifyComplete()

        verify(ecommerceHelpdeskServiceV1)
            .getDeadletterTransactionsByFilter(date, pageSize, pageNumber)
        verify(ecommerceHelpdeskServiceV1).searchTransactions("testTransactionId")
    }

    @Test
    fun `getDeadletterTransactions should return a ListDeadletterTransactions200ResponseDto with deadletterTransactions empty because of rptId null`() {
        val date: LocalDate = LocalDate.parse("2025-08-19")
        val pageNumber: Int = 0
        val pageSize: Int = 1

        val transactionInfo: DeadLetterTransactionInfoDto = DeadLetterTransactionInfoDto()
        transactionInfo.transactionId = "testTransactionId"
        transactionInfo.paymentGateway = "NPG"

        val deadletterEventDto: DeadLetterEventDto = DeadLetterEventDto()
        deadletterEventDto.data = "2025-08-19"
        deadletterEventDto.transactionInfo = transactionInfo

        val deadletterEvents: ArrayList<DeadLetterEventDto> = ArrayList<DeadLetterEventDto>()
        deadletterEvents.add(deadletterEventDto)

        val searchDeadLetterEventResponseDto: SearchDeadLetterEventResponseDto =
            SearchDeadLetterEventResponseDto()
        searchDeadLetterEventResponseDto.deadLetterEvents = deadletterEvents
        val pageInfoDto: PageInfoDto = PageInfoDto()
        pageInfoDto.current = 0
        pageInfoDto.total = 0
        searchDeadLetterEventResponseDto.page = pageInfoDto

        val transactionResultDto: TransactionResultDto = TransactionResultDto()
        val paymentInfoDto: PaymentInfoDto = PaymentInfoDto()
        paymentInfoDto.origin = "CHECKOUT"
        val paymentDetailInfoDto: PaymentDetailInfoDto = PaymentDetailInfoDto()
        paymentDetailInfoDto.rptId = null
        val details = ArrayList<PaymentDetailInfoDto>()
        details.add(paymentDetailInfoDto)
        paymentInfoDto.details = details
        val transactionInfoDto: TransactionInfoDto = TransactionInfoDto()
        transactionInfoDto.creationDate = OffsetDateTime.MIN

        transactionResultDto.transactionInfo = transactionInfoDto
        transactionResultDto.paymentInfo = paymentInfoDto

        val searchTransactionResponseDto: SearchTransactionResponseDto =
            SearchTransactionResponseDto()
        searchTransactionResponseDto.transactions.add(transactionResultDto)

        val expectedError: Exception = Exception("Error during the fetch")

        whenever(ecommerceHelpdeskServiceV1.getDeadletterTransactionsByFilter(any(), any(), any()))
            .thenReturn(Mono.just(searchDeadLetterEventResponseDto))

        whenever(ecommerceHelpdeskServiceV1.searchTransactions(any()))
            .thenReturn(Mono.just(searchTransactionResponseDto))

        whenever(ecommerceHelpdeskServiceV1.searchNpgOperations(any()))
            .thenReturn(Mono.just(SearchNpgOperationsResponseDto()))

        whenever(
                nodoTechnicalSupportClient.searchNodoGivenNoticeNumberAndFiscalCode(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(Mono.just(TransactionResponseDto()))

        StepVerifier.create(
                deadletterTransactionsService.getDeadletterTransactions(date, pageNumber, pageSize)
            )
            .expectNextMatches { response ->
                response.javaClass == ListDeadletterTransactions200ResponseDto::class.java &&
                    response.deadletterTransactions.isNotEmpty() &&
                    // response.deadletterTransactions[0].geteCommerceDetails() == null &&
                    // response.deadletterTransactions[0].nodoDetails == null &&
                    // response.deadletterTransactions[0].geteCommerceStatus() == null &&
                    // response.deadletterTransactions[0].gatewayAuthorizationStatus == null &&
                    // response.deadletterTransactions[0].npgDetails == null &&
                    response.page.current == 0 &&
                    response.page.total == 0
            }
            .verifyComplete()

        verify(ecommerceHelpdeskServiceV1)
            .getDeadletterTransactionsByFilter(date, pageSize, pageNumber)
        verify(ecommerceHelpdeskServiceV1).searchTransactions("testTransactionId")
        verify(ecommerceHelpdeskServiceV1).searchNpgOperations("testTransactionId")
    }

    @Test
    fun `getDeadletterTransactions should return a ListDeadletterTransactions200ResponseDto with deadletterTransactions empty because of the searchNodoGivenNoticeNumberAndFiscalCode error`() {
        val date: LocalDate = LocalDate.parse("2025-08-19")
        val pageNumber: Int = 0
        val pageSize: Int = 1

        val transactionInfo: DeadLetterTransactionInfoDto = DeadLetterTransactionInfoDto()
        transactionInfo.transactionId = "testTransactionId"
        transactionInfo.paymentGateway = "NPG"

        val deadletterEventDto: DeadLetterEventDto = DeadLetterEventDto()
        deadletterEventDto.data = "2025-08-19"
        deadletterEventDto.transactionInfo = transactionInfo

        val deadletterEvents: ArrayList<DeadLetterEventDto> = ArrayList<DeadLetterEventDto>()
        deadletterEvents.add(deadletterEventDto)

        val searchDeadLetterEventResponseDto: SearchDeadLetterEventResponseDto =
            SearchDeadLetterEventResponseDto()
        searchDeadLetterEventResponseDto.deadLetterEvents = deadletterEvents
        val pageInfoDto: PageInfoDto = PageInfoDto()
        pageInfoDto.current = 0
        pageInfoDto.total = 0
        searchDeadLetterEventResponseDto.page = pageInfoDto

        val transactionResultDto: TransactionResultDto = TransactionResultDto()
        val paymentInfoDto: PaymentInfoDto = PaymentInfoDto()
        paymentInfoDto.origin = "CHECKOUT"
        val paymentDetailInfoDto: PaymentDetailInfoDto = PaymentDetailInfoDto()
        paymentDetailInfoDto.rptId = "00000000000000"
        val details = ArrayList<PaymentDetailInfoDto>()
        details.add(paymentDetailInfoDto)
        paymentInfoDto.details = details
        val transactionInfoDto: TransactionInfoDto = TransactionInfoDto()
        transactionInfoDto.creationDate = OffsetDateTime.MIN

        transactionResultDto.transactionInfo = transactionInfoDto
        transactionResultDto.paymentInfo = paymentInfoDto

        val searchTransactionResponseDto: SearchTransactionResponseDto =
            SearchTransactionResponseDto()
        searchTransactionResponseDto.transactions.add(transactionResultDto)

        val expectedError: Exception = Exception("Error during the fetch")

        whenever(ecommerceHelpdeskServiceV1.getDeadletterTransactionsByFilter(any(), any(), any()))
            .thenReturn(Mono.just(searchDeadLetterEventResponseDto))

        whenever(ecommerceHelpdeskServiceV1.searchTransactions(any()))
            .thenReturn(Mono.just(searchTransactionResponseDto))

        whenever(ecommerceHelpdeskServiceV1.searchNpgOperations(any()))
            .thenReturn(Mono.just(SearchNpgOperationsResponseDto()))

        whenever(
                nodoTechnicalSupportClient.searchNodoGivenNoticeNumberAndFiscalCode(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(Mono.error(expectedError))

        StepVerifier.create(
                deadletterTransactionsService.getDeadletterTransactions(date, pageNumber, pageSize)
            )
            .expectNextMatches { response ->
                response.javaClass == ListDeadletterTransactions200ResponseDto::class.java &&
                    response.deadletterTransactions.isNotEmpty() &&
                    response.deadletterTransactions[0].nodoDetails == null &&
                    // response.deadletterTransactions[0].geteCommerceDetails() != null &&
                    // response.deadletterTransactions[0].npgDetails != null &&
                    response.page.current == 0 &&
                    response.page.total == 0
            }
            .verifyComplete()

        verify(ecommerceHelpdeskServiceV1)
            .getDeadletterTransactionsByFilter(date, pageSize, pageNumber)
        verify(ecommerceHelpdeskServiceV1).searchTransactions("testTransactionId")
        verify(ecommerceHelpdeskServiceV1).searchNpgOperations("testTransactionId")
    }

    @Test
    fun `getDeadletterTransactions should return a ListDeadletterTransactions200ResponseDto with deadletterTransactions empty because transactionId in deadLetterEvent is null`() {
        val date: LocalDate = LocalDate.parse("2025-08-19")
        val pageNumber: Int = 0
        val pageSize: Int = 1

        val transactionInfo: DeadLetterTransactionInfoDto = DeadLetterTransactionInfoDto()
        transactionInfo.transactionId = "testTransactionId"

        val deadletterEventDto: DeadLetterEventDto = DeadLetterEventDto()
        deadletterEventDto.data = "2025-08-19"
        deadletterEventDto.transactionInfo = null

        val deadletterEvents: ArrayList<DeadLetterEventDto> = ArrayList<DeadLetterEventDto>()
        deadletterEvents.add(deadletterEventDto)

        val searchDeadLetterEventResponseDto: SearchDeadLetterEventResponseDto =
            SearchDeadLetterEventResponseDto()
        searchDeadLetterEventResponseDto.deadLetterEvents = deadletterEvents
        val pageInfoDto: PageInfoDto = PageInfoDto()
        pageInfoDto.current = 0
        pageInfoDto.total = 0
        searchDeadLetterEventResponseDto.page = pageInfoDto

        val transactionResultDto: TransactionResultDto = TransactionResultDto()
        val paymentInfoDto: PaymentInfoDto = PaymentInfoDto()
        paymentInfoDto.origin = "CHECKOUT"
        val paymentDetailInfoDto: PaymentDetailInfoDto = PaymentDetailInfoDto()
        paymentDetailInfoDto.rptId = "00000000000000"
        val details = ArrayList<PaymentDetailInfoDto>()
        details.add(paymentDetailInfoDto)
        paymentInfoDto.details = details
        val transactionInfoDto: TransactionInfoDto = TransactionInfoDto()
        transactionInfoDto.creationDate = OffsetDateTime.MIN

        transactionResultDto.transactionInfo = transactionInfoDto
        transactionResultDto.paymentInfo = paymentInfoDto

        val searchTransactionResponseDto: SearchTransactionResponseDto =
            SearchTransactionResponseDto()
        searchTransactionResponseDto.transactions.add(transactionResultDto)

        whenever(ecommerceHelpdeskServiceV1.getDeadletterTransactionsByFilter(any(), any(), any()))
            .thenReturn(Mono.just(searchDeadLetterEventResponseDto))

        StepVerifier.create(
                deadletterTransactionsService.getDeadletterTransactions(date, pageNumber, pageSize)
            )
            .expectNextMatches { response ->
                response.javaClass == ListDeadletterTransactions200ResponseDto::class.java &&
                    response.deadletterTransactions.isNotEmpty() &&
                    response.deadletterTransactions[0].transactionId == "N/A" &&
                    response.page.current == 0 &&
                    response.page.total == 0
            }
            .verifyComplete()

        verify(ecommerceHelpdeskServiceV1)
            .getDeadletterTransactionsByFilter(date, pageSize, pageNumber)
    }

    @Test
    fun `buildDeadletterTransactionDto obfuscate email when userInfo are available`() {
        val date: LocalDate = LocalDate.parse("2025-08-19")
        val pageNumber: Int = 0
        val pageSize: Int = 1

        val transactionInfo: DeadLetterTransactionInfoDto = DeadLetterTransactionInfoDto()
        transactionInfo.transactionId = "testTransactionId"
        transactionInfo.paymentGateway = "NPG"
        val deadletterEventDto: DeadLetterEventDto = DeadLetterEventDto()
        deadletterEventDto.data = "2025-08-19"
        deadletterEventDto.transactionInfo = transactionInfo

        val deadletterEvents: ArrayList<DeadLetterEventDto> = ArrayList<DeadLetterEventDto>()
        deadletterEvents.add(deadletterEventDto)

        val searchDeadLetterEventResponseDto: SearchDeadLetterEventResponseDto =
            SearchDeadLetterEventResponseDto()
        searchDeadLetterEventResponseDto.deadLetterEvents = deadletterEvents
        val pageInfoDto: PageInfoDto = PageInfoDto()
        pageInfoDto.current = 0
        pageInfoDto.total = 0
        searchDeadLetterEventResponseDto.page = pageInfoDto

        val transactionResultDto: TransactionResultDto = TransactionResultDto()
        val paymentInfoDto: PaymentInfoDto = PaymentInfoDto()
        paymentInfoDto.origin = "CHECKOUT"
        val paymentDetailInfoDto: PaymentDetailInfoDto = PaymentDetailInfoDto()
        paymentDetailInfoDto.rptId = "00000000000000"
        val details = ArrayList<PaymentDetailInfoDto>()
        details.add(paymentDetailInfoDto)
        paymentInfoDto.details = details
        val transactionInfoDto: TransactionInfoDto = TransactionInfoDto()
        transactionInfoDto.creationDate = OffsetDateTime.MIN

        transactionResultDto.transactionInfo = transactionInfoDto
        transactionResultDto.paymentInfo = paymentInfoDto

        val userInfo = UserInfoDto()
        userInfo.notificationEmail = "testmail@gmail.com"
        transactionResultDto.userInfo = userInfo

        val searchTransactionResponseDto: SearchTransactionResponseDto =
            SearchTransactionResponseDto()
        searchTransactionResponseDto.transactions.add(transactionResultDto)

        whenever(ecommerceHelpdeskServiceV1.getDeadletterTransactionsByFilter(any(), any(), any()))
            .thenReturn(Mono.just(searchDeadLetterEventResponseDto))

        whenever(ecommerceHelpdeskServiceV1.searchTransactions(any()))
            .thenReturn(Mono.just(searchTransactionResponseDto))

        whenever(ecommerceHelpdeskServiceV1.searchNpgOperations(any()))
            .thenReturn(Mono.just(SearchNpgOperationsResponseDto()))

        whenever(
                nodoTechnicalSupportClient.searchNodoGivenNoticeNumberAndFiscalCode(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(Mono.just(TransactionResponseDto()))

        StepVerifier.create(
                deadletterTransactionsService.getDeadletterTransactions(date, pageNumber, pageSize)
            )
            .expectNextMatches { response ->
                response.javaClass == ListDeadletterTransactions200ResponseDto::class.java &&
                    response.deadletterTransactions.isNotEmpty() &&
                    response.deadletterTransactions[0].transactionId == "testTransactionId" &&
                    response.page.current == 0 &&
                    response.page.total == 0
            }
            .verifyComplete()

        verify(ecommerceHelpdeskServiceV1)
            .getDeadletterTransactionsByFilter(date, pageSize, pageNumber)
        verify(ecommerceHelpdeskServiceV1).searchTransactions("testTransactionId")
        verify(ecommerceHelpdeskServiceV1).searchNpgOperations("testTransactionId")
        // verify(nodoTechnicalSupportClient)
        //     .searchNodoGivenNoticeNumberAndFiscalCode(any(), any(), any(), any())
    }

    @Test
    fun `addActionToDeadletterTransaction should save a deadletterTransactionAction`() {
        val transactionId = "testId"
        val userId = "userIdTest"
        val actionValueType = ActionTypeDto("test", ActionTypeDto.TypeEnum.NOT_FINAL)
        val actionValue = "test"
        val actionTypes = listOf<ActionTypeDto>(actionValueType)
        actionConfig.types = actionTypes

        val action =
            Action(
                UUID.randomUUID().toString(),
                transactionId,
                userId,
                actionValueType,
                Instant.now(),
            )
        whenever(ecommerceHelpdeskServiceV1.searchTransactions(any()))
            .thenReturn(Mono.just(SearchTransactionResponseDto()))
        whenever(deadletterTransactionActionRepository.save(any())).thenReturn(Mono.just(action))

        val resultMono =
            deadletterTransactionsService.addActionToDeadletterTransaction(
                transactionId,
                userId,
                actionValue,
            )

        StepVerifier.create(resultMono).expectNext(action).expectComplete().verify()

        // Verify the object pass to the repository and his parameters
        val actionCaptor = argumentCaptor<Action>()
        verify(ecommerceHelpdeskServiceV1).searchTransactions(transactionId)
        verify(deadletterTransactionActionRepository).save(actionCaptor.capture())

        val newDeadLetterActionCapture = actionCaptor.firstValue

        assertNotNull(newDeadLetterActionCapture.id)
        assertNotNull(newDeadLetterActionCapture.timestamp)
        assertEquals(newDeadLetterActionCapture.transactionId, transactionId)
        assertEquals(newDeadLetterActionCapture.userId, userId)
        assertEquals(newDeadLetterActionCapture.action.value, actionValue)
    }

    @Test
    fun `addActionToDeadletterTransaction should return an InvalidActionValue`() {
        val transactionId = "testId"
        val userId = "userIdTest"
        val actionValueType = ActionTypeDto("test", ActionTypeDto.TypeEnum.NOT_FINAL)
        val actionValue = "wrong-value"
        val actionTypes = listOf<ActionTypeDto>(actionValueType)
        actionConfig.types = actionTypes

        val resultMono =
            deadletterTransactionsService.addActionToDeadletterTransaction(
                transactionId,
                userId,
                actionValue,
            )

        StepVerifier.create(resultMono).expectError(InvalidActionValue::class.java).verify()
    }

    @Test
    fun `addActionToDeadletterTransaction should return an InvalidTransactionId`() {
        val transactionId = "testId"
        val userId = "userIdTest"
        val actionValueType = ActionTypeDto("test", ActionTypeDto.TypeEnum.NOT_FINAL)
        val actionValue = "test"
        val actionTypes = listOf<ActionTypeDto>(actionValueType)
        actionConfig.types = actionTypes

        whenever(ecommerceHelpdeskServiceV1.searchTransactions(any())).thenReturn(Mono.empty())

        val resultMono =
            deadletterTransactionsService.addActionToDeadletterTransaction(
                transactionId,
                userId,
                actionValue,
            )
        verify(ecommerceHelpdeskServiceV1).searchTransactions(transactionId)
        StepVerifier.create(resultMono).expectError(InvalidTransactionId::class.java).verify()
    }

    @Test
    fun `listActionsForDeadletterTransaction should return all the action associated with a certain transactionId`() {
        val transactionId = "testId"
        val userId = "userIdTest"
        val actionValueType = ActionTypeDto("test", ActionTypeDto.TypeEnum.NOT_FINAL)

        val action: Action =
            Action(
                UUID.randomUUID().toString(),
                transactionId,
                userId,
                actionValueType,
                Instant.now(),
            )

        whenever(deadletterTransactionActionRepository.findByTransactionId(any()))
            .thenReturn(Flux.just(action))

        val resultFlux =
            deadletterTransactionsService.listActionsForDeadletterTransaction(transactionId, userId)

        StepVerifier.create(resultFlux).expectNext(action).expectComplete().verify()

        // Verify the object pass to the repository and his parameters
        val actionCaptor = argumentCaptor<String>()
        verify(deadletterTransactionActionRepository).findByTransactionId(actionCaptor.capture())

        val transactionIdValuePassed = actionCaptor.firstValue
        assertEquals(transactionIdValuePassed, transactionId)
    }

    @Test
    fun `getDeadletterTransactionsByDateRange should return a ListDeadletterTransactions200ResponseDto V2 with elements`() {

        val fromDate = LocalDate.parse("2025-01-01")
        val toDate = LocalDate.parse("2025-01-02")
        val pageNumber = 0
        val pageSize = 10

        val deadLetterEventV2 =
            DeadLetterEventDto().apply {
                transactionInfo =
                    DeadLetterTransactionInfoDto().apply {
                        transactionId = "transactionIdV2"
                        paymentGateway = "NPG"
                    }
            }

        val searchResponseV2 =
            SearchDeadLetterEventResponseDto().apply {
                deadLetterEvents = listOf(deadLetterEventV2)
                page =
                    PageInfoDto().apply {
                        current = 0
                        total = 1
                        results = 1
                    }
            }

        val transactionResultV2 =
            TransactionResultDto().apply {
                transactionInfo =
                    TransactionInfoDto().apply { eventStatus = TransactionStatusDto.CLOSED }
            }

        val searchTransactionResponseV2 =
            SearchTransactionResponseDto().apply {
                transactions = mutableListOf(transactionResultV2)
            }

        whenever(
                ecommerceHelpdeskServiceV1.getDeadletterTransactionsByDateRange(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(Mono.just(searchResponseV2))

        whenever(ecommerceHelpdeskServiceV1.searchTransactions("transactionIdV2"))
            .thenReturn(Mono.just(searchTransactionResponseV2))

        whenever(ecommerceHelpdeskServiceV1.searchNpgOperations("transactionIdV2"))
            .thenReturn(
                Mono.just(
                    it.pagopa.generated.ecommerce.helpdesk.model.SearchNpgOperationsResponseDto()
                )
            )

        val resultMono =
            deadletterTransactionsService.getDeadletterTransactionsByDateRange(
                fromDate,
                toDate,
                pageNumber,
                pageSize,
            )

        StepVerifier.create(resultMono)
            .expectNextMatches { response ->
                response is
                    it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.ListDeadletterTransactions200ResponseDto &&
                    response.deadletterTransactions.size == 1 &&
                    response.deadletterTransactions[0].transactionId == "transactionIdV2" &&
                    response.page.total == 1
            }
            .verifyComplete()

        verify(ecommerceHelpdeskServiceV1)
            .getDeadletterTransactionsByDateRange(fromDate, toDate, pageSize, pageNumber)
        verify(ecommerceHelpdeskServiceV1).searchTransactions("transactionIdV2")
        verify(ecommerceHelpdeskServiceV1).searchNpgOperations("transactionIdV2")
    }

    @Test
    fun `getDeadletterTransactionsByDateRange should return empty response on client error`() {
        val fromDate = LocalDate.now()
        val toDate = LocalDate.now()

        whenever(
                ecommerceHelpdeskServiceV1.getDeadletterTransactionsByDateRange(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(Mono.error(RuntimeException("Service Down")))

        val resultMono =
            deadletterTransactionsService.getDeadletterTransactionsByDateRange(
                fromDate,
                toDate,
                0,
                10,
            )

        StepVerifier.create(resultMono)
            .expectNextMatches { response ->
                response.deadletterTransactions.isEmpty() && response.page.total == 0
            }
            .verifyComplete()
    }

    @Test
    fun `addNoteToDeadLetterTransaction should return the Note saved without error`() {

        val note =
            Note("testId", "test", "transactionId", "usertestId", Instant.now(), Instant.now())

        whenever(deadletterTransactionsNoteRepo.countByTransactionId(any<String>()))
            .thenReturn(Mono.just(0L))
        whenever(deadletterTransactionsNoteRepo.save(any())).thenReturn(Mono.just(note))

        val resultMono =
            deadletterTransactionsService.addNoteToDeadLetterTransaction(
                "test",
                "usertestId",
                "transactionId",
            )

        StepVerifier.create(resultMono)
            .expectNextMatches { response ->
                response is it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.NoteDto &&
                    response.noteId == "testId" &&
                    response.transactionId == "transactionId" &&
                    response.note == "test"
            }
            .verifyComplete()
    }

    @Test
    fun `addNoteToDeadLetterTransaction should return InvalidTransactionId because the transactionId doen't exist`() {

        whenever(deadletterTransactionsNoteRepo.countByTransactionId(any<String>()))
            .thenReturn(Mono.just(0L))
        whenever(deadletterTransactionsNoteRepo.save(any())).thenReturn(Mono.empty())

        val resultMono =
            deadletterTransactionsService.addNoteToDeadLetterTransaction(
                "test",
                "usertestId",
                "transactionId",
            )

        StepVerifier.create(resultMono).expectError(InvalidTransactionId::class.java).verify()
    }

    @Test
    fun `addNoteToDeadLetterTransaction should return NotesLimitException because there are too many notes already`() {

        whenever(deadletterTransactionsNoteRepo.countByTransactionId(any<String>()))
            .thenReturn(Mono.just(noteNumLimit + 1))

        val resultMono =
            deadletterTransactionsService.addNoteToDeadLetterTransaction(
                "test",
                "usertestId",
                "transactionId",
            )

        StepVerifier.create(resultMono).expectError(NotesLimitException::class.java).verify()
    }

    @Test
    fun `getAllNotesByTransactionIdList should return the TransactionNotesDto without error`() {
        val now = Instant.now()
        val note = Note("testId", "test", "transactionId", "usertestId", now, now)
        val note2 =
            Note(
                "testId",
                "test",
                "transactionId",
                "usertestId",
                now.plusSeconds(30),
                now.plusSeconds(30),
            )
        val notes: ArrayList<Note> = ArrayList()
        notes.add(note2)
        notes.add(note)

        val transactionIdList = notes.map { it.transactionId }

        whenever(deadletterTransactionsNoteRepo.findAllByTransactionIdIn(any()))
            .thenReturn(Flux.fromIterable(notes))

        val resultMono =
            deadletterTransactionsService.getAllNotesByTransactionIdList(transactionIdList)

        StepVerifier.create(resultMono)
            .expectNextMatches { response ->
                response is
                    it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.TransactionNotesDto &&
                    response.notesList.size == 2 &&
                    response.transactionId == "transactionId" &&
                    // Check the cronological order of the result
                    response.notesList[0].createdAt < response.notesList[1].createdAt
            }
            .verifyComplete()
    }

    @Test
    fun `getAllNotesByTransactionIdList should return an empty result if the findAllByTransactionIdIn don't find any notes`() {
        val now = Instant.now()
        val note = Note("testId", "test", "transactionId", "usertestId", now, now)
        val note2 =
            Note(
                "testId",
                "test",
                "transactionId",
                "usertestId",
                now.plusSeconds(30),
                now.plusSeconds(30),
            )
        val notes: ArrayList<Note> = ArrayList()
        notes.add(note2)
        notes.add(note)

        val transactionIdList = notes.map { it.transactionId }

        whenever(deadletterTransactionsNoteRepo.findAllByTransactionIdIn(any()))
            .thenReturn(Flux.empty())

        val resultFlux =
            deadletterTransactionsService.getAllNotesByTransactionIdList(transactionIdList)

        StepVerifier.create(resultFlux).expectComplete()
    }

    @Test
    fun `updateNote should return the count of the modified notes`() {
        whenever(deadletterTransactionsNoteRepo.updateNoteById(any(), any(), any()))
            .thenReturn(Mono.just<Long>(1L))

        val resultMono = deadletterTransactionsService.updateNote("idTest", "test")

        StepVerifier.create(resultMono)
            .expectNextMatches { response -> response > 0 }
            .verifyComplete()
    }

    @Test
    fun `updateNote should return error because update fail`() {
        whenever(deadletterTransactionsNoteRepo.updateNoteById(any(), any(), any()))
            .thenReturn(Mono.just<Long>(0L))

        val resultMono = deadletterTransactionsService.updateNote("idTest", "test")

        StepVerifier.create(resultMono).expectError(InvalidNoteId::class.java)
    }

    @Test
    fun `deleteNote should return without error because the delete on db worked`() {
        whenever(deadletterTransactionsNoteRepo.deleteBy_id(any())).thenReturn(Mono.just<Long>(1L))

        val resultMono = deadletterTransactionsService.deleteNote("idTest")

        StepVerifier.create(resultMono)
            .expectNextMatches { result -> result is Unit }
            .verifyComplete()
    }

    @Test
    fun `deleteNote should return error because the delete fail`() {
        whenever(deadletterTransactionsNoteRepo.deleteBy_id(any())).thenReturn(Mono.just<Long>(0L))

        val resultMono = deadletterTransactionsService.deleteNote("idTest")

        StepVerifier.create(resultMono).expectError(InvalidNoteId::class.java)
    }
}
