package it.pagopa.ecommerce.watchdog.deadletter.config

import org.openapitools.jackson.nullable.JsonNullableModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {
    @Bean
    fun jsonNullableModule(): JsonNullableModule {
        return JsonNullableModule()
    }
}
