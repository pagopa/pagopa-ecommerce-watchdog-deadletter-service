package it.pagopa.ecommerce.watchdog.deadletter.scheduledjobs

import it.pagopa.ecommerce.watchdog.deadletter.clients.EcommerceHelpdeskServiceClient
import it.pagopa.ecommerce.watchdog.deadletter.documents.CalendarStats
import it.pagopa.ecommerce.watchdog.deadletter.repositories.CalendarStatsRepository
import it.pagopa.generated.ecommerce.helpdesk.model.PageInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchDeadLetterEventResponseDto
import java.time.LocalDate
import java.time.ZoneOffset
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

@Service
class StatsJobs(
    private val ecommerceHelpdeskServiceV1: EcommerceHelpdeskServiceClient,
    private val calendarStatsRepository: CalendarStatsRepository,
) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    @Scheduled(cron = "\${scheduled-job.stats.cron}")
    fun updatePreviousDayStats(): Mono<CalendarStats> {
        val pageSize = 10
        val yesterdayMono = Mono.fromCallable { LocalDate.now(ZoneOffset.UTC).minusDays(1) }
        return yesterdayMono.flatMap { yesterday ->
            val stats =
                calendarStatsRepository
                    .findByDate(yesterday)
                    .doOnNext { logger.info("Found already existing stats for $yesterday") }
                    .switchIfEmpty {
                        logger.info("No stats found for $yesterday")
                        CalendarStats.createFrom(date = yesterday).toMono()
                    }

            ecommerceHelpdeskServiceV1
                .getDeadletterTransactionsByDateRange(yesterday, yesterday, pageSize, 0)
                .onErrorResume { error ->
                    when (error) {
                        is WebClientResponseException.NotFound -> {
                            logger.info("No deadletter transactions found for date {}", yesterday)
                            Mono.just(
                                SearchDeadLetterEventResponseDto().apply {
                                    page =
                                        PageInfoDto().apply {
                                            current = 0
                                            results = 0
                                            total = 0
                                        }
                                    deadLetterEvents = listOf()
                                }
                            )
                        }

                        else -> {
                            logger.error(
                                "Error retrieving deadletter transactions for date {}: [{}]",
                                yesterday,
                                error.message,
                                error,
                            )
                            Mono.empty()
                        }
                    }
                }
                .flatMap {
                    if (it.page.total <= 1) {
                        stats
                            .map { s -> s.copy(notAnalyzed = it.page.results) }
                            .flatMap { s -> calendarStatsRepository.save(s) }
                            .doOnNext { logger.info("Updated $yesterday stats (single page)") }
                    } else {
                        val total =
                            ecommerceHelpdeskServiceV1
                                .getDeadletterTransactionsByDateRange(
                                    yesterday,
                                    yesterday,
                                    pageSize,
                                    it.page.total - 1,
                                )
                                .map { p -> (pageSize * (it.page.total - 1)) + p.page.results }

                        Mono.zip(stats, total)
                            .map { (s, t) -> s.copy(notAnalyzed = t) }
                            .flatMap { s -> calendarStatsRepository.save(s) }
                            .doOnNext { logger.info("Updated $yesterday stats (multiple pages)") }
                    }
                }
        }
    }
}
