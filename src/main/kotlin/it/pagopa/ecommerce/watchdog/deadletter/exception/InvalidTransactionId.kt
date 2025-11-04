package it.pagopa.ecommerce.watchdog.deadletter.exception

import org.springframework.http.HttpStatus

class InvalidTransactionId : ApiError("The transaction doesn't exist") {
    override fun toRestException(): RestApiException =
        RestApiException(HttpStatus.NOT_FOUND, "The transaction doesn't exist", message!!)
}