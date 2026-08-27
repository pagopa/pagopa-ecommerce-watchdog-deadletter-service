package it.pagopa.ecommerce.watchdog.deadletter.scheduledjobs

import it.pagopa.ecommerce.watchdog.deadletter.clients.EcommerceHelpdeskServiceClient
import it.pagopa.ecommerce.watchdog.deadletter.documents.CalendarStats
import it.pagopa.ecommerce.watchdog.deadletter.repositories.CalendarStatsRepository
import it.pagopa.generated.ecommerce.helpdesk.model.PageInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchDeadLetterEventResponseDto
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class StatsJobsTest {

    private val calendarStatsRepository: CalendarStatsRepository = mock()
    private val ecommerceHelpdeskServiceClient: EcommerceHelpdeskServiceClient = mock()
    private val statsJobs = StatsJobs(ecommerceHelpdeskServiceClient, calendarStatsRepository)

    @Test
    fun `updatePreviousDayStats should create a new document with NOT_ANALYZED count if it didn't exist`() {
        val singlePageResult =
            SearchDeadLetterEventResponseDto()
                .apply {
                    page =
                        PageInfoDto().apply {
                            results = 10
                            total = 1
                        }
                }
                .let { Mono.just(it) }

        val multiPageResultOne =
            SearchDeadLetterEventResponseDto()
                .apply {
                    page =
                        PageInfoDto().apply {
                            results = 10
                            total = 2
                        }
                }
                .let { Mono.just(it) }

        val multiPageResultTwo =
            SearchDeadLetterEventResponseDto()
                .apply {
                    page =
                        PageInfoDto().apply {
                            results = 4
                            total = 2
                        }
                }
                .let { Mono.just(it) }

        whenever(calendarStatsRepository.findByDate(any<LocalDate>())).thenReturn(Mono.empty())
        whenever(calendarStatsRepository.save(any())).thenAnswer {
            Mono.just(it.getArgument<CalendarStats>(0))
        }
        whenever(
                ecommerceHelpdeskServiceClient.getDeadletterTransactionsByDateRange(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(singlePageResult)
            .thenReturn(multiPageResultOne)
            .thenReturn(multiPageResultTwo)

        StepVerifier.create(statsJobs.updatePreviousDayStats())
            .assertNext { stats ->
                assertEquals(10, stats.notAnalyzed, "The NOT_ANALYZED count is not the same")
                assertEquals(0, stats.notFinalized, "The NOT_FINAL count is not zero")
                assertEquals(0, stats.finalized, "The FINAL count is not zero")
            }
            .verifyComplete()

        StepVerifier.create(statsJobs.updatePreviousDayStats())
            .assertNext { stats ->
                assertEquals(14, stats.notAnalyzed, "The NOT_ANALYZED count is not the same")
                assertEquals(0, stats.notFinalized, "The NOT_FINAL count is not zero")
                assertEquals(0, stats.finalized, "The FINAL count is not zero")
            }
            .verifyComplete()
    }

    @Test
    fun `updatePreviousDayStats should update document with NOT_ANALYZED count if it did exist`() {
        val singlePageResult =
            SearchDeadLetterEventResponseDto()
                .apply {
                    page =
                        PageInfoDto().apply {
                            results = 10
                            total = 1
                        }
                }
                .let { Mono.just(it) }

        val multiPageResultOne =
            SearchDeadLetterEventResponseDto()
                .apply {
                    page =
                        PageInfoDto().apply {
                            results = 10
                            total = 2
                        }
                }
                .let { Mono.just(it) }

        val multiPageResultTwo =
            SearchDeadLetterEventResponseDto()
                .apply {
                    page =
                        PageInfoDto().apply {
                            results = 4
                            total = 2
                        }
                }
                .let { Mono.just(it) }

        whenever(calendarStatsRepository.save(any())).thenAnswer {
            Mono.just(it.getArgument<CalendarStats>(0))
        }
        whenever(calendarStatsRepository.findByDate(any<LocalDate>()))
            .thenReturn(
                Mono.just(
                    CalendarStats(
                        date = LocalDate.now().minusDays(1).toString(),
                        finalized = 5,
                        notFinalized = 13,
                        notAnalyzed = null,
                    )
                )
            )
        whenever(
                ecommerceHelpdeskServiceClient.getDeadletterTransactionsByDateRange(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(singlePageResult)
            .thenReturn(multiPageResultOne)
            .thenReturn(multiPageResultTwo)

        StepVerifier.create(statsJobs.updatePreviousDayStats())
            .assertNext { stats ->
                assertEquals(10, stats.notAnalyzed, "The NOT_ANALYZED count is not the same")
                assertEquals(13, stats.notFinalized, "The NOT_FINAL count is not zero")
                assertEquals(5, stats.finalized, "The FINAL count is not zero")
            }
            .verifyComplete()

        StepVerifier.create(statsJobs.updatePreviousDayStats())
            .assertNext { stats ->
                assertEquals(14, stats.notAnalyzed, "The NOT_ANALYZED count is not the same")
                assertEquals(13, stats.notFinalized, "The NOT_FINAL count is not zero")
                assertEquals(5, stats.finalized, "The FINAL count is not zero")
            }
            .verifyComplete()
    }

    @Test
    fun `updatePreviousDayStats should only log if helpdesk returns an error or there are no transactions`() {
        whenever(calendarStatsRepository.findByDate(any<LocalDate>())).thenReturn(Mono.empty())
        whenever(
                ecommerceHelpdeskServiceClient.getDeadletterTransactionsByDateRange(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            )
            .thenReturn(Mono.error(WebClientResponseException(404, "NOT_FOUND", null, null, null)))
            .thenReturn(Mono.error(RuntimeException()))

        StepVerifier.create(statsJobs.updatePreviousDayStats()).verifyComplete()
        StepVerifier.create(statsJobs.updatePreviousDayStats()).verifyComplete()
    }
}
