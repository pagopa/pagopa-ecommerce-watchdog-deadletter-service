package it.pagopa.ecommerce.watchdog.deadletter.exception

import org.springframework.http.HttpStatus

class InvalidActionValue : ApiError("The action value provided is not allowed") {
    override fun toRestException(): RestApiException =
        RestApiException(HttpStatus.BAD_REQUEST, "Invalid action value", message!!)
}
