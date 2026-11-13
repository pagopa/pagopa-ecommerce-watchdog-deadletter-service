package it.pagopa.ecommerce.watchdog.deadletter.config

import it.pagopa.ecommerce.watchdog.deadletter.config.jwt.JwtAuthenticationConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

const val INTERNAL_API_PATH_PATTERN = "/*/internal/**"

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    @Autowired private val jwtDecoder: ReactiveJwtDecoder,
    @Autowired private val jwtAuthenticationConverter: JwtAuthenticationConverter,
    @Value("\${auth.internal.apiKey}") private val internalApiKey: String,
) {

    // api key authentication is evaluated only on internal APIs (e.g. /v2/internal/**)
    private fun apiKeyAuthenticationFilter(): AuthenticationWebFilter {
        val filter = AuthenticationWebFilter(ApiKeyAuthenticationManager(internalApiKey))
        filter.setServerAuthenticationConverter(ApiKeyAuthenticationConverter())
        filter.setRequiresAuthenticationMatcher(
            ServerWebExchangeMatchers.pathMatchers(INTERNAL_API_PATH_PATTERN)
        )
        return filter
    }

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .addFilterAt(apiKeyAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .csrf { it.disable() } // CSRF is not needed with JWT authentication
            .cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/authenticate")
                    .permitAll()
                    .pathMatchers("/actuator/**")
                    .permitAll()
                    .pathMatchers(INTERNAL_API_PATH_PATTERN)
                    .hasAuthority(INTERNAL_API_AUTHORITY)
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

    @Bean
    fun corsConfigurationSource(): UrlBasedCorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("http://localhost:3000")
        configuration.allowedMethods = listOf("GET", "POST", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
