package it.pagopa.ecommerce.watchdog.deadletter.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@TestConfiguration
class TestSecurityConfig {
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .authorizeExchange { exchanges -> exchanges.anyExchange().permitAll() }
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build()
    }
}
