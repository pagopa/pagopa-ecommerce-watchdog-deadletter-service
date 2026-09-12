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

    fun transition(previousActionType: ActionType.Type?, nextActionType: ActionType.Type): CalendarStats {
        if (previousActionType == nextActionType) {
            return CalendarStats(
                this.date,
                this.finalized.coerceAtLeast(0),
                this.notFinalized.coerceAtLeast(0),
                this.notAnalyzed?.coerceAtLeast(0),
                this.version,
            )
        }

        var finalized = this.finalized
        var notFinalized = this.notFinalized
        var notAnalyzed = this.notAnalyzed

        when (previousActionType to nextActionType) {
            ActionType.Type.NOT_FINAL to ActionType.Type.FINAL -> {
                notFinalized = (notFinalized - 1).coerceAtLeast(0)
                finalized += 1
            }
            ActionType.Type.FINAL to ActionType.Type.NOT_FINAL -> {
                notFinalized += 1
                finalized = (finalized - 1).coerceAtLeast(0)
            }
            null to ActionType.Type.FINAL -> {
                if (notAnalyzed != null) {
                    notAnalyzed = (notAnalyzed - 1).coerceAtLeast(0)
                }
                finalized += 1
            }
            null to ActionType.Type.NOT_FINAL -> {
                if (notAnalyzed != null) {
                    notAnalyzed = (notAnalyzed - 1).coerceAtLeast(0)
                }
                notFinalized += 1
            }
            else -> {}
        }
        return CalendarStats(
            this.date,
            finalized.coerceAtLeast(0),
            notFinalized.coerceAtLeast(0),
            notAnalyzed?.coerceAtLeast(0),
            this.version,
        )
    }
}
