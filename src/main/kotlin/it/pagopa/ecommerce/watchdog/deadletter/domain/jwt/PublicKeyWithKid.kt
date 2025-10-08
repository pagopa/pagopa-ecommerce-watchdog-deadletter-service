package it.pagopa.ecommerce.watchdog.deadletter.domain.jwt

import java.io.Serializable
import java.security.PublicKey

data class PublicKeyWithKid(val kid: String, val publicKey: PublicKey) : Serializable
