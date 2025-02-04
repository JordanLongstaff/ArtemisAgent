package artemis.agent.util

import kotlin.math.sign
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

object TimerText {
    fun getTimeSince(startTime: Long): String =
        (Clock.System.now() - Instant.fromEpochMilliseconds(startTime)).timerString(false)

    fun getTimeUntil(endTime: Long): String =
        maxOf(Duration.ZERO, Instant.fromEpochMilliseconds(endTime) - Clock.System.now())
            .timerString(true)

    fun Duration.timerString(roundUp: Boolean): String {
        val rounded = this + if (roundUp) 1.seconds - 1.nanoseconds else Duration.ZERO
        return rounded.toComponents { h, m, s, _ ->
            listOfNotNull(h.toInt().takeIf { it > 0 }, m, s)
                .mapIndexed { index, num -> num.toString().padStart(1 + index.sign, '0') }
                .joinToString(":")
        }
    }
}
