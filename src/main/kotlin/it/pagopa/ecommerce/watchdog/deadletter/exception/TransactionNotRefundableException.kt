package it.pagopa.ecommerce.watchdog.deadletter.exception

import org.springframework.http.HttpStatus

class TransactionNotRefundableException(detail: String) : ApiError(detail) {
    override fun toRestException(): RestApiException =
        RestApiException(HttpStatus.CONFLICT, "Transaction not refundable", message!!)
}
