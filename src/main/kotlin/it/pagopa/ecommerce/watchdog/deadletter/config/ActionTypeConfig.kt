package it.pagopa.ecommerce.watchdog.deadletter.config

import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ActionTypeDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.util.ArrayList


@Configuration
@ConfigurationProperties(prefix = "actiontype")
class ActionTypeConfig {

    var types: List<ActionTypeDto> = ArrayList()

}