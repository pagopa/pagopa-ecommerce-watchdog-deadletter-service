package it.pagopa.ecommerce.watchdog.deadletter.repositories

import it.pagopa.ecommerce.watchdog.deadletter.documents.Note
import java.time.Instant
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.Update
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface DeadletterTransactionNoteRepository : ReactiveCrudRepository<Note, String> {

    /*
       Count the number of the notes associate to a transactionId
    */
    fun countByTransactionId(transactionId: String): Mono<Long>

    /*
       Get all the notes by a list of transactionId
    */
    fun findAllByTransactionIdIn(transactionIds: List<String>): Flux<Note>

    /*
       Update the note text and the updatedAt field of the target note, return the number of document updated
    */
    @Query("{'_id': ?0 }")
    @Update("{ '\$set': { 'note': ?1, 'updatedAt': ?2 } }")
    fun updateNoteById(noteId: String, noteText: String, updateInstant: Instant): Mono<Long>
}
