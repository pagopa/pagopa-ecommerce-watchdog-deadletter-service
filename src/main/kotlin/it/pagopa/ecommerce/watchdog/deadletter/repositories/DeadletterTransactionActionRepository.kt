package it.pagopa.ecommerce.watchdog.deadletter.repositories

import it.pagopa.ecommerce.watchdog.deadletter.documents.DeadletterTransactionAction
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface DeadletterTransactionActionRepository :
    ReactiveCrudRepository<DeadletterTransactionAction, String> {

    fun findByTransactionId(transactionId: String): Flux<DeadletterTransactionAction>
}
