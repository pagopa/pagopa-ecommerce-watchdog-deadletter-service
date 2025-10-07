package it.pagopa.ecommerce.watchdog.deadletter.config.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("auth.jwt.secret.key")
data class AzureSecretConfigProperties(val name: String, val password: String)