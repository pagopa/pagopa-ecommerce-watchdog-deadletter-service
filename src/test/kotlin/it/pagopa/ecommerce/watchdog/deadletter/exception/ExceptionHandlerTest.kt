package it.pagopa.ecommerce.watchdog.deadletter.exception

import it.pagopa.ecommerce.watchdog.deadletter.WatchdogDeadletterTestUtils
import it.pagopa.ecommerce.watchdog.deadletter.exception.handler.ExceptionHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ExceptionHandlerTest {

    private val exceptionHandler = ExceptionHandler()

    @Test
    fun `Should handle RestApiException`() {
        val response =
            exceptionHandler.handleException(
                RestApiException(
                    httpStatus = HttpStatus.BAD_REQUEST,
                    title = "title",
                    description = "description",
                )
            )
        assertEquals(
            WatchdogDeadletterTestUtils.buildProblemJson(
                httpStatus = HttpStatus.BAD_REQUEST,
                title = "title",
                description = "description",
            ),
            response.body,
        )
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `Should handle ApiError`() {
        val exception = UserUnauthorizedException()
        val response = exceptionHandler.handleException(exception)
        assertEquals(
            WatchdogDeadletterTestUtils.buildProblemJson(
                httpStatus = HttpStatus.UNAUTHORIZED,
                title = "Operator unauthorized",
                description = "Operator is not authorized to access the resource",
            ),
            response.body,
        )
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }
}
