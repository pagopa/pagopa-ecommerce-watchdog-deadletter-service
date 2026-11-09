package it.pagopa.ecommerce.watchdog.deadletter.repositories

import it.pagopa.ecommerce.watchdog.deadletter.documents.Action
import java.time.Instant
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface DeadletterTransactionActionRepository : ReactiveCrudRepository<Action, String> {

    fun findByTransactionId(transactionId: String): Flux<Action>

    fun findAllByTransactionIdIn(transactionIds: List<String>): Flux<Action>

    fun findFirstByTransactionIdOrderByTimestampDesc(transactionId: String): Mono<Action>

    /** Finds the action that immediately precedes the given one in the transaction history. */
    fun findFirstByTransactionIdAndIdNotAndTimestampLessThanOrderByTimestampDesc(
        transactionId: String,
        id: String,
        timestamp: Instant,
    ): Mono<Action>
}
