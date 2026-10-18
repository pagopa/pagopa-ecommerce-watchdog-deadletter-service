package it.pagopa.ecommerce.watchdog.deadletter.exception

import org.springframework.http.HttpStatus

class InvalidOtpException : ApiError("Invalid OTP code") {
    override fun toRestException(): RestApiException =
        RestApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid OTP code", message!!)
}
