package it.pagopa.ecommerce.watchdog.deadletter.clients

import it.pagopa.generated.ecommerce.helpdesk.api.ECommerceApi
import it.pagopa.generated.ecommerce.helpdesk.model.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class EcommerceHelpdeskServiceClient(private val eCommerceHelpdeskApi: ECommerceApi) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Deprecated("Switch to getDeadletterTransactionsByDateRange")
    fun getDeadletterTransactionsByFilter(
        date: LocalDate,
        pageSize: Int,
        pageNumber: Int,
    ): Mono<SearchDeadLetterEventResponseDto> {

        val startDate: OffsetDateTime = date.atStartOfDay().atOffset(ZoneOffset.UTC)
        val endDate: OffsetDateTime = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)

        val timeRange = DeadLetterSearchDateTimeRangeDto().startDate(startDate).endDate(endDate)

        val excludedStatuses =
            DeadLetterExcludedStatusesDto()
                .ecommerceStatuses(
                    listOf(
                        "CANCELED",
                        "NOTIFIED_OK",
                        "NOTIFICATION_REQUESTED",
                        "EXPIRED_NOT_AUTHORIZED",
                    )
                )
                .npgStatuses(listOf("CANCELED"))

        val requestDto =
            EcommerceSearchDeadLetterEventsRequestDto()
                .source(DeadLetterSearchEventSourceDto.ECOMMERCE)
                .timeRange(timeRange)
                .excludedStatuses(excludedStatuses)

        return eCommerceHelpdeskApi.ecommerceSearchDeadLetterEvents(
            pageNumber,
            pageSize,
            requestDto,
        )
    }

    fun getDeadletterTransactionsByDateRange(
        dateFrom: LocalDate,
        dateTo: LocalDate,
        pageSize: Int,
        pageNumber: Int,
    ): Mono<SearchDeadLetterEventResponseDto> {

        val startDate: OffsetDateTime = dateFrom.atStartOfDay().atOffset(ZoneOffset.UTC)
        val endDate: OffsetDateTime = dateTo.atStartOfDay().atOffset(ZoneOffset.UTC)

        val timeRange = DeadLetterSearchDateTimeRangeDto().startDate(startDate).endDate(endDate)

        val excludedStatuses =
            DeadLetterExcludedStatusesDto()
                .ecommerceStatuses(
                    listOf(
                        "CANCELED",
                        "NOTIFIED_OK",
                        "NOTIFICATION_REQUESTED",
                        "EXPIRED_NOT_AUTHORIZED",
                    )
                )
                .npgStatuses(listOf("CANCELED"))

        val excludedPaymentGateway = listOf("REDIRECT")

        val requestDto =
            EcommerceSearchDeadLetterEventsRequestDto()
                .source(DeadLetterSearchEventSourceDto.ECOMMERCE)
                .timeRange(timeRange)
                .excludedStatuses(excludedStatuses)
                .excludedPaymentGateway(excludedPaymentGateway)

        return eCommerceHelpdeskApi.ecommerceSearchDeadLetterEvents(
            pageNumber,
            pageSize,
            requestDto,
        )
    }

    /**
     * Calls the searchTransaction API with a TransactionId as search filter.
     *
     * @param transactionId the transaction identifier to search for
     * @return A Mono emitting the SearchTransactionResponseDto or an error
     */
    fun searchTransactions(transactionId: String): Mono<SearchTransactionResponseDto> {
        val pageNumber = 0
        val pageSize = 10

        val request =
            SearchTransactionRequestTransactionIdDto()
                .type("TRANSACTION_ID")
                .transactionId(transactionId)

        return eCommerceHelpdeskApi
            .ecommerceSearchTransaction(pageNumber, pageSize, request)
            .doOnError {
                logger.error(
                    "Error calling searchTransaction API for transactionId: $transactionId",
                    it,
                )
            }
    }

    /**
     * Calls the searchNpgOperations API with a TransactionId as search filter.
     *
     * @param transactionId the transaction identifier to search for
     * @return A Mono emitting the SearchNpgOperationsResponseDto or an error
     */
    fun searchNpgOperations(transactionId: String): Mono<SearchNpgOperationsResponseDto> {
        val request = SearchNpgOperationsRequestDto().idTransaction(transactionId)

        return eCommerceHelpdeskApi.ecommerceSearchNpgOperations(request).doOnError {
            logger.error(
                "Error calling searchNpgOperations API for transactionId: $transactionId",
                it,
            )
        }
    }
}
