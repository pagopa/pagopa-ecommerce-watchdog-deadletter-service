package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.clients.EcommerceHelpdeskServiceClient
import it.pagopa.ecommerce.watchdog.deadletter.clients.NodoTechnicalSupportClient
import it.pagopa.ecommerce.watchdog.deadletter.config.ActionTypeConfig
import it.pagopa.ecommerce.watchdog.deadletter.documents.Action
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidActionValue
import it.pagopa.ecommerce.watchdog.deadletter.repositories.DeadletterTransactionActionRepository
import it.pagopa.ecommerce.watchdog.deadletter.utils.ObfuscationUtils.obfuscateEmail
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterEventDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchNpgOperationsResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionResultDto
import it.pagopa.generated.ecommerce.helpdesk.model.UserInfoDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ActionTypeDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.DeadletterTransactionDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ListDeadletterTransactions200ResponseDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.PageInfoDto
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
    ): Mono<ListDeadletterTransactions200ResponseDto> {
        return ecommerceHelpdeskServiceV1
            .getDeadletterTransactionsByFilter(date, pageSize, pageNumber)
            .flatMap { response ->
                val deadLetterEvents = response.deadLetterEvents
                val pageInfo = response.page
                val size = deadLetterEvents.size
                logger.info("Retrieved [{}] deadletter transactions.", size)

                Flux.fromIterable(deadLetterEvents)
                    .flatMap { deadLetterEvent ->
                        val transactionId = deadLetterEvent.transactionInfo?.transactionId

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
                                ecommerceHelpdeskServiceV1
                                    .searchNpgOperations(transactionId)
                                    .onErrorResume { e ->
                                        logger.error(
                                            "Error retrieving NPG details for transactionId [{}]: [{}]",
                                            transactionId,
                                            e.message,
                                        )
                                        Mono.empty()
                                    }

                            Mono.zip(ecommerceDetailsMono, npgDetailsMono).flatMap { tuple ->
                                val ecommerceDetails = tuple.t1
                                val npgDetails = tuple.t2
                                val rptId =
                                    ecommerceDetails.paymentInfo.details?.firstOrNull()?.rptId
                                val creationDate = ecommerceDetails.transactionInfo.creationDate

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

                                Mono.zip(
                                        Mono.justOrEmpty(ecommerceDetails),
                                        Mono.justOrEmpty(npgDetails),
                                        nodoDetailsMono,
                                    )
                                    .map { nestedTuple ->
                                        val ecommerceDetailsResult = nestedTuple.t1
                                        val npgDetailsResult = nestedTuple.t2
                                        val nodoDetailsResult = nestedTuple.t3

                                        buildDeadletterTransactionDto(
                                            deadLetterEvent,
                                            ecommerceDetailsResult,
                                            npgDetailsResult,
                                            nodoDetailsResult,
                                        )
                                    }
                                    .defaultIfEmpty(
                                        buildDeadletterTransactionDto(
                                            deadLetterEvent,
                                            null,
                                            null,
                                            null,
                                        )
                                    )
                            }
                        } else {
                            Mono.just(
                                buildDeadletterTransactionDto(deadLetterEvent, null, null, null)
                            )
                        }
                    }
                    .collectList()
                    .map { enrichedDeadletterTransactions ->
                        ListDeadletterTransactions200ResponseDto(
                            enrichedDeadletterTransactions,
                            PageInfoDto(pageInfo.current, pageInfo.total, pageInfo.results),
                        )
                    }
            }
            .switchIfEmpty(
                Mono.fromCallable {
                    logger.warn("No deadletter transactions found for date [{}]", date)
                    ListDeadletterTransactions200ResponseDto(emptyList(), PageInfoDto(0, 0, 0))
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
            val newAction =
                Action(
                    id = UUID.randomUUID().toString(),
                    transactionId = transactionId,
                    userId = userId,
                    action = actionTypeDto,
                    timestamp = Instant.now(),
                )
            return deadletterTransactionActionRepository.save(newAction)
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
