package it.pagopa.ecommerce.watchdog.deadletter.exception

import org.springframework.http.HttpStatus

class RefundGatewayException(detail: String) : ApiError(detail) {
    override fun toRestException(): RestApiException =
        RestApiException(
            HttpStatus.BAD_GATEWAY,
            "Refund rejected by the payment gateway",
            message!!,
        )
}
