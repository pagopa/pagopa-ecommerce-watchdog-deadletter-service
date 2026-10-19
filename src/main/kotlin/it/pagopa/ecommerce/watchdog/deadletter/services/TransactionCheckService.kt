package it.pagopa.ecommerce.watchdog.deadletter.services

import com.fasterxml.jackson.databind.ObjectMapper
import it.pagopa.ecommerce.watchdog.deadletter.clients.EcommerceHelpdeskCommandsClient
import it.pagopa.ecommerce.watchdog.deadletter.clients.EcommerceHelpdeskServiceClient
import it.pagopa.ecommerce.watchdog.deadletter.clients.NodoTechnicalSupportClient
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidTransactionId
import it.pagopa.ecommerce.watchdog.deadletter.exception.RefundGatewayException
import it.pagopa.ecommerce.watchdog.deadletter.exception.TransactionNotRefundableException
import it.pagopa.ecommerce.watchdog.deadletter.utils.LogFields
import it.pagopa.ecommerce.watchdog.deadletter.utils.LogFields.ACTION_REFUND_TRANSACTION
import it.pagopa.ecommerce.watchdog.deadletter.utils.LogFields.OUTCOME_FAILURE
import it.pagopa.ecommerce.watchdog.deadletter.utils.LogFields.OUTCOME_SUCCESS
import it.pagopa.ecommerce.watchdog.deadletter.utils.ObfuscationUtils.obfuscateEmail
import it.pagopa.ecommerce.watchdog.deadletter.utils.event
import it.pagopa.generated.ecommerce.helpdesk.commands.model.RefundTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.OperationTypeDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchNpgOperationsResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionResultDto
import it.pagopa.generated.ecommerce.watchdog.transactions.v1.model.TransactionCheckDto
import it.pagopa.generated.ecommerce.watchdog.transactions.v1.model.TransactionEventDto
import it.pagopa.generated.ecommerce.watchdog.transactions.v1.model.TransactionRefundRequestDto
import it.pagopa.generated.ecommerce.watchdog.transactions.v1.model.TransactionStatusResponseDto
import it.pagopa.generated.nodo.support.model.PositionPaymentSnapshotDtoDto
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Optional
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

enum class RefundOutcome {
    /** Refund queued downstream, the client should poll the eCommerce status. */
    ACCEPTED,
    /** Forced refund executed on the payment gateway. */
    COMPLETED,
}

@Service
class TransactionCheckService(
    private val helpdeskClient: EcommerceHelpdeskServiceClient,
    private val nodoTechnicalSupportClient: NodoTechnicalSupportClient,
    private val helpdeskCommandsClient: EcommerceHelpdeskCommandsClient,
    private val totpService: TotpService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()

    fun getTransaction(transactionId: String): Mono<TransactionCheckDto> =
        findTransaction(transactionId, LogFields.ACTION_GET_TRANSACTION).flatMap { transaction ->
            Mono.zip(
                    fetchNpgDetails(transactionId, transaction, LogFields.ACTION_GET_TRANSACTION),
                    fetchNodoDetails(transactionId, transaction),
                )
                .map { (npgOpt, nodoOpt) ->
                    buildTransactionCheck(
                        transactionId,
                        transaction,
                        npgOpt.orElse(null),
                        nodoOpt.orElse(null),
                    )
                }
        }

    fun getTransactionStatus(transactionId: String): Mono<TransactionStatusResponseDto> =
        findTransaction(transactionId, LogFields.ACTION_GET_TRANSACTION_STATUS).map {
            TransactionStatusResponseDto(it.transactionInfo.eventStatus?.value ?: "UNKNOWN")
        }

    fun refundTransaction(
        transactionId: String,
        userId: String,
        request: TransactionRefundRequestDto,
    ): Mono<RefundOutcome> =
        if (request.force == true) forceRefund(transactionId, userId, request.otpCode)
        else requestRefund(transactionId, userId, request.otpCode)

    private fun requestRefund(
        transactionId: String,
        userId: String,
        otpCode: String,
    ): Mono<RefundOutcome> =
        totpService
            .verifyAndConsume(userId, otpCode, transactionId)
            .then(
                Mono.defer {
                    helpdeskCommandsClient
                        .requestTransactionRefund(transactionId, userId)
                        .doOnSuccess {
                            logRefundCall(
                                Level.INFO,
                                "Requested transaction refund",
                                transactionId,
                                userId,
                                REQUEST_REFUND_PATH,
                            )
                        }
                        .doOnError {
                            logRefundCall(
                                failureLevel(it),
                                "Failed to request transaction refund",
                                transactionId,
                                userId,
                                REQUEST_REFUND_PATH,
                                it,
                            )
                        }
                }
            )
            .thenReturn(RefundOutcome.ACCEPTED)
            .onErrorMap(WebClientResponseException::class.java) { mapDownstreamError(it) }

    // The request is built before consuming the OTP, so a non refundable transaction does not burn
    // the operator's code.
    private fun forceRefund(
        transactionId: String,
        userId: String,
        otpCode: String,
    ): Mono<RefundOutcome> =
        buildForceRefundRequest(transactionId, userId).flatMap { refundRequest ->
            totpService
                .verifyAndConsume(userId, otpCode, transactionId)
                .then(
                    Mono.defer {
                        helpdeskCommandsClient
                            .refundOperation(refundRequest, userId)
                            .doOnSuccess {
                                logRefundCall(
                                    Level.INFO,
                                    "Forced transaction refund",
                                    transactionId,
                                    userId,
                                    FORCE_REFUND_PATH,
                                    details = mapOf("refund_operation_id" to it?.refundOperationId),
                                )
                            }
                            .doOnError {
                                logRefundCall(
                                    failureLevel(it),
                                    "Failed to force transaction refund",
                                    transactionId,
                                    userId,
                                    FORCE_REFUND_PATH,
                                    it,
                                )
                            }
                    }
                )
                .thenReturn(RefundOutcome.COMPLETED)
                .onErrorMap(WebClientResponseException::class.java) { mapDownstreamError(it) }
        }

    private fun buildForceRefundRequest(
        transactionId: String,
        userId: String,
    ): Mono<RefundTransactionRequestDto> =
        findTransaction(transactionId, ACTION_REFUND_TRANSACTION).flatMap { transaction ->
            val info = transaction.transactionInfo
            if (info.paymentGateway != "NPG") {
                return@flatMap rejectForcedRefund(
                    transactionId,
                    userId,
                    "Forced refund is supported only for NPG transactions",
                )
            }
            fetchNpgDetails(transactionId, transaction, ACTION_REFUND_TRANSACTION).flatMap { npgOpt
                ->
                val operationId =
                    npgOpt
                        .orElse(null)
                        ?.operations
                        ?.firstOrNull { it.operationType == OperationTypeDto.AUTHORIZATION }
                        ?.operationId ?: info.authorizationOperationId
                // Optional fields of the helpdesk payload: check the source values, not the request
                val paymentMethodName = info.paymentMethodName
                val pspId = transaction.pspInfo.pspId
                val correlationId = info.correlationId?.toString()
                val amount = info.grandTotal
                if (
                    listOf(paymentMethodName, pspId, operationId, correlationId, amount).any {
                        it == null
                    }
                ) {
                    rejectForcedRefund(
                        transactionId,
                        userId,
                        "Missing data to perform a forced refund for this transaction",
                    )
                } else {
                    Mono.just(
                        RefundTransactionRequestDto()
                            .transactionId(transactionId)
                            .paymentMethodName(paymentMethodName)
                            .pspId(pspId)
                            .operationId(operationId)
                            .correlationId(correlationId)
                            .amount(amount)
                    )
                }
            }
        }

    private fun <T> rejectForcedRefund(
        transactionId: String,
        userId: String,
        reason: String,
    ): Mono<T> {
        logger.event(
            Level.WARN,
            "Rejected forced refund",
            LogFields.of(
                ACTION_REFUND_TRANSACTION,
                OUTCOME_FAILURE,
                transactionId,
                userId,
                mapOf("reason" to reason),
            ),
        )
        return Mono.error(TransactionNotRefundableException(reason))
    }

    private fun logRefundCall(
        level: Level,
        message: String,
        transactionId: String,
        userId: String,
        path: String,
        error: Throwable? = null,
        details: Map<String, Any?> = emptyMap(),
    ) =
        logger.event(
            level,
            message,
            LogFields.of(
                ACTION_REFUND_TRANSACTION,
                if (error == null) OUTCOME_SUCCESS else OUTCOME_FAILURE,
                transactionId,
                userId,
                mapOf(
                    "dependency" to LogFields.DEPENDENCY_HELPDESK_COMMANDS,
                    "path" to path,
                    "http_status" to (error as? WebClientResponseException)?.statusCode?.value(),
                ) + details,
            ),
            error,
        )

    // Downstream 4xx are business outcomes handled by the caller, not anomalies of this service.
    private fun failureLevel(e: Throwable): Level =
        if (e is WebClientResponseException && e.statusCode.is4xxClientError) Level.WARN
        else Level.ERROR

    private fun mapDownstreamError(e: WebClientResponseException): Throwable =
        when (e.statusCode.value()) {
            HttpStatus.NOT_FOUND.value() -> InvalidTransactionId()
            HttpStatus.UNPROCESSABLE_ENTITY.value() ->
                TransactionNotRefundableException("Transaction not in a refundable state")
            else -> RefundGatewayException(downstreamDetail(e))
        }

    private fun downstreamDetail(e: WebClientResponseException): String =
        runCatching { objectMapper.readTree(e.responseBodyAsString).path("detail").asText("") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "Refund service answered with status ${e.statusCode.value()}"

    private fun findTransaction(transactionId: String, action: String): Mono<TransactionResultDto> =
        helpdeskClient
            .searchTransactions(transactionId)
            .doOnSuccess {
                logger.event(
                    Level.INFO,
                    "Retrieved transaction",
                    LogFields.of(
                        action,
                        OUTCOME_SUCCESS,
                        transactionId,
                        details =
                            mapOf(
                                "dependency" to LogFields.DEPENDENCY_HELPDESK,
                                "path" to SEARCH_TRANSACTION_PATH,
                                "found" to (it?.transactions?.isNotEmpty() == true),
                            ),
                    ),
                )
            }
            .doOnError {
                logger.event(
                    Level.ERROR,
                    "Failed to retrieve transaction",
                    LogFields.of(
                        action,
                        OUTCOME_FAILURE,
                        transactionId,
                        details =
                            mapOf(
                                "dependency" to LogFields.DEPENDENCY_HELPDESK,
                                "path" to SEARCH_TRANSACTION_PATH,
                            ),
                    ),
                    it,
                )
            }
            .mapNotNull<TransactionResultDto> { it.transactions.firstOrNull() }
            .onErrorMap(WebClientResponseException::class.java) {
                if (it.statusCode == HttpStatus.NOT_FOUND) InvalidTransactionId() else it
            }
            .switchIfEmpty(Mono.error(InvalidTransactionId()))

    private fun fetchNpgDetails(
        transactionId: String,
        transaction: TransactionResultDto,
        action: String,
    ): Mono<Optional<SearchNpgOperationsResponseDto>> {
        if (transaction.transactionInfo.paymentGateway != "NPG") return Mono.just(Optional.empty())
        val details =
            mapOf("dependency" to LogFields.DEPENDENCY_HELPDESK, "path" to SEARCH_NPG_PATH)
        return helpdeskClient
            .searchNpgOperations(transactionId)
            .map { Optional.ofNullable(it) }
            .defaultIfEmpty(Optional.empty())
            .doOnNext {
                logger.event(
                    Level.INFO,
                    "Retrieved NPG operations",
                    LogFields.of(action, OUTCOME_SUCCESS, transactionId, details = details),
                )
            }
            .onErrorResume { e ->
                logger.event(
                    Level.WARN,
                    "Failed to retrieve NPG operations",
                    LogFields.of(action, OUTCOME_FAILURE, transactionId, details = details),
                    e,
                )
                Mono.just(Optional.empty())
            }
    }

    private fun fetchNodoDetails(
        transactionId: String,
        transaction: TransactionResultDto,
    ): Mono<Optional<PositionPaymentSnapshotDtoDto>> {
        val paymentToken =
            transaction.paymentInfo.details?.firstOrNull()?.paymentToken
                ?: return Mono.just(Optional.empty())
        val details =
            mapOf(
                "dependency" to LogFields.DEPENDENCY_NODO,
                "path" to NODO_PAYMENT_TOKEN_PATH,
                "payment_token" to paymentToken,
            )
        return nodoTechnicalSupportClient
            .paymentTokenPaymentTokenGet(paymentToken)
            .map { Optional.ofNullable(it) }
            .defaultIfEmpty(Optional.empty())
            .doOnNext {
                logger.event(
                    Level.INFO,
                    "Retrieved Nodo payment snapshot",
                    LogFields.of(
                        LogFields.ACTION_GET_TRANSACTION,
                        OUTCOME_SUCCESS,
                        transactionId,
                        details = details,
                    ),
                )
            }
            .onErrorResume { e ->
                logger.event(
                    Level.WARN,
                    "Failed to retrieve Nodo payment snapshot",
                    LogFields.of(
                        LogFields.ACTION_GET_TRANSACTION,
                        OUTCOME_FAILURE,
                        transactionId,
                        details = details,
                    ),
                    e,
                )
                Mono.just(Optional.empty())
            }
    }

    /**
     * Parses the operation time as an instant. The helpdesk contract allows ISO-8601 timestamps
     * with any offset; timestamps without timezone (e.g. `2025-10-28 11:51:03.455`) are assumed to
     * be UTC. Missing or unparsable values are considered the oldest.
     */
    private fun parseOperationTime(value: String?): Instant? =
        if (value.isNullOrBlank()) {
            null
        } else {
            runCatching { OffsetDateTime.parse(value).toInstant() }
                .recoverCatching {
                    LocalDateTime.parse(value.trim().replace(' ', 'T')).toInstant(ZoneOffset.UTC)
                }
                .getOrNull()
        }

    private fun buildTransactionCheck(
        transactionId: String,
        transaction: TransactionResultDto,
        npgDetails: SearchNpgOperationsResponseDto?,
        nodoDetails: PositionPaymentSnapshotDtoDto?,
    ): TransactionCheckDto {
        val info = transaction.transactionInfo
        val npgOperationResult =
            npgDetails
                ?.operations
                ?.maxByOrNull { parseOperationTime(it.operationTime) ?: Instant.MIN }
                ?.operationResult
                ?.value

        transaction.userInfo.notificationEmail?.let {
            transaction.userInfo.notificationEmail(obfuscateEmail(it))
        }

        return TransactionCheckDto()
            .transactionId(transactionId)
            .eCommerceStatus(info.eventStatus?.value)
            .nodoStatus(nodoDetails?.status)
            .gatewayStatus(npgOperationResult ?: info.gatewayAuthorizationStatus)
            .events(
                info.events.orEmpty().map {
                    TransactionEventDto().eventCode(it.eventCode).creationDate(it.creationDate)
                }
            )
            .eCommerceDetails(transaction)
            .npgDetails(npgDetails)
            .nodoDetails(nodoDetails)
    }

    companion object {
        private const val SEARCH_TRANSACTION_PATH = "/v2/ecommerce/searchTransaction"
        private const val SEARCH_NPG_PATH = "/v1/ecommerce/searchNpgOperations"
        private const val NODO_PAYMENT_TOKEN_PATH = "/paymentToken/{paymentToken}"
        private const val REQUEST_REFUND_PATH = "/commands/transactions/{transactionId}/refund"
        private const val FORCE_REFUND_PATH = "/commands/refund"
    }
}
