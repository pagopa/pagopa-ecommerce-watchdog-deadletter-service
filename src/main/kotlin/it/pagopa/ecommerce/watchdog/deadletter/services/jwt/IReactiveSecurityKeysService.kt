package it.pagopa.ecommerce.watchdog.deadletter.services.jwt

import it.pagopa.ecommerce.watchdog.deadletter.domain.jwt.PrivateKeyWithKid
import it.pagopa.ecommerce.watchdog.deadletter.domain.jwt.PublicKeyWithKid
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IReactiveSecurityKeysService {
    fun getPublic(): Flux<PublicKeyWithKid>

    fun getPrivate(): Mono<PrivateKeyWithKid>
}
