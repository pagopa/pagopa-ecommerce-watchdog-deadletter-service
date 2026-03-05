package it.pagopa.ecommerce.watchdog.deadletter.repositories

import reactor.core.publisher.Mono

/*
   Interface for create the deleteById that return the count of note deleted
*/
interface DeadletterTransactionNoteRepositoryCustom {

    fun deleteByIdAndReturnCount(id: String): Mono<Long>
}
