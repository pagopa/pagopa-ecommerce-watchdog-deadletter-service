package it.pagopa.ecommerce.watchdog.deadletter.exception

import org.springframework.http.HttpStatus

class InvalidCredentialsException : ApiError("The credential provided are not valid") {
    override fun toRestException(): RestApiException =
        RestApiException(HttpStatus.UNAUTHORIZED, "User not found", message!!)
}
