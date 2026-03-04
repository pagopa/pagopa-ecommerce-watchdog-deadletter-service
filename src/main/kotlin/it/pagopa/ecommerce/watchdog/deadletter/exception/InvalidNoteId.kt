package it.pagopa.ecommerce.watchdog.deadletter.exception

import org.springframework.http.HttpStatus

class InvalidNoteId : ApiError("The note doesn't exist") {
    override fun toRestException(): RestApiException =
        RestApiException(HttpStatus.NOT_FOUND, "The note doesn't exist", message!!)
}
