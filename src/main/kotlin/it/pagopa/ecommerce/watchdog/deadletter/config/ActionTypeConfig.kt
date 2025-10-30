package it.pagopa.ecommerce.watchdog.deadletter.config

import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ActionTypeDto
import java.util.ArrayList
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "actiontype")
class ActionTypeConfig {

    var types: List<ActionTypeDto> = ArrayList()
}
