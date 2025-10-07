package it.pagopa.ecommerce.watchdog.deadletter.domain.jwt

import java.io.Serializable
import java.security.PrivateKey

data class PrivateKeyWithKid(val kid: String, val privateKey: PrivateKey) : Serializable