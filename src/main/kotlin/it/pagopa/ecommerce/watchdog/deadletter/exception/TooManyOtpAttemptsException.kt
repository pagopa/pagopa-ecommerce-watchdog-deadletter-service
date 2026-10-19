package it.pagopa.ecommerce.watchdog.deadletter.exception

import org.springframework.http.HttpStatus

class TooManyOtpAttemptsException : ApiError("Too many invalid OTP attempts, retry later") {
    override fun toRestException(): RestApiException =
        RestApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many OTP attempts", message!!)
}
