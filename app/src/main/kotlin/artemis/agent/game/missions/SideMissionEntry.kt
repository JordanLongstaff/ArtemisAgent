package artemis.agent.game.missions

import artemis.agent.AgentViewModel
import artemis.agent.game.ObjectEntry

class SideMissionEntry(
    val source: ObjectEntry<*>,
    val destination: ObjectEntry<*>,
    payout: RewardType,
    val timestamp: Long
) {
    val rewards = IntArray(RewardType.entries.size).also {
        it[payout.ordinal] = 1
    }

    var associatedShipName: String = ""
    val isStarted: Boolean get() = associatedShipName.isNotEmpty()

    var completionTimestamp = Long.MAX_VALUE
    val isCompleted: Boolean get() = completionTimestamp != Long.MAX_VALUE

    val durationText: String get() {
        val duration = System.currentTimeMillis() - timestamp
        val totalSeconds = duration / AgentViewModel.SECONDS_TO_MILLIS
        val (totalMinutes, seconds) = AgentViewModel.getTimer(totalSeconds.toInt())
        val secondsString = seconds.toString().padStart(2, '0')
        val (hours, minutes) = AgentViewModel.getTimer(totalMinutes)

        return if (hours > 0) {
            "$hours:${minutes.toString().padStart(2, '0')}:$secondsString"
        } else {
            "$minutes:$secondsString"
        }
    }

    override fun hashCode(): Int = arrayOf(source, destination).contentHashCode()

    override fun equals(other: Any?): Boolean =
        other is SideMissionEntry && other.timestamp == timestamp
}
