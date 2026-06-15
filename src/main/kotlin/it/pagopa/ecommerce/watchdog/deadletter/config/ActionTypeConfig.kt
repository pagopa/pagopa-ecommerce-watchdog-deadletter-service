package it.pagopa.ecommerce.watchdog.deadletter.config

import it.pagopa.ecommerce.watchdog.deadletter.documents.ActionType
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "actiontype")
class ActionTypeConfig {

    var types: List<ActionType> = ArrayList()
}
