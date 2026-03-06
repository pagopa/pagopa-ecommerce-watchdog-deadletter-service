package it.pagopa.ecommerce.watchdog.deadletter.repositories

import it.pagopa.ecommerce.watchdog.deadletter.documents.Note
import java.time.Instant
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Mono

class DeadletterTransactionNoteRepositoryCustomImpl(
    private val mongoTemplate: ReactiveMongoTemplate
) : DeadletterTransactionNoteRepositoryCustom {
    /*
       Implementation for the method of deleteByIdAndReturnCount for return the number of document deleted
    */
    override fun deleteByIdAndReturnCountIfRecent(id: String, limitTime: Instant): Mono<Long> {
        val criteria = Criteria.where("_id").`is`(id).and("createdAt").gte(limitTime)
        val query = Query(criteria)
        return mongoTemplate.remove(query, Note::class.java).map { it.deletedCount }
    }
}
