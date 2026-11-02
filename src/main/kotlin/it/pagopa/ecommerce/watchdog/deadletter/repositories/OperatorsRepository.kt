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
       (or none was accepted yet). Resets the failed attempts and the lock.
       Returns the number of documents updated (1 = consumed, 0 = replay).
    */
    @Query(
        "{ '_id': ?0, '\$or': [ { 'lastTotpTimeStep': null }, { 'lastTotpTimeStep': { '\$lt': ?1 } } ] }"
    )
    @Update(
        "{ '\$set': { 'lastTotpTimeStep': ?1, 'totpFailedAttempts': 0, 'totpLockedUntil': null } }"
    )
    fun updateLastTotpTimeStepIfNewer(operatorId: String, timeStep: Long): Mono<Long>

    /*
       Atomically reserve an OTP attempt in a SINGLE update (so concurrent requests, also across
       replicas, cannot exceed the limit and an interruption cannot leave the operator half-locked):
       - it matches only if the operator is not locked (no lock, or lock already expired)
       - the counter restarts from 0 when a previous lock has expired, then it is incremented
       - the lock expiry (lockedUntil) is stored together with the maxAttempts-th reservation,
         and cleared otherwise
       Returns 1 if the attempt is allowed, 0 if the operator is locked or does not exist.
    */
    @Query(
        "{ '_id': ?0, '\$or': [ { 'totpLockedUntil': null }, { 'totpLockedUntil': { '\$lte': ?1 } } ] }"
    )
    @Update(
        pipeline =
            [
                "{ '\$set': { 'totpFailedAttempts': { '\$add': [ { '\$cond': [ { '\$ne': [ { '\$ifNull': [ '\$totpLockedUntil', null ] }, null ] }, 0, { '\$ifNull': [ '\$totpFailedAttempts', 0 ] } ] }, 1 ] } } }",
                "{ '\$set': { 'totpLockedUntil': { '\$cond': [ { '\$gte': [ '\$totpFailedAttempts', ?2 ] }, ?3, null ] } } }",
            ]
    )
    fun reserveTotpAttempt(
        operatorId: String,
        now: Instant,
        maxAttempts: Int,
        lockedUntil: Instant,
    ): Mono<Long>
}
