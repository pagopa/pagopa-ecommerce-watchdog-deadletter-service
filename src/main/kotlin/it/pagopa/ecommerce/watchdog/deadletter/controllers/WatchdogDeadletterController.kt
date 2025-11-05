package it.pagopa.ecommerce.watchdog.deadletter.controllers

import it.pagopa.ecommerce.watchdog.deadletter.services.AuthService
import it.pagopa.ecommerce.watchdog.deadletter.services.DeadletterTransactionsService
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.api.DeadletterTransactionsApi
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.DeadletterTransactionActionDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.DeadletterTransactionActionInputDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ListDeadletterTransactions200ResponseDto
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.net.URI
import java.time.LocalDate
import java.time.ZoneOffset
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@Validated
class WatchdogDeadletterController(
    @Autowired val deadletterTransactionsService: DeadletterTransactionsService,
    @Autowired val authService: AuthService,
) : DeadletterTransactionsApi {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun addActionToDeadletterTransaction(
        deadletterTransactionId: String,
        deadletterTransactionActionInputDto: @Valid Mono<DeadletterTransactionActionInputDto>,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<Void>> {
        return deadletterTransactionActionInputDto
            .flatMap { actionDto ->
                authService.getAuthenticatedUserId().flatMap { userId ->
                    deadletterTransactionsService.addActionToDeadletterTransaction(
                        deadletterTransactionId,
                        userId,
                        actionDto.value,
                    )
                }
            }
            .thenReturn(ResponseEntity.created(URI("")).build())
    }

    override fun listActionsForDeadletterTransaction(
        deadletterTransactionId: String,
        exchange: ServerWebExchange?,
    ): Mono<ResponseEntity<Flux<DeadletterTransactionActionDto>>> {
        return authService.getAuthenticatedUserId().map { userId ->
            ResponseEntity.ok(
                deadletterTransactionsService
                    .listActionsForDeadletterTransaction(deadletterTransactionId, userId)
                    .map {
                        DeadletterTransactionActionDto(
                            it.id,
                            it.transactionId,
                            it.userId,
                            it.action,
                            it.timestamp.atOffset(ZoneOffset.UTC),
                        )
                    }
            )
        }
    }

    override fun listDeadletterTransactions(
        @RequestParam("pageNumber") @NotNull @Min(value = 0) pageNumber: Int,
        @RequestParam("pageSize") @NotNull @Min(value = 1) @Max(value = 500) pageSize: Int,
        @RequestParam("date") date: LocalDate,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<ListDeadletterTransactions200ResponseDto>> {
        logger.info("Received listDeadletterTransactions request for [{}] ", date)
        return deadletterTransactionsService
            .getDeadletterTransactions(date, pageNumber, pageSize)
            .map { transactions -> ResponseEntity.ok(transactions) }
    }
}
