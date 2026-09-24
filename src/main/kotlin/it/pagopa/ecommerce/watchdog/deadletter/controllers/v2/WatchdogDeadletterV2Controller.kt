package it.pagopa.ecommerce.watchdog.deadletter.controllers.v2

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import it.pagopa.ecommerce.watchdog.deadletter.services.AuthService
import it.pagopa.ecommerce.watchdog.deadletter.services.DeadletterTransactionsService
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.api.V2Api
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.DeadletterTransactionActionDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.DeadletterTransactionActionsRequestDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.ListDeadletterTransactions200ResponseDto
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import net.minidev.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.ResponseEntity
import org.springframework.security.web.server.firewall.StrictServerWebExchangeFirewall
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.router
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@RestController("WatchdogDeadletterV2Controller")
@Validated
class WatchdogDeadletterV2Controller(
    @Autowired val deadletterTransactionsService: DeadletterTransactionsService,
    @Autowired val authService: AuthService,
) : V2Api {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun listActionsForDeadletterTransaction(
        deadletterTransactionActionsRequestDto: @Valid Mono<DeadletterTransactionActionsRequestDto>,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<Flux<List<DeadletterTransactionActionDto>>>> {
        logger.info("Received actions request for multiple transaction ids")
        return deadletterTransactionActionsRequestDto.map {
            ResponseEntity.ok(
                deadletterTransactionsService.listActionsForDeadletterTransactions(it)
            )
        }
    }

    override fun listDeadletterTransactions(
        @RequestParam("fromDate") @NotNull @Valid fromDate: LocalDate,
        @RequestParam("toDate") @NotNull @Valid toDate: LocalDate,
        @RequestParam("pageNumber") @NotNull @Min(value = 0) @Valid pageNumber: Int,
        @RequestParam("pageSize") @NotNull @Min(value = 1) @Max(value = 1000) @Valid pageSize: Int,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<ListDeadletterTransactions200ResponseDto>> {
        logger.info("Received listDeadletterTransactions request for [{},{}] ", fromDate, toDate)
        return deadletterTransactionsService
            .getDeadletterTransactionsByDateRange(fromDate, toDate, pageNumber, pageSize)
            .map { transactions -> ResponseEntity.ok(transactions) }
    }

    @Bean
    fun httpFirewall(): StrictServerWebExchangeFirewall {
        val firewall = StrictServerWebExchangeFirewall()
        firewall.setAllowedHttpMethods(
            buildList {
                addAll(HttpMethod.values())
                add(HttpMethod.valueOf("QUERY"))
            }
        )
        return firewall
    }

    @Bean
    fun endpoints() = router {
        (accept(APPLICATION_JSON) and "/test_query").nest {
            method(HttpMethod.valueOf("QUERY"))
                .invoke(helloQuery)
        }
    }

    val helloQuery: (ServerRequest) -> Mono<ServerResponse> = { req ->
        req.bodyToMono<Map<String, String>>()
            .flatMap {
                logger.info(it.toString())
                val defaultName = "someone who forgot to set the name in the body"
                val name = it?.getOrDefault("name", defaultName)
                val result = JsonNodeFactory.instance.objectNode()
                result.put("result", "Hello, ${name ?: defaultName}!")
                ServerResponse.ok().bodyValue(result)
            }
            .switchIfEmpty {
                val result = JsonNodeFactory.instance.objectNode()
                result.put("result", "Hello, someone who forgot to send a valid JSON body!")
                ServerResponse.ok().bodyValue(result)
            }
            .doOnError {
                val result = JsonNodeFactory.instance.objectNode()
                result.put("error", "Unable to create map: $it")
                ServerResponse.ok().bodyValue(result)
            }
    }
}
