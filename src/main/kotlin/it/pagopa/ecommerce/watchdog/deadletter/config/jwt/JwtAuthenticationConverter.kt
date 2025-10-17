package it.pagopa.ecommerce.watchdog.deadletter.config.jwt

import it.pagopa.ecommerce.watchdog.deadletter.domain.UserDetails
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationConverter : Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    override fun convert(source: Jwt): Mono<AbstractAuthenticationToken>? {
        val claims = source.claims
        val user =
            UserDetails(
                claims["id"] as String,
                claims["name"] as String,
                claims["surname"] as String,
                claims["email"] as String,
            )
        val auth = UsernamePasswordAuthenticationToken(user, source.tokenValue, null)
        return Mono.just(auth)
    }
}
