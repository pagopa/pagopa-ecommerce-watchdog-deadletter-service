package it.pagopa.ecommerce.watchdog.deadletter.repositories

import java.time.Instant
import reactor.core.publisher.Mono

/*
   Interface for create the deleteById if createdAt is greater or equal the limitTime, then return the count of note deleted
*/
interface DeadletterTransactionNoteRepositoryCustom {

    fun deleteByIdAndReturnCountIfRecent(id: String, limitTime: Instant): Mono<Long>
}
