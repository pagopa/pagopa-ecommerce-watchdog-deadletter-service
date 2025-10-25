package it.pagopa.ecommerce.watchdog.deadletter.config.azure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("auth.azure.keyvault")
data class AzureKeyVaultClientConfigProperties(
    val endpoint: String,
    val maxRetries: Int,
    val retryDelayMillis: Long,
)
