package artemis.agent.util

import artemis.agent.util.TimerText.timerString
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.duration
import io.kotest.property.arbitrary.nonPositiveLong
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.datetime.Clock

class TimerTextTest :
    DescribeSpec({
        describe("TimerText") {
            fun arbDuration(range: ClosedRange<Duration>): Arb<Duration> =
                Arb.duration(range, DurationUnit.MILLISECONDS)

            val underOneHour = arbDuration(0.seconds..(1.hours - 1.seconds))
            val overOneHour = arbDuration(1.hours..100.hours)

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
                    if (includeHours)
                        "${totalMinutes / minutesPerHour}:${if (minutes < 10) "0" else ""}"
                    else ""

                val secondPadding = if (seconds < 10) "0" else ""
                return "$hoursPrefix$minutes:$secondPadding$seconds"
            }

            val now = Clock.System.now()
            mockkObject(Clock.System)
            every { Clock.System.now() } returns now

            afterSpec { unmockkObject(Clock.System) }

            describe("Arbitrary") {
                listOf("up" to true, "down" to false).forEach { (direction, roundUp) ->
                    describe("Rounding $direction") {
                        listOf(
                                Triple("Under", underOneHour, false),
                                Triple("Over", overOneHour, true),
                            )
                            .forEach { (overUnder, durationArb, includeHours) ->
                                it("$overUnder one hour") {
                                    durationArb.checkAll { duration ->
                                        duration.timerString(roundUp) shouldBeEqual
                                            duration.expectedTimerString(roundUp, includeHours)
                                    }
                                }
                            }
                    }
                }
            }

            describe("Time until") {
                it("Earlier time") {
                    Arb.nonPositiveLong().checkAll { time ->
                        TimerText.getTimeUntil(time) shouldBeEqual "0:00"
                    }
                }

                describe("Later time") {
                    listOf(Triple("Under", underOneHour, false), Triple("Over", overOneHour, true))
                        .forEach { (overUnder, durationArb, includeHours) ->
                            it("$overUnder one hour") {
                                durationArb.checkAll { duration ->
                                    TimerText.getTimeUntil(
                                        (now + duration).toEpochMilliseconds()
                                    ) shouldBeEqual duration.expectedTimerString(true, includeHours)
                                }
                            }
                        }
                }
            }

            describe("Time since") {
                listOf(Triple("Under", underOneHour, false), Triple("Over", overOneHour, true))
                    .forEach { (overUnder, durationArb, includeHours) ->
                        it("$overUnder one hour") {
                            durationArb.checkAll { duration ->
                                TimerText.getTimeSince(
                                    (now - duration).toEpochMilliseconds()
                                ) shouldBeEqual duration.expectedTimerString(false, includeHours)
                            }
                        }
                    }
            }
        }
    })
