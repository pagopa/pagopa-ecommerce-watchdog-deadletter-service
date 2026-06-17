package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.config.ActionTypeConfig
import it.pagopa.ecommerce.watchdog.deadletter.documents.ActionType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class ActionService(@Autowired val actionTypeConfig: ActionTypeConfig) {

    fun getActionType(): Flux<ActionType> {
        return Flux.fromIterable(actionTypeConfig.types)
    }
}
