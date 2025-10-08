package it.pagopa.ecommerce.watchdog.deadletter.services.jwt

import it.pagopa.ecommerce.watchdog.deadletter.domain.UserDetails
import it.pagopa.ecommerce.watchdog.deadletter.utils.jwt.JwtUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class JwtService(
    @Autowired private val reactiveAzureKVSecurityKeysService: ReactiveAzureKVSecurityKeysService,
    @Autowired private val jwtUtils: JwtUtils,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun generateUserJwtToken(userDetails: UserDetails): Mono<String> {
        return reactiveAzureKVSecurityKeysService
            .getPrivate()
            .map { jwtUtils.generateJwtToken(userDetails.toMap(), it) }
            .doOnNext { logger.info("User token generated successfully") }
    }
}
