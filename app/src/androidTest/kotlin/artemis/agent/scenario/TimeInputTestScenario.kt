package artemis.agent.scenario

import artemis.agent.setup.settings.KTimeInputBinder
import com.kaspersky.kaspresso.testcases.api.scenario.Scenario
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext

class TimeInputTestScenario(
    private val timeInput: KTimeInputBinder,
    private var seconds: Int,
    private val includeMinutes: Boolean,
) : Scenario() {
    override val steps: TestContext<Unit>.() -> Unit = {
        step("Test current time") { testCurrentTime() }

        val initialSeconds = seconds
        if (seconds > 0) {
            testDecreaseToZero()
            testIncreaseToMax()
        } else {
            testIncreaseToMax()
            testDecreaseToZero()
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

    private fun TestContext<*>.testDecreaseToZero() {
        step("Decrease to zero") {
            while (seconds > 0) {
                seconds -=
                    if (includeMinutes) {
                        subtractOneMinute()
                        SIXTY
                    } else {
                        subtractTenSeconds()
                        TEN
                    }

                seconds = seconds.coerceAtLeast(0)
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
                    minutesDisplay {
                        isDisplayed()
                        hasText(minutes.toString())
                    }
                    colon.isDisplayed()
                }
                seconds % SIXTY
            } else {
                timeInput {
                    minutesDisplay.isNotDisplayed()
                    colon.isNotDisplayed()
                }
                seconds
            }

        testSeconds(remainingSeconds)
    }

    private fun testSeconds(seconds: Int) {
        val tens = seconds / TEN
        val ones = seconds % TEN

        timeInput {
            secondsTenDisplay {
                isDisplayed()
                hasText(tens.toString())
            }
            secondsOneDisplay {
                isDisplayed()
                hasText(ones.toString())
            }
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
        repeat(if (includeMinutes) 1 else 5) {
            change()
            testCurrentTime()
        }
    }

    private fun testChangingOnes(change: () -> Unit) {
        repeat(TEN) {
            change()
            testCurrentTime()
        }
    }

    private fun revertToTime(initialSeconds: Int) {
        while (initialSeconds - seconds >= SIXTY) {
            addOneMinute()
        }
        while (seconds - initialSeconds >= SIXTY) {
            subtractOneMinute()
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
