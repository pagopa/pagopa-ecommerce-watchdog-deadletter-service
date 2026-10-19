package it.pagopa.ecommerce.watchdog.deadletter.repositories

import it.pagopa.ecommerce.watchdog.deadletter.documents.Operator
import java.time.Instant
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
    @Update("{ '\$set': { 'lastTotpTimeStep': ?1, 'totpFailedAttempts': 0 } }")
    fun updateLastTotpTimeStepIfNewer(operatorId: String, timeStep: Long): Mono<Long>

    /*
       Atomically reserve an OTP attempt: increments the attempts counter only if the operator is not
       locked and has not reached maxAttempts. Returns 1 if the attempt is allowed, 0 otherwise.
       Being a single conditional update, concurrent requests (also across replicas) cannot exceed the limit.
    */
    @Query(
        "{ '_id': ?0, '\$and': [ " +
            "{ '\$or': [ { 'totpLockedUntil': null }, { 'totpLockedUntil': { '\$lte': ?1 } } ] }, " +
            "{ '\$or': [ { 'totpFailedAttempts': null }, { 'totpFailedAttempts': { '\$lt': ?2 } } ] } " +
            "] }"
    )
    @Update("{ '\$inc': { 'totpFailedAttempts': 1 } }")
    fun reserveTotpAttempt(operatorId: String, now: Instant, maxAttempts: Int): Mono<Long>

    /*
       Lock the operator until lockedUntil when the failed attempts reached maxAttempts,
       resetting the counter. Returns the number of documents updated (1 = locked now).
    */
    @Query("{ '_id': ?0, 'totpFailedAttempts': { '\$gte': ?1 } }")
    @Update("{ '\$set': { 'totpLockedUntil': ?2, 'totpFailedAttempts': 0 } }")
    fun lockTotpIfTooManyAttempts(
        operatorId: String,
        maxAttempts: Int,
        lockedUntil: Instant,
    ): Mono<Long>
}
