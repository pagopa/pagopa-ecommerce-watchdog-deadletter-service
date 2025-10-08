package it.pagopa.ecommerce.watchdog.deadletter

import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ProblemJsonDto
import org.springframework.http.HttpStatus

object WatchdogDeadletterTestUtils {

    fun buildProblemJson(
        httpStatus: HttpStatus,
        title: String,
        description: String,
    ): ProblemJsonDto = ProblemJsonDto().status(httpStatus.value()).detail(description).title(title)
}
