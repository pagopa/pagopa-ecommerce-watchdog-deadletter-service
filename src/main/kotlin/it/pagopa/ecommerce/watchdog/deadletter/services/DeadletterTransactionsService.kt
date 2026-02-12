package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.clients.EcommerceHelpdeskServiceClient
import it.pagopa.ecommerce.watchdog.deadletter.clients.NodoTechnicalSupportClient
import it.pagopa.ecommerce.watchdog.deadletter.config.ActionTypeConfig
import it.pagopa.ecommerce.watchdog.deadletter.documents.Action
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidActionValue
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidTransactionId
import it.pagopa.ecommerce.watchdog.deadletter.repositories.DeadletterTransactionActionRepository
import it.pagopa.ecommerce.watchdog.deadletter.utils.ObfuscationUtils.obfuscateEmail
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterEventDto as DeadLetterEventDtoV2
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterEventDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchNpgOperationsResponseDto as SearchNpgOperationsResponseDtoV2
import it.pagopa.generated.ecommerce.helpdesk.model.SearchNpgOperationsResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionResultDto as TransactionResultDtoV2
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionResultDto
import it.pagopa.generated.ecommerce.helpdesk.model.UserInfoDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ActionTypeDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.DeadletterTransactionDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ListDeadletterTransactions200ResponseDto as ListDeadletterTransactions200ResponseDtoV1
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.PageInfoDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.DeadletterTransactionDto as DeadletterTransactionDtoV2
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.ListDeadletterTransactions200ResponseDto as ListDeadletterTransactions200ResponseDtoV2
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.PageInfoDto as PageInfoDtoV2
import it.pagopa.generated.nodo.support.model.TransactionResponseDto as TransactionResponseDtoV2
import it.pagopa.generated.nodo.support.model.TransactionResponseDto
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

@Service
class DeadletterTransactionsService(
    private val ecommerceHelpdeskServiceV1: EcommerceHelpdeskServiceClient,
    private val nodoTechnicalSupportClient: NodoTechnicalSupportClient,
    private val deadletterTransactionActionRepository: DeadletterTransactionActionRepository,
    @Autowired val actionTypeConfig: ActionTypeConfig,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun getDeadletterTransactions(
        date: LocalDate,
        pageNumber: Int,
        pageSize: Int,
    ): Mono<ListDeadletterTransactions200ResponseDtoV1> {
        return ecommerceHelpdeskServiceV1
            .getDeadletterTransactionsByFilter(date, pageSize, pageNumber)
            .onErrorResume { error ->
                logger.error(
                    "Error retrieving deadletter transactions for date [{}]: [{}]",
                    date,
                    error.message,
                    error,
                )
                // No transaction found return an empty Mono
                Mono.empty()
            }
            .flatMap { response ->
                val deadLetterEvents = response.deadLetterEvents
                val pageInfo = response.page
                val size = deadLetterEvents.size
                logger.info("Retrieved [{}] deadletter transactions.", size)

                Flux.fromIterable(deadLetterEvents)
                    .flatMap { deadLetterEvent ->
                        val transactionId = deadLetterEvent.transactionInfo?.transactionId
                        val paymentGateway = deadLetterEvent.transactionInfo?.paymentGateway

                        if (transactionId != null) {
                            val ecommerceDetailsMono: Mono<TransactionResultDto?> =
                                ecommerceHelpdeskServiceV1
                                    .searchTransactions(transactionId)
                                    .map { it.transactions.firstOrNull() }
                                    .onErrorResume { e ->
                                        logger.error(
                                            "Error retrieving eCommerce details for transactionId [{}]: [{}]",
                                            transactionId,
                                            e.message,
                                        )
                                        Mono.empty()
                                    }

                            val npgDetailsMono: Mono<SearchNpgOperationsResponseDto?> =
                                if (paymentGateway == "NPG") {
                                    ecommerceHelpdeskServiceV1
                                        .searchNpgOperations(transactionId)
                                        .onErrorResume { e ->
                                            logger.error(
                                                "Error retrieving NPG details for transactionId [{}]: [{}]",
                                                transactionId,
                                                e.message,
                                            )
                                            Mono.justOrEmpty(null)
                                        }
                                } else {
                                    Mono.justOrEmpty<SearchNpgOperationsResponseDto?>(null)
                                }

                            Mono.zip(ecommerceDetailsMono, npgDetailsMono).flatMap { tuple ->
                                val ecommerceDetails = tuple.t1
                                val npgDetails = tuple.t2
                                // val rptId =
                                //     ecommerceDetails.paymentInfo.details?.firstOrNull()?.rptId
                                // val creationDate = ecommerceDetails.transactionInfo.creationDate
                                // TODO Disable Nodo helpdesk waiting new api PIDM-1117
                                val nodoDetailsMono: Mono<TransactionResponseDto> = Mono.empty()
                                /*
                                val nodoDetailsMono: Mono<TransactionResponseDto?> =
                                    if (rptId != null && creationDate != null) {
                                        val organizationFiscalCode = rptId.substring(0, 11)
                                        val noticeNumber = rptId.substring(11)
                                        val dateFrom =
                                            convertTimestampToLocalDate(creationDate.toString())
                                        val dateTo = dateFrom

                                        nodoTechnicalSupportClient
                                            .searchNodoGivenNoticeNumberAndFiscalCode(
                                                noticeNumber,
                                                organizationFiscalCode,
                                                dateFrom,
                                                dateTo,
                                            )
                                            .onErrorResume { e ->
                                                logger.error(
                                                    "Error retrieving Nodo details for transactionId [{}]: [{}]",
                                                    transactionId,
                                                    e.message,
                                                )
                                                Mono.empty()
                                            }
                                    } else {
                                        Mono.empty()
                                    }
                                */
                                nodoDetailsMono
                                    .map {
                                        buildDeadletterTransactionDto(
                                            deadLetterEvent,
                                            ecommerceDetails,
                                            npgDetails,
                                            it,
                                        )
                                    }
                                    .switchIfEmpty {
                                        Mono.just(
                                            buildDeadletterTransactionDto(
                                                deadLetterEvent,
                                                ecommerceDetails,
                                                npgDetails,
                                                null,
                                            )
                                        )
                                    }
                            }
                        } else {
                            Mono.just(
                                buildDeadletterTransactionDto(deadLetterEvent, null, null, null)
                            )
                        }
                    }
                    .collectList()
                    .map { enrichedDeadletterTransactions ->
                        ListDeadletterTransactions200ResponseDtoV1(
                            enrichedDeadletterTransactions,
                            PageInfoDto(pageInfo.current, pageInfo.total, pageInfo.results),
                        )
                    }
            }
            .switchIfEmpty(
                Mono.fromCallable {
                    logger.warn("No deadletter transactions found for date [{}]", date)
                    ListDeadletterTransactions200ResponseDtoV1(ArrayList(), PageInfoDto(0, 0, 0))
                }
            )
            .onErrorMap { ex ->
                logger.error(
                    "Error retrieving deadletter transactions for date [{}]: [{}]",
                    date,
                    ex.message,
                    ex,
                )
                ex
            }
    }

    fun getDeadletterTransactionsByDateRange(
        fromDate: LocalDate,
        toDate: LocalDate,
        pageNumber: Int,
        pageSize: Int,
    ): Mono<ListDeadletterTransactions200ResponseDtoV2> {
        return ecommerceHelpdeskServiceV1
            .getDeadletterTransactionsByDateRange(fromDate, toDate, pageSize, pageNumber)
            .onErrorResume { error ->
                logger.error(
                    "Error retrieving deadletter transactions for range date [{},{}]: [{}]",
                    fromDate,
                    toDate,
                    error.message,
                    error,
                )
                Mono.empty()
            }
            .flatMap { response ->
                val deadLetterEvents = response.deadLetterEvents
                val pageInfo = response.page
                val size = deadLetterEvents.size

                logger.info(
                    "Retrieved [{}] deadletter transactions for range date [{},{}]:",
                    size,
                    fromDate,
                    toDate,
                )

                Flux.fromIterable(deadLetterEvents)
                    .flatMap { deadLetterEvent ->
                        val transactionId = deadLetterEvent.transactionInfo?.transactionId
                        val paymentGateway = deadLetterEvent.transactionInfo?.paymentGateway

                        if (transactionId != null) {
                            val ecommerceDetailsMono: Mono<Optional<TransactionResultDto>> =
                                ecommerceHelpdeskServiceV1
                                    .searchTransactions(transactionId)
                                    .map { Optional.ofNullable(it.transactions.firstOrNull()) }
                                    .onErrorResume { e ->
                                        logger.error(
                                            "Error retrieving eCommerce details by transactionId [{}]: [{}]",
                                            transactionId,
                                            e.message,
                                        )
                                        Mono.just(Optional.empty())
                                    }
                                    .defaultIfEmpty(Optional.empty())

                            val npgDetailsMono: Mono<Optional<SearchNpgOperationsResponseDto>> =
                                if (paymentGateway == "NPG") {
                                    ecommerceHelpdeskServiceV1
                                        .searchNpgOperations(transactionId)
                                        .map { Optional.ofNullable(it) }
                                        .onErrorResume { e ->
                                            logger.error(
                                                "Error retrieving NPG details by transactionId [{}]: [{}]",
                                                transactionId,
                                                e.message,
                                            )
                                            Mono.just(Optional.empty())
                                        }
                                        .defaultIfEmpty(Optional.empty())
                                } else {
                                    Mono.just(Optional.empty())
                                }

                            ecommerceDetailsMono.zipWith(npgDetailsMono).map {
                                (ecommerceOpt, npgOpt) ->
                                buildDeadletterTransactionDtoV2(
                                    deadLetterEvent,
                                    ecommerceOpt.orElse(null),
                                    npgOpt.orElse(null),
                                    null,
                                )
                            }
                        } else {
                            Mono.just(
                                buildDeadletterTransactionDtoV2(deadLetterEvent, null, null, null)
                            )
                        }
                    }
                    .collectList()
                    .map { enrichedDeadletterTransactions ->
                        ListDeadletterTransactions200ResponseDtoV2(
                            enrichedDeadletterTransactions,
                            PageInfoDtoV2(pageInfo.current, pageInfo.total, pageInfo.results),
                        )
                    }
            }
            .switchIfEmpty(
                Mono.fromCallable {
                    logger.warn(
                        "No deadletter transactions found for date range [{},{}]",
                        fromDate,
                        toDate,
                    )
                    ListDeadletterTransactions200ResponseDtoV2(ArrayList(), PageInfoDtoV2(0, 0, 0))
                }
            )
            .onErrorMap { ex ->
                logger.error(
                    "Error retrieving deadletter transactions for date range [{},{}]: [{}]",
                    fromDate,
                    toDate,
                    ex.message,
                    ex,
                )
                ex
            }
    }

    private fun buildDeadletterTransactionDto(
        deadLetterEvent: DeadLetterEventDto,
        ecommerceDetails: TransactionResultDto?,
        npgDetails: SearchNpgOperationsResponseDto?,
        nodoDetails: TransactionResponseDto?,
    ): DeadletterTransactionDto {
        val info = deadLetterEvent.transactionInfo
        val ecommerceStatus = ecommerceDetails?.transactionInfo?.eventStatus.toString()
        val gatewayAuthorizationStatus =
            ecommerceDetails?.transactionInfo?.gatewayAuthorizationStatus.toString()

        val paymentToken = info?.paymentTokens?.firstOrNull() ?: "N/A"

        // TO DO: review obfuscatedEmail to avoid side effect
        val userInfo: UserInfoDto? = ecommerceDetails?.userInfo
        if (userInfo != null && userInfo.notificationEmail != null) {
            val obfuscatedEmail: String? = obfuscateEmail(userInfo.notificationEmail)
            userInfo.notificationEmail(obfuscatedEmail)
            ecommerceDetails.userInfo(userInfo)
        }

        return DeadletterTransactionDto().apply {
            transactionId = info?.transactionId ?: "N/A"
            insertionDate = deadLetterEvent.timestamp
            paymentToken(paymentToken)
            paymentMethodName = info?.paymentMethodName ?: "N/A"
            pspId = info?.pspId ?: "N/A"
            eCommerceStatus(ecommerceStatus)
            gatewayAuthorizationStatus(gatewayAuthorizationStatus)
            paymentEndToEndId = info?.details?.paymentEndToEndId
            operationId = info?.details?.operationId
            deadletterTransactionDetails = deadLetterEvent
            eCommerceDetails(ecommerceDetails)
            npgDetails(npgDetails)
            nodoDetails(nodoDetails)
        }
    }

    private fun buildDeadletterTransactionDtoV2(
        deadLetterEvent: DeadLetterEventDtoV2,
        ecommerceDetails: TransactionResultDtoV2?,
        npgDetails: SearchNpgOperationsResponseDtoV2?,
        nodoDetails: TransactionResponseDtoV2?,
    ): DeadletterTransactionDtoV2 {
        val info = deadLetterEvent.transactionInfo
        val ecommerceStatus = ecommerceDetails?.transactionInfo?.eventStatus.toString()
        val gatewayAuthorizationStatus =
            ecommerceDetails?.transactionInfo?.gatewayAuthorizationStatus.toString()

        val paymentToken = info?.paymentTokens?.firstOrNull() ?: "N/A"

        // TO DO: review obfuscatedEmail to avoid side effect
        val userInfo: UserInfoDto? = ecommerceDetails?.userInfo
        if (userInfo != null && userInfo.notificationEmail != null) {
            val obfuscatedEmail: String? = obfuscateEmail(userInfo.notificationEmail)
            userInfo.notificationEmail(obfuscatedEmail)
            ecommerceDetails.userInfo(userInfo)
        }

        return DeadletterTransactionDtoV2().apply {
            transactionId = info?.transactionId ?: "N/A"
            insertionDate = deadLetterEvent.timestamp
            paymentToken(paymentToken)
            paymentMethodName = info?.paymentMethodName ?: "N/A"
            pspId = info?.pspId ?: "N/A"
            eCommerceStatus(ecommerceStatus)
            gatewayAuthorizationStatus(gatewayAuthorizationStatus)
            paymentEndToEndId = info?.details?.paymentEndToEndId
            operationId = info?.details?.operationId
            deadletterTransactionDetails = deadLetterEvent
            eCommerceDetails(ecommerceDetails)
            npgDetails(npgDetails)
            nodoDetails(nodoDetails)
        }
    }

    fun convertTimestampToLocalDate(timestamp: String): LocalDate {
        try {
            return LocalDate.parse(timestamp.substringBefore("T"))
        } catch (e: DateTimeParseException) {
            println("Error parsing date: $e")
            throw e
        }
    }

    fun addActionToDeadletterTransaction(
        transactionId: String,
        userId: String,
        actionValue: String,
    ): Mono<Action> {

        val actionTypeDto: ActionTypeDto? = actionTypeConfig.types.find { actionValue in it.value }
        if (actionTypeDto != null) {
            return ecommerceHelpdeskServiceV1
                .searchTransactions(transactionId)
                .flatMap { value ->
                    val newAction =
                        Action(
                            id = UUID.randomUUID().toString(),
                            transactionId = transactionId,
                            userId = userId,
                            action = actionTypeDto,
                            timestamp = Instant.now(),
                        )
                    deadletterTransactionActionRepository.save(newAction)
                }
                .switchIfEmpty(Mono.error(InvalidTransactionId()))
        } else return Mono.error(InvalidActionValue())
    }

    fun listActionsForDeadletterTransaction(transactionId: String, userId: String): Flux<Action> {
        logger.info(
            "Retrieving actions for deadletter transaction with ID: [{}] requested by user: [{}]",
            transactionId,
            userId,
        )
        return deadletterTransactionActionRepository.findByTransactionId(transactionId)
    }
}
