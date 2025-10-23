package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.clients.EcommerceHelpdeskServiceClient
import it.pagopa.ecommerce.watchdog.deadletter.clients.NodoTechnicalSupportClient
import it.pagopa.ecommerce.watchdog.deadletter.documents.DeadletterTransactionAction
import it.pagopa.ecommerce.watchdog.deadletter.repositories.DeadletterTransactionActionRepository
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
import it.pagopa.generated.ecommerce.helpdesk.model.UserInfoDto
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
    private val ecommerceHelpdeskServiceV1: EcommerceHelpdeskServiceClient = mock()
    private val nodoTechnicalSupportClient: NodoTechnicalSupportClient = mock()
    private val deadletterTransactionActionRepository: DeadletterTransactionActionRepository =
        mock()
    private val deadletterTransactionsService: DeadletterTransactionsService =
        DeadletterTransactionsService(
            ecommerceHelpdeskServiceV1,
            nodoTechnicalSupportClient,
            deadletterTransactionActionRepository,
        )

    @Test
    fun `getDeadletterTransactions should return a ListDeadletterTransactions200ResponseDto with an element`() {
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
        verify(nodoTechnicalSupportClient)
            .searchNodoGivenNoticeNumberAndFiscalCode(any(), any(), any(), any())
    }

    @Test
    fun `getDeadletterTransactions should return a ListDeadletterTransactions200ResponseDto with an empty deadletterTransaction`() {
        val date: LocalDate = LocalDate.parse("2025-08-19")
        val pageNumber: Int = 0
        val pageSize: Int = 1

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
    fun `getDeadletterTransactions should return an empy ListDeadletterTransactions200ResponseDto because of the searchTransactions error`() {
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
        verify(ecommerceHelpdeskServiceV1).searchNpgOperations("testTransactionId")
    }

    @Test
    fun `getDeadletterTransactions should return a ListDeadletterTransactions200ResponseDto with deadletterTransactions empty because of rptId null`() {
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
        verify(nodoTechnicalSupportClient)
            .searchNodoGivenNoticeNumberAndFiscalCode(any(), any(), any(), any())
    }

    @Test
    fun `addActionToDeadletterTransaction should save a deadletterTransactionAction`() {
        val transactionId = "testId"
        val userId = "userIdTest"
        val actionValue = "valueTest"

        val deadletterTransactionAction: DeadletterTransactionAction =
            DeadletterTransactionAction(
                UUID.randomUUID().toString(),
                transactionId,
                userId,
                actionValue,
                Instant.now(),
            )

        whenever(deadletterTransactionActionRepository.save(any()))
            .thenReturn(Mono.just(deadletterTransactionAction))

        val resultMono =
            deadletterTransactionsService.addActionToDeadletterTransaction(
                transactionId,
                userId,
                actionValue,
            )

        StepVerifier.create(resultMono)
            .expectNext(deadletterTransactionAction)
            .expectComplete()
            .verify()

        // Verify the object pass to the repository and his parameters
        val actionCaptor = argumentCaptor<DeadletterTransactionAction>()
        verify(deadletterTransactionActionRepository).save(actionCaptor.capture())

        val newDeadLetterActionCapture = actionCaptor.firstValue

        assertNotNull(newDeadLetterActionCapture.id)
        assertNotNull(newDeadLetterActionCapture.timestamp)
        assertEquals(newDeadLetterActionCapture.transactionId, transactionId)
        assertEquals(newDeadLetterActionCapture.userId, userId)
        assertEquals(newDeadLetterActionCapture.value, actionValue)
    }

    @Test
    fun `listActionsForDeadletterTransaction should return all the action associated with a certain transactionId`() {
        val transactionId = "testId"
        val userId = "userIdTest"
        val actionValue = "valueTest"

        val deadletterTransactionAction: DeadletterTransactionAction =
            DeadletterTransactionAction(
                UUID.randomUUID().toString(),
                transactionId,
                userId,
                actionValue,
                Instant.now(),
            )

        whenever(deadletterTransactionActionRepository.findByTransactionId(any()))
            .thenReturn(Flux.just(deadletterTransactionAction))

        val resultFlux =
            deadletterTransactionsService.listActionsForDeadletterTransaction(transactionId, userId)

        StepVerifier.create(resultFlux)
            .expectNext(deadletterTransactionAction)
            .expectComplete()
            .verify()

        // Verify the object pass to the repository and his parameters
        val actionCaptor = argumentCaptor<String>()
        verify(deadletterTransactionActionRepository).findByTransactionId(actionCaptor.capture())

        val transactionIdValuePassed = actionCaptor.firstValue
        assertEquals(transactionIdValuePassed, transactionId)
    }
}
