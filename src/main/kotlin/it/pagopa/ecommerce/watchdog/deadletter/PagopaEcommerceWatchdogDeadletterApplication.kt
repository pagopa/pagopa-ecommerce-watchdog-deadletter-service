package it.pagopa.ecommerce.watchdog.deadletter

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import reactor.core.publisher.Hooks

@SpringBootApplication class WatchdogDeadletterApplication

fun main(args: Array<String>) {
    Hooks.enableAutomaticContextPropagation()
    runApplication<it.pagopa.ecommerce.watchdog.deadletter.WatchdogDeadletterApplication>(*args)
}
