package it.pagopa.ecommerce.watchdog.deadletter.repositories

import it.pagopa.ecommerce.watchdog.deadletter.documents.CalendarStats
import java.time.LocalDate
import java.time.YearMonth
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface CalendarStatsRepository : ReactiveCrudRepository<CalendarStats, String> {

    fun findByDate(date: String): Mono<CalendarStats>

    fun findByDate(date: LocalDate): Mono<CalendarStats> = findByDate(date.toString())

    @Query("{_id: {\$gte: ?0, \$lte: ?1}}")
    fun getBetweenDates(from: String, to: String): Flux<CalendarStats>

    fun getBetweenDates(year: Int, month: Int): Flux<CalendarStats> {
        val from = YearMonth.of(year, month).atDay(1)
        val to = YearMonth.of(year, month).atEndOfMonth()
        return getBetweenDates(from.toString(), to.toString())
    }
}
