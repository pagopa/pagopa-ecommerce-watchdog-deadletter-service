package it.pagopa.ecommerce.watchdog.deadletter.controllers

import it.pagopa.ecommerce.watchdog.deadletter.services.DeadletterTransactionsService
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.api.DeadletterTransactionsApi
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.DeadletterTransactionActionDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.DeadletterTransactionActionInputDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ListDeadletterTransactions200ResponseDto
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.ZoneOffset
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class WatchdogDeadletterController(
    @Autowired val deadletterTransactionsService: DeadletterTransactionsService
) : DeadletterTransactionsApi {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun addActionToDeadletterTransaction(
        deadletterTransactionId: String,
        xUserId: @NotNull String,
        deadletterTransactionActionInputDto: @Valid Mono<DeadletterTransactionActionInputDto>,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<Void>> {
        return deadletterTransactionActionInputDto
            .flatMap { actionDto ->
                deadletterTransactionsService.addActionToDeadletterTransaction(
                    deadletterTransactionId,
                    xUserId,
                    actionDto.value,
                )
            }
            .thenReturn(ResponseEntity.accepted().build())
    }

    override fun listActionsForDeadletterTransaction(
        deadletterTransactionId: String,
        xUserId: @NotNull String,
        exchange: ServerWebExchange?,
    ): Mono<ResponseEntity<Flux<DeadletterTransactionActionDto>>> {
        return Mono.just(
            ResponseEntity.ok(
                deadletterTransactionsService
                    .listActionsForDeadletterTransaction(deadletterTransactionId, xUserId)
                    .map {
                        DeadletterTransactionActionDto(
                            it.id,
                            it.transactionId,
                            it.userId,
                            it.value,
                            it.timestamp.atOffset(ZoneOffset.UTC),
                        )
                    }
            )
        )
    }

    override fun listDeadletterTransactions(
        pageNumber: @NotNull @Min(value = 0) @Valid Int,
        pageSize: @NotNull @Min(value = 1) @Max(value = 20) @Valid Int,
        xUserId: @NotNull String,
        date: @Valid LocalDate,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<ListDeadletterTransactions200ResponseDto>> {
        logger.info("Received listDeadletterTransactions request for [{}] ", date)
        return deadletterTransactionsService
            .getDeadletterTransactions(date, pageNumber, pageSize)
            .map { transactions -> ResponseEntity.ok(transactions) }
    }
}
