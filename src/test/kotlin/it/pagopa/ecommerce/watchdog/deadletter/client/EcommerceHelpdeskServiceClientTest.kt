package it.pagopa.ecommerce.watchdog.deadletter.client

import it.pagopa.ecommerce.watchdog.deadletter.clients.EcommerceHelpdeskServiceClient
import it.pagopa.generated.ecommerce.helpdesk.api.ECommerceApi
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
class EcommerceHelpdeskServiceClientTest {

    @Mock private lateinit var eCommerceHelpdeskApi: ECommerceApi

    @InjectMocks private lateinit var client: EcommerceHelpdeskServiceClient

    @Test
    fun `getDeadletterTransactionsByFilter should call API with correct parameters`() {
        // Given
        val date = LocalDate.of(2024, 1, 15)
        val pageSize = 10
        val pageNumber = 0

        val expectedResponse = SearchDeadLetterEventResponseDto()

        whenever(eCommerceHelpdeskApi.ecommerceSearchDeadLetterEvents(any(), any(), any()))
            .thenReturn(Mono.just(expectedResponse))

        // When
        val result = client.getDeadletterTransactionsByFilter(date, pageSize, pageNumber)

        // Then
        StepVerifier.create(result).expectNext(expectedResponse).verifyComplete()

        verify(eCommerceHelpdeskApi)
            .ecommerceSearchDeadLetterEvents(eq(pageNumber), eq(pageSize), any())
    }

    @Test
    fun `getDeadletterTransactionsByDateRange should call API with correct parameters`() {
        // Given
        val dateFrom = LocalDate.of(2024, 1, 1)
        val dateTo = LocalDate.of(2024, 1, 31)
        val pageSize = 20
        val pageNumber = 1

        val expectedResponse = SearchDeadLetterEventResponseDto()

        whenever(eCommerceHelpdeskApi.ecommerceSearchDeadLetterEvents(any(), any(), any()))
            .thenReturn(Mono.just(expectedResponse))

        // When
        val result =
            client.getDeadletterTransactionsByDateRange(dateFrom, dateTo, pageSize, pageNumber)

        // Then
        StepVerifier.create(result).expectNext(expectedResponse).verifyComplete()

        verify(eCommerceHelpdeskApi)
            .ecommerceSearchDeadLetterEvents(eq(pageNumber), eq(pageSize), any())
    }

    @Test
    fun `getDeadletterTransactionsByDateRange should exclude REDIRECT payment gateway`() {
        // Given
        val dateFrom = LocalDate.of(2024, 1, 1)
        val dateTo = LocalDate.of(2024, 1, 31)
        val pageSize = 10
        val pageNumber = 0

        val expectedResponse = SearchDeadLetterEventResponseDto()

        var capturedRequest: EcommerceSearchDeadLetterEventsRequestDto? = null
        whenever(eCommerceHelpdeskApi.ecommerceSearchDeadLetterEvents(any(), any(), any()))
            .thenAnswer { invocation ->
                capturedRequest = invocation.getArgument(2)
                Mono.just(expectedResponse)
            }

        // When
        client.getDeadletterTransactionsByDateRange(dateFrom, dateTo, pageSize, pageNumber).block()

        // Then
        assert(capturedRequest?.excludedPaymentGateway?.contains("REDIRECT") == true)
    }

    @Test
    fun `searchTransactions should call API with transaction ID`() {
        // Given
        val transactionId = "TRX123456"
        val expectedResponse = SearchTransactionResponseDto()

        whenever(eCommerceHelpdeskApi.ecommerceSearchTransaction(any(), any(), any()))
            .thenReturn(Mono.just(expectedResponse))

        // When
        val result = client.searchTransactions(transactionId)

        // Then
        StepVerifier.create(result).expectNext(expectedResponse).verifyComplete()

        verify(eCommerceHelpdeskApi).ecommerceSearchTransaction(eq(0), eq(10), any())
    }

    @Test
    fun `searchTransactions should handle errors`() {
        // Given
        val transactionId = "TRX123456"
        val expectedException = RuntimeException("API Error")

        whenever(eCommerceHelpdeskApi.ecommerceSearchTransaction(any(), any(), any()))
            .thenReturn(Mono.error(expectedException))

        // When
        val result = client.searchTransactions(transactionId)

        // Then
        StepVerifier.create(result).expectError(RuntimeException::class.java).verify()
    }

    @Test
    fun `searchNpgOperations should call API with transaction ID`() {
        // Given
        val transactionId = "TRX123456"
        val expectedResponse = SearchNpgOperationsResponseDto()

        whenever(eCommerceHelpdeskApi.ecommerceSearchNpgOperations(any()))
            .thenReturn(Mono.just(expectedResponse))

        // When
        val result = client.searchNpgOperations(transactionId)

        // Then
        StepVerifier.create(result).expectNext(expectedResponse).verifyComplete()

        verify(eCommerceHelpdeskApi).ecommerceSearchNpgOperations(any())
    }

    @Test
    fun `searchNpgOperations should handle errors`() {
        // Given
        val transactionId = "TRX123456"
        val expectedException = RuntimeException("API Error")

        whenever(eCommerceHelpdeskApi.ecommerceSearchNpgOperations(any()))
            .thenReturn(Mono.error(expectedException))

        // When
        val result = client.searchNpgOperations(transactionId)

        // Then
        StepVerifier.create(result).expectError(RuntimeException::class.java).verify()
    }

    @Test
    fun `getDeadletterTransactionsByFilter should set correct date range`() {
        // Given
        val date = LocalDate.of(2024, 1, 15)
        val expectedStartDate = date.atStartOfDay().atOffset(ZoneOffset.UTC)
        val expectedEndDate = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)

        var capturedRequest: EcommerceSearchDeadLetterEventsRequestDto? = null
        whenever(eCommerceHelpdeskApi.ecommerceSearchDeadLetterEvents(any(), any(), any()))
            .thenAnswer { invocation ->
                capturedRequest = invocation.getArgument(2)
                Mono.just(SearchDeadLetterEventResponseDto())
            }

        // When
        client.getDeadletterTransactionsByFilter(date, 10, 0).block()

        // Then
        assert(capturedRequest?.timeRange?.startDate == expectedStartDate)
        assert(capturedRequest?.timeRange?.endDate == expectedEndDate)
    }
}
