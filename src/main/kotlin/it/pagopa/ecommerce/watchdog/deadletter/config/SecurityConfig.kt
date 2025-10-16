package it.pagopa.ecommerce.watchdog.deadletter.config

import it.pagopa.ecommerce.watchdog.deadletter.config.jwt.JwtAuthenticationConverter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    @Autowired private val jwtDecoder: ReactiveJwtDecoder,
    @Autowired private val jwtAuthenticationConverter: JwtAuthenticationConverter,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() } // CSRF is not needed with JWT authentication
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers(HttpMethod.POST, "/authenticate")
                    .permitAll()
                    .anyExchange()
                    .authenticated()
            }
            .oauth2ResourceServer { oauth ->
                oauth.jwt { jwt ->
                    jwt.jwtDecoder(jwtDecoder)
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)
                }
            }
            .build()
    }
}
