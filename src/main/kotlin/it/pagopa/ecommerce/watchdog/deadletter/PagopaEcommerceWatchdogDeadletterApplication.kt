package it.pagopa.ecommerce.watchdog.deadletter

import it.pagopa.ecommerce.watchdog.deadletter.config.azure.AzureKeyVaultClientConfigProperties
import it.pagopa.ecommerce.watchdog.deadletter.config.azure.AzureSecretConfigProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import reactor.core.publisher.Hooks

@EnableScheduling
@EnableConfigurationProperties(
    AzureSecretConfigProperties::class,
    AzureKeyVaultClientConfigProperties::class,
)
@SpringBootApplication
class WatchdogDeadletterApplication

fun main(args: Array<String>) {
    Hooks.enableAutomaticContextPropagation()
    runApplication<it.pagopa.ecommerce.watchdog.deadletter.WatchdogDeadletterApplication>(*args)
}
