package it.pagopa.ecommerce.watchdog.deadletter.repositories

import java.time.Instant
import reactor.core.publisher.Mono

/*
   Interface for create the deleteById if createdAt is greater or equal the limitTime and the user is the same, then return the count of note deleted
*/
interface DeadletterTransactionNoteRepositoryCustom {

    fun deleteByIdAndReturnCountIfRecent(id: String, limitTime: Instant, userId: String): Mono<Long>
}
