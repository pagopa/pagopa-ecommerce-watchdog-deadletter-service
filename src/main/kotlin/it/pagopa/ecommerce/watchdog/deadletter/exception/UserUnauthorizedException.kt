package it.pagopa.ecommerce.watchdog.deadletter.exception

import org.springframework.http.HttpStatus

class UserUnauthorizedException : ApiError("User is not authorized to access the resource") {
    override fun toRestException(): RestApiException =
        RestApiException(HttpStatus.UNAUTHORIZED, "User unauthorized", message!!)
}
