package it.pagopa.ecommerce.watchdog.deadletter.exception

import org.springframework.http.HttpStatus

class NotesLimitException :
    ApiError("Unprocessable entity. Number of notes already equal to the limit") {
    override fun toRestException(): RestApiException =
        RestApiException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Unprocessable entity. Number of notes already equal to the limit",
            message!!,
        )
}
