package it.pagopa.ecommerce.watchdog.deadletter.controllers

import it.pagopa.ecommerce.watchdog.deadletter.services.ActionService
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.api.ActionsApi
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ActionTypeDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@Validated
class ActionController(@Autowired val actionService: ActionService) : ActionsApi {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    override fun listActions(
        exchange: ServerWebExchange?
    ): Mono<ResponseEntity<Flux<ActionTypeDto>?>> {
        return Mono.just(ResponseEntity.ok(actionService.getActionType()))
    }
}
