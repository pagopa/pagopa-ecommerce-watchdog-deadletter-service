package it.pagopa.ecommerce.watchdog.deadletter.config

import java.security.MessageDigest
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

const val API_KEY_HEADER = "x-api-key"
const val INTERNAL_API_AUTHORITY = "INTERNAL_API"

class ApiKeyAuthenticationToken(val apiKey: String) :
    AbstractAuthenticationToken(AuthorityUtils.createAuthorityList(INTERNAL_API_AUTHORITY)) {
    override fun getCredentials(): Any = apiKey

    override fun getPrincipal(): Any = "api-key-client"
}

/** Extracts the api key from the [API_KEY_HEADER] header. Empty when the header is missing. */
class ApiKeyAuthenticationConverter : ServerAuthenticationConverter {
    override fun convert(exchange: ServerWebExchange): Mono<Authentication> =
        Mono.justOrEmpty(exchange.request.headers.getFirst(API_KEY_HEADER)).map {
            ApiKeyAuthenticationToken(it)
        }
}

class ApiKeyAuthenticationManager(private val validApiKey: String) : ReactiveAuthenticationManager {
    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        val provided = (authentication as ApiKeyAuthenticationToken).apiKey
        val valid =
            validApiKey.isNotBlank() &&
                MessageDigest.isEqual(provided.toByteArray(), validApiKey.toByteArray())
        return if (valid) {
            authentication.isAuthenticated = true
            Mono.just(authentication)
        } else {
            Mono.error(BadCredentialsException("Invalid api key"))
        }
    }
}
