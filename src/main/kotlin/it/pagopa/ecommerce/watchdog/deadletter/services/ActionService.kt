package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.config.ActionTypeConfig
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ActionTypeDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class ActionService(@Autowired val actionTypeConfig: ActionTypeConfig) {

    fun getActionType(): Flux<ActionTypeDto> {
        return Flux.fromIterable(actionTypeConfig.types)
    }
}
