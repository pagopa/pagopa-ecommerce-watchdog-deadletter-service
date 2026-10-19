package it.pagopa.ecommerce.watchdog.deadletter.controllers.v1

import it.pagopa.ecommerce.watchdog.deadletter.services.AuthService
import it.pagopa.ecommerce.watchdog.deadletter.services.RefundOutcome
import it.pagopa.ecommerce.watchdog.deadletter.services.TransactionCheckService
import it.pagopa.generated.ecommerce.watchdog.transactions.v1.api.TransactionsApi
import it.pagopa.generated.ecommerce.watchdog.transactions.v1.model.TransactionCheckDto
import it.pagopa.generated.ecommerce.watchdog.transactions.v1.model.TransactionRefundRequestDto
import it.pagopa.generated.ecommerce.watchdog.transactions.v1.model.TransactionStatusResponseDto
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

@RestController("TransactionsController")
@Validated
class TransactionsController(
    @Autowired val authService: AuthService,
    @Autowired val transactionCheckService: TransactionCheckService,
) : TransactionsApi {

    override fun getTransaction(
        transactionId: String,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<TransactionCheckDto>> =
        transactionCheckService.getTransaction(transactionId).map { ResponseEntity.ok(it) }

    override fun getTransactionStatus(
        transactionId: String,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<TransactionStatusResponseDto>> =
        transactionCheckService.getTransactionStatus(transactionId).map { ResponseEntity.ok(it) }

    override fun refundTransaction(
        transactionId: String,
        transactionRefundRequestDto: @Valid Mono<TransactionRefundRequestDto>,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<Void>> =
        Mono.zip(transactionRefundRequestDto, authService.getAuthenticatedUserId()).flatMap {
            (request, userId) ->
            transactionCheckService.refundTransaction(transactionId, userId, request).map { outcome
                ->
                when (outcome) {
                    RefundOutcome.ACCEPTED -> ResponseEntity.status(HttpStatus.CREATED).build()
                    RefundOutcome.COMPLETED -> ResponseEntity.ok().build()
                }
            }
        }
}
