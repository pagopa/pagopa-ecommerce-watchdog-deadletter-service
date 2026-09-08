package it.pagopa.ecommerce.watchdog.deadletter.documents

import java.time.LocalDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.Sharded

@Document(collection = "calendar_stats")
@Sharded(shardKey = ["date"])
data class CalendarStats(
    @Id @Field("_id") val date: String,
    val finalized: Int,
    val notFinalized: Int,
    val notAnalyzed: Int?,
    @Version val version: Long? = null,
) {
    companion object {
        fun createFrom(
            action: ActionType? = null,
            date: LocalDate = LocalDate.now(),
        ): CalendarStats {
            var finalized = 0
            var notFinalized = 0

            when (action?.type) {
                ActionType.Type.FINAL -> finalized += 1
                ActionType.Type.NOT_FINAL -> notFinalized += 1
                null -> {
                    /* No action, just a bare initialization */
                }
            }

            return CalendarStats(date.toString(), finalized, notFinalized, null, null)
        }
    }

    fun transition(old: ActionType.Type?, new: ActionType.Type): CalendarStats {
        var finalized = this.finalized
        var notFinalized = this.notFinalized
        var notAnalyzed = this.notAnalyzed

        when (old to new) {
            ActionType.Type.NOT_FINAL to ActionType.Type.FINAL -> {
                notFinalized -= 1
                finalized += 1
            }
            ActionType.Type.FINAL to ActionType.Type.NOT_FINAL -> {
                notFinalized += 1
                finalized -= 1
            }
            null to ActionType.Type.FINAL -> {
                notAnalyzed = notAnalyzed?.minus(1)
                finalized += 1
            }
            null to ActionType.Type.NOT_FINAL -> {
                notAnalyzed = notAnalyzed?.minus(1)
                notFinalized += 1
            }
        }
        return CalendarStats(this.date, finalized, notFinalized, notAnalyzed, this.version)
    }
}
