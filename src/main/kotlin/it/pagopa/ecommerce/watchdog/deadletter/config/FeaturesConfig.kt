package it.pagopa.ecommerce.watchdog.deadletter.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "feature")
class FeaturesConfig {
    var postStats: Boolean = false
}
