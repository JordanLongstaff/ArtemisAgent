package artemis.agent.util

import kotlin.math.sign
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

object TimerText {
    fun getTimeSince(startTime: Long): String =
        (System.currentTimeMillis() - startTime).milliseconds.timerString(false)

    fun getTimeUntil(endTime: Long): String =
        maxOf(0L, endTime - System.currentTimeMillis()).milliseconds.timerString(true)

    fun Duration.timerString(roundUp: Boolean): String {
        val rounded = this + if (roundUp) 1.seconds - 1.nanoseconds else Duration.ZERO
        return rounded.toComponents { h, m, s, _ ->
            listOfNotNull(h.toInt().takeIf { it > 0 }, m, s)
                .mapIndexed { index, num -> num.toString().padStart(1 + index.sign, '0') }
                .joinToString(":")
        }
    }
}
