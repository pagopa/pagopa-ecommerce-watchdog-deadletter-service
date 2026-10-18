package it.pagopa.ecommerce.watchdog.deadletter.services

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Provides operators' TOTP secrets from the `AUTH_TOTP_SECRETS` env secret, a JSON object `{
 * "<operatorId>": "<base32 secret>" }`.
 */
@Component
class TotpSecretsProvider(@Value("\${auth.totp.secrets}") secretsJson: String) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val secrets: Map<String, String> =
        try {
            ObjectMapper().readValue(secretsJson, object : TypeReference<Map<String, String>>() {})
        } catch (e: Exception) {
            logger.error("Failed to parse TOTP secrets, error.type={}", e.javaClass.name)
            emptyMap()
        }

    fun getSecret(operatorId: String): Mono<String> = Mono.justOrEmpty(secrets[operatorId])
}
