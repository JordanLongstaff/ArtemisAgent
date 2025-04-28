package artemis.agent.scenario

import artemis.agent.R
import artemis.agent.isDisplayedWithText
import artemis.agent.isRemoved
import artemis.agent.setup.settings.KTimeInputBinder
import com.kaspersky.kaspresso.testcases.api.scenario.Scenario
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext

class TimeInputTestScenario(
    private val timeInput: KTimeInputBinder,
    private var seconds: Int,
    private val includeMinutes: Boolean,
    private val minimumSeconds: Int = 0,
) : Scenario() {
    override val steps: TestContext<Unit>.() -> Unit = {
        step("Test current time") { testCurrentTime() }

        val initialSeconds = seconds
        if (seconds > minimumSeconds) {
            testDecreaseToMin()
            testIncreaseToMax()
        } else {
            testIncreaseToMax()
            testDecreaseToMin()
        }

        step("Revert to initial time") { revertToTime(initialSeconds) }
    }

    private fun TestContext<*>.testIncreaseToMax() {
        step("Increase to max") {
            val maxSeconds = if (includeMinutes) MAX_WITH_MINUTES else MAX_WITHOUT_MINUTES
            while (seconds < maxSeconds) {
                seconds +=
                    if (includeMinutes) {
                        addOneMinute()
                        SIXTY
                    } else {
                        addTenSeconds()
                        TEN
                    }

                seconds = seconds.coerceAtMost(maxSeconds)
                testCurrentTime()
            }
        }

        step("Decrease one second at a time") { testDecreaseOnes() }

        step("Decrease ten seconds at a time") { testDecreaseTens() }

        if (includeMinutes) {
            step("Decrease one minute") { testDecreaseMinute() }
        }
    }

    private fun TestContext<*>.testDecreaseToMin() {
        step("Decrease to minimum") {
            while (seconds > minimumSeconds) {
                seconds -=
                    if (includeMinutes) {
                        subtractOneMinute()
                        SIXTY
                    } else {
                        subtractTenSeconds()
                        TEN
                    }

                seconds = seconds.coerceAtLeast(minimumSeconds)
                testCurrentTime()
            }
        }

        step("Increase one second at a time") { testIncreaseOnes() }

        step("Increase ten seconds at a time") { testIncreaseTens() }

        if (includeMinutes) {
            step("Increase one minute") { testIncreaseMinute() }
        }
    }

    private fun testCurrentTime() {
        val remainingSeconds =
            if (includeMinutes) {
                val minutes = seconds / SIXTY
                timeInput {
                    minutesDisplay.isDisplayedWithText(minutes.toString())
                    colon.isDisplayedWithText(R.string.colon)
                }
                seconds % SIXTY
            } else {
                timeInput {
                    minutesDisplay.isRemoved()
                    colon.isRemoved()
                }
                seconds
            }

        testSeconds(remainingSeconds)
    }

    private fun testSeconds(seconds: Int) {
        val tens = seconds / TEN
        val ones = seconds % TEN

        timeInput {
            secondsTenDisplay.isDisplayedWithText(tens.toString())
            secondsOneDisplay.isDisplayedWithText(ones.toString())
        }
    }

    private fun testIncreaseMinute() {
        addOneMinute()
        testCurrentTime()
    }

    private fun testDecreaseMinute() {
        subtractOneMinute()
        testCurrentTime()
    }

    private fun testIncreaseTens() {
        testChangingTens(::addTenSeconds)
    }

    private fun testDecreaseTens() {
        testChangingTens(::subtractTenSeconds)
    }

    private fun testIncreaseOnes() {
        testChangingOnes(::addOneSecond)
    }

    private fun testDecreaseOnes() {
        testChangingOnes(::subtractOneSecond)
    }

    private fun addOneMinute() {
        timeInput.minutesUpButton.click()
        seconds += SIXTY
    }

    private fun subtractOneMinute() {
        timeInput.minutesDownButton.click()
        seconds -= SIXTY
    }

    private fun addTenSeconds() {
        timeInput.secondsTenUpButton.click()
        seconds += TEN
    }

    private fun subtractTenSeconds() {
        timeInput.secondsTenDownButton.click()
        seconds -= TEN
    }

    private fun addOneSecond() {
        timeInput.secondsOneUpButton.click()
        seconds++
    }

    private fun subtractOneSecond() {
        timeInput.secondsOneDownButton.click()
        seconds--
    }

    private fun testChangingTens(change: () -> Unit) {
        repeat(if (includeMinutes) 5 else 1) {
            change()
            testCurrentTime()
        }
    }

    private fun testChangingOnes(change: () -> Unit) {
        repeat(TEN - minimumSeconds) {
            change()
            testCurrentTime()
        }
    }

    private fun revertToTime(initialSeconds: Int) {
        if (includeMinutes) {
            while (initialSeconds - seconds >= SIXTY) {
                addOneMinute()
            }
            while (seconds - initialSeconds >= SIXTY) {
                subtractOneMinute()
            }
        }
        while (initialSeconds - seconds >= TEN) {
            addTenSeconds()
        }
        while (seconds - initialSeconds >= TEN) {
            subtractTenSeconds()
        }
        while (initialSeconds > seconds) {
            addOneSecond()
        }
        while (seconds > initialSeconds) {
            subtractOneSecond()
        }
    }

    private companion object {
        const val MAX_WITHOUT_MINUTES = 99
        const val MAX_WITH_MINUTES = 599

        const val TEN = 10
        const val SIXTY = 60
    }
}
