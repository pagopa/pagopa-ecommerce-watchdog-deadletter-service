package it.pagopa.ecommerce.watchdog.deadletter.repositories

import it.pagopa.ecommerce.watchdog.deadletter.documents.DayStats
import java.time.YearMonth
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface DayStatsRepository : ReactiveCrudRepository<DayStats, String> {

    @Query("{'admission_date': {\$gte: ?0, \$lte: ?1}}")
    fun getBetweenDates(from: String, to: String): Flux<DayStats>

    fun getBetweenDates(year: Int, month: Int): Flux<DayStats> {
        val from = YearMonth.of(year, month).atDay(1)
        val to = YearMonth.of(year, month).atEndOfMonth()
        return getBetweenDates(from.toString(), to.toString())
    }
}
