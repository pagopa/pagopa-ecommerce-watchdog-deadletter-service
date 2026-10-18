package it.pagopa.ecommerce.watchdog.deadletter.repositories

import it.pagopa.ecommerce.watchdog.deadletter.documents.Operator
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface OperatorsRepository : ReactiveCrudRepository<Operator, String> {

    /*
       Atomically store the TOTP time step only if it is newer than the last accepted one
       (or none was accepted yet). Returns the number of documents updated (1 = consumed, 0 = replay).
    */
    @Query(
        "{ '_id': ?0, '\$or': [ { 'lastTotpTimeStep': null }, { 'lastTotpTimeStep': { '\$lt': ?1 } } ] }"
    )
    @Update("{ '\$set': { 'lastTotpTimeStep': ?1 } }")
    fun updateLastTotpTimeStepIfNewer(operatorId: String, timeStep: Long): Mono<Long>
}
