package it.pagopa.ecommerce.watchdog.deadletter.clients

import it.pagopa.generated.ecommerce.helpdesk.commands.api.EcommerceApi
import it.pagopa.generated.ecommerce.helpdesk.commands.model.RefundTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.commands.model.RefundTransactionResponseDto
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class EcommerceHelpdeskCommandsClient(private val commandsApi: EcommerceApi) {

    /** Queues a refund request for the transaction (async, downstream answers 202). */
    fun requestTransactionRefund(transactionId: String, userId: String): Mono<Void> =
        commandsApi.requestTransactionRefund(transactionId, userId)

    /** Performs the refund synchronously on the payment gateway. */
    fun refundOperation(
        request: RefundTransactionRequestDto,
        userId: String,
    ): Mono<RefundTransactionResponseDto> = commandsApi.refundOperation(userId, request)
}
