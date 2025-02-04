package artemis.agent.game.missions

import artemis.agent.game.ObjectEntry
import artemis.agent.util.TimerText

class SideMissionEntry(
    val source: ObjectEntry<*>,
    val destination: ObjectEntry<*>,
    payout: RewardType,
    val timestamp: Long,
) {
    val rewards = IntArray(RewardType.entries.size).also { it[payout.ordinal] = 1 }

    var associatedShipName: String = ""
    val isStarted: Boolean
        get() = associatedShipName.isNotEmpty()

    var completionTimestamp = Long.MAX_VALUE
    val isCompleted: Boolean
        get() = completionTimestamp != Long.MAX_VALUE

    val durationText: String
        get() = TimerText.getTimeSince(timestamp)

    override fun hashCode(): Int = timestamp.hashCode()

    override fun equals(other: Any?): Boolean =
        other is SideMissionEntry && other.timestamp == timestamp
}
