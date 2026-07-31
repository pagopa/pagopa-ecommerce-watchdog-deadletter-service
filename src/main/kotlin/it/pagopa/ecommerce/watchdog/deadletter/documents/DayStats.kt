package it.pagopa.ecommerce.watchdog.deadletter.documents

import java.time.LocalDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "notes")
data class DayStats(
    @Id val date: String,
    val finalized: Int,
    val notFinalized: Int,
    val notAnalyzed: Int?,
    @Version val version: Int,
) {
    companion object {
        fun createFrom(action: ActionType, date: LocalDate = LocalDate.now()): DayStats {
            var finalized = 0
            var notFinalized = 0

            when (action.type) {
                ActionType.Type.FINAL -> finalized += 1
                ActionType.Type.NOT_FINAL -> notFinalized += 1
            }

            return DayStats(date.toString(), finalized, notFinalized, null, 0)
        }
    }

    fun transition(old: ActionType.Type?, new: ActionType.Type): DayStats {
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
        return DayStats(this.date, finalized, notFinalized, notAnalyzed, this.version)
    }
}
