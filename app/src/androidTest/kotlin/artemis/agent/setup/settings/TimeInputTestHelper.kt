package artemis.agent.setup.settings

import androidx.annotation.IdRes
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.internal.matcher.withCompatText
import com.adevinta.android.barista.internal.performAction
import org.hamcrest.Matchers.allOf

data class TimeInputTestHelper(
    @IdRes val parentId: Int,
    var seconds: Int,
    val includeMinutes: Boolean,
) {
    private val parentMatcher by lazy { withParent(withId(parentId)) }

    fun testFully() {
        testTime()
        if (seconds > 0) {
            testDecreaseToZero()
            testIncreaseToMax()
        } else {
            testIncreaseToMax()
            testDecreaseToZero()
        }
    }

    private fun testTime() {
        val remainingSeconds =
            if (includeMinutes) {
                val minutes = seconds / SIXTY
                assertDisplayed(
                    allOf(withId(R.id.minutes), parentMatcher, withCompatText(minutes.toString()))
                )
                assertDisplayed(allOf(withId(R.id.colon), parentMatcher))
                seconds % SIXTY
            } else {
                assertNotDisplayed(allOf(withId(R.id.minutes), parentMatcher))
                assertNotDisplayed(allOf(withId(R.id.colon), parentMatcher))
                seconds
            }

        testSeconds(remainingSeconds)
    }

    private fun testDecreaseToZero() {
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
            testTime()
        }

        repeat(TEN) {
            addOneSecond()
            seconds++
            testTime()
        }

        repeat(if (includeMinutes) 1 else 5) {
            addTenSeconds()
            seconds += TEN
            testTime()
        }

        if (includeMinutes) {
            addOneMinute()
            seconds += SIXTY
            testTime()
        }
    }

    private fun testIncreaseToMax() {
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
            testTime()
        }

        repeat(TEN) {
            subtractOneSecond()
            seconds--
            testTime()
        }

        repeat(if (includeMinutes) 1 else 5) {
            subtractTenSeconds()
            seconds -= TEN
            testTime()
        }

        if (includeMinutes) {
            subtractOneMinute()
            seconds -= SIXTY
            testTime()
        }
    }

    private fun testSeconds(seconds: Int) {
        val tens = seconds / TEN
        val ones = seconds % TEN

        assertDisplayed(
            allOf(withId(R.id.secondsTen), parentMatcher, withCompatText(tens.toString()))
        )
        assertDisplayed(
            allOf(withId(R.id.secondsOne), parentMatcher, withCompatText(ones.toString()))
        )
    }

    private fun clickButton(@IdRes buttonId: Int) {
        allOf(withId(buttonId), parentMatcher).performAction(click())
    }

    private fun addOneMinute() {
        clickButton(R.id.minutesUpButton)
    }

    private fun subtractOneMinute() {
        clickButton(R.id.minutesDownButton)
    }

    private fun addTenSeconds() {
        clickButton(R.id.secondsTenUpButton)
    }

    private fun subtractTenSeconds() {
        clickButton(R.id.secondsTenDownButton)
    }

    private fun addOneSecond() {
        clickButton(R.id.secondsOneUpButton)
    }

    private fun subtractOneSecond() {
        clickButton(R.id.secondsOneDownButton)
    }

    private companion object {
        const val MAX_WITHOUT_MINUTES = 99
        const val MAX_WITH_MINUTES = 599

        const val TEN = 10
        const val SIXTY = 60
    }
}
