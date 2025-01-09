package artemis.agent.util

import artemis.agent.util.TimerText.timerString
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.duration
import io.kotest.property.arbitrary.nonNegativeLong
import io.kotest.property.checkAll
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimerTextTest :
    DescribeSpec({
        describe("TimerText") {
            val tolerance = 1.seconds
            val underOneHour = Arb.duration(0.seconds..(1.hours - tolerance))
            val overOneHour = Arb.duration((1.hours + tolerance)..(100.hours - tolerance))

            val minutesPerHour = 1.hours.inWholeMinutes
            val secondsPerMinute = 1.minutes.inWholeSeconds
            val nanosPerSecond = 1.seconds.inWholeNanoseconds

            fun Duration.expectedTimerString(roundUp: Boolean, includeHours: Boolean): String {
                val totalNanos = inWholeNanoseconds
                val nanos = totalNanos % nanosPerSecond
                val totalSeconds = totalNanos / nanosPerSecond + if (roundUp && nanos > 0) 1 else 0
                val seconds = totalSeconds % secondsPerMinute
                val totalMinutes = totalSeconds / secondsPerMinute
                val minutes = totalMinutes % minutesPerHour

                val hoursPrefix =
                    if (includeHours) {
                        val hours = totalMinutes / minutesPerHour
                        val minutePadding = if (minutes < 10) "0" else ""
                        "$hours:$minutePadding"
                    } else {
                        ""
                    }

                val secondPadding = if (seconds < 10) "0" else ""
                return "$hoursPrefix$minutes:$secondPadding$seconds"
            }

            describe("Arbitrary") {
                listOf("up" to true, "down" to false).forEach { (direction, roundUp) ->
                    describe("Rounding $direction") {
                        listOf(
                                Triple("Under", underOneHour, false),
                                Triple("Over", overOneHour, true),
                            )
                            .forEach {
                                it("${it.first} one hour") {
                                    it.second.checkAll { duration ->
                                        duration.timerString(roundUp) shouldBeEqual
                                            duration.expectedTimerString(roundUp, it.third)
                                    }
                                }
                            }
                    }
                }
            }

            describe("Time until") {
                it("Earlier time") {
                    val currentTime = System.currentTimeMillis()
                    Arb.nonNegativeLong(max = currentTime).checkAll { time ->
                        TimerText.getTimeUntil(time) shouldBeEqual "0:00"
                    }
                }

                describe("Later time") {
                    listOf(Triple("Under", underOneHour, false), Triple("Over", overOneHour, true))
                        .forEach {
                            it("${it.first} one hour") {
                                it.second.checkAll { duration ->
                                    TimerText.getTimeUntil(
                                        System.currentTimeMillis() + duration.inWholeMilliseconds
                                    ) shouldBeEqual duration.expectedTimerString(true, it.third)
                                }
                            }
                        }
                }
            }

            describe("Time since") {
                listOf(Triple("Under", underOneHour, false), Triple("Over", overOneHour, true))
                    .forEach {
                        it("${it.first} one hour") {
                            it.second.checkAll { duration ->
                                TimerText.getTimeSince(
                                    System.currentTimeMillis() - duration.inWholeMilliseconds
                                ) shouldBeEqual duration.expectedTimerString(false, it.third)
                            }
                        }
                    }
            }
        }
    })
