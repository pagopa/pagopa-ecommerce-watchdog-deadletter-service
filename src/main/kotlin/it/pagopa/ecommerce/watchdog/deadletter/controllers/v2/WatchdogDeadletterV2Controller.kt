package it.pagopa.ecommerce.watchdog.deadletter.controllers.v2

import it.pagopa.ecommerce.watchdog.deadletter.services.AuthService
import it.pagopa.ecommerce.watchdog.deadletter.services.DeadletterTransactionsService
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.api.V2Api
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.ListDeadletterTransactions200ResponseDto
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController("WatchdogDeadletterV2Controller")
@Validated
class WatchdogDeadletterV2Controller(
    @Autowired val deadletterTransactionsService: DeadletterTransactionsService,
    @Autowired val authService: AuthService,
) : V2Api {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun listDeadletterTransactions(
        @RequestParam("fromDate") @NotNull @Valid fromDate: LocalDate,
        @RequestParam("toDate") @NotNull @Valid toDate: LocalDate,
        @RequestParam("pageNumber") @NotNull @Min(value = 0) @Valid pageNumber: Int,
        @RequestParam("pageSize") @NotNull @Min(value = 1) @Max(value = 20) @Valid pageSize: Int,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<ListDeadletterTransactions200ResponseDto?>?>? {
        logger.info("Received listDeadletterTransactions request for [{},{}] ", fromDate, toDate)
        return deadletterTransactionsService
            .getDeadletterTransactionsByDateRange(fromDate, toDate, pageNumber, pageSize)
            .map { transactions -> ResponseEntity.ok(transactions) }
    }
}
