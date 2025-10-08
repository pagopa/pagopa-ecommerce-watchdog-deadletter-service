package it.pagopa.ecommerce.watchdog.deadletter.config.jwt

import com.azure.core.http.policy.FixedDelay
import com.azure.core.http.policy.RetryPolicy
import com.azure.identity.DefaultAzureCredentialBuilder
import com.azure.security.keyvault.certificates.CertificateAsyncClient
import com.azure.security.keyvault.certificates.CertificateClientBuilder
import com.azure.security.keyvault.secrets.SecretAsyncClient
import com.azure.security.keyvault.secrets.SecretClientBuilder
import java.time.Duration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AzureConfig(
    @Autowired private val azureKeyVaultClientConfigProperties: AzureKeyVaultClientConfigProperties
) {

    @Bean
    fun azureKeyVaultSecretClient(): SecretAsyncClient {
        return SecretClientBuilder()
            .vaultUrl(azureKeyVaultClientConfigProperties.endpoint)
            .retryPolicy(
                RetryPolicy(
                    FixedDelay(
                        azureKeyVaultClientConfigProperties.maxRetries, // Maximum retries
                        Duration.ofMillis(
                            azureKeyVaultClientConfigProperties.retryDelayMillis
                        ), // Delay between retries in milliseconds
                    )
                )
            )
            .credential(DefaultAzureCredentialBuilder().build())
            .buildAsyncClient()
    }

    @Bean
    fun azureKeyVaultCertClient(): CertificateAsyncClient {
        return CertificateClientBuilder()
            .vaultUrl(azureKeyVaultClientConfigProperties.endpoint)
            .retryPolicy(
                RetryPolicy(
                    FixedDelay(
                        azureKeyVaultClientConfigProperties.maxRetries, // Maximum retries
                        Duration.ofMillis(
                            azureKeyVaultClientConfigProperties.retryDelayMillis
                        ), // Delay between retries in milliseconds
                    )
                )
            )
            .credential(DefaultAzureCredentialBuilder().build())
            .buildAsyncClient()
    }
}
