package artemis.agent.setup.settings

import androidx.annotation.IdRes
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import artemis.agent.R
import artemis.agent.isDisplayedIf
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView

class KTimeInputBinder(@IdRes parentId: Int) {
    private val root = KView { withId(parentId) }
    private val parentMatcher by lazy { withParent(withId(parentId)) }

    val minutesUpButton = KButton(parentMatcher) { withId(R.id.minutesUpButton) }
    val minutesDownButton = KButton(parentMatcher) { withId(R.id.minutesDownButton) }
    val minutesDisplay = KTextView(parentMatcher) { withId(R.id.minutes) }

    val colon = KTextView(parentMatcher) { withId(R.id.colon) }

    val secondsTenUpButton = KButton(parentMatcher) { withId(R.id.secondsTenUpButton) }
    val secondsTenDownButton = KButton(parentMatcher) { withId(R.id.secondsTenDownButton) }
    val secondsTenDisplay = KTextView(parentMatcher) { withId(R.id.secondsTen) }

    val secondsOneUpButton = KButton(parentMatcher) { withId(R.id.secondsOneUpButton) }
    val secondsOneDownButton = KButton(parentMatcher) { withId(R.id.secondsOneDownButton) }
    val secondsOneDisplay = KTextView(parentMatcher) { withId(R.id.secondsOne) }

    operator fun invoke(function: KTimeInputBinder.() -> Unit) = function(this)

    fun isDisplayed(withMinutes: Boolean) {
        root.isDisplayed()
        minutesUpButton.isDisplayedIf(withMinutes)
        minutesDownButton.isDisplayedIf(withMinutes)
        minutesDisplay.isDisplayedIf(withMinutes)
        colon.isDisplayedIf(withMinutes)
        secondsTenUpButton.isDisplayed()
        secondsTenDownButton.isDisplayed()
        secondsTenDisplay.isDisplayed()
        secondsOneUpButton.isDisplayed()
        secondsOneDownButton.isDisplayed()
        secondsOneDisplay.isDisplayed()
    }

    fun isNotDisplayed() {
        root.isNotDisplayed()
        minutesUpButton.doesNotExist()
        minutesDownButton.doesNotExist()
        minutesDisplay.doesNotExist()
        colon.doesNotExist()
        secondsTenUpButton.doesNotExist()
        secondsTenDownButton.doesNotExist()
        secondsTenDisplay.doesNotExist()
        secondsOneUpButton.doesNotExist()
        secondsOneDownButton.doesNotExist()
        secondsOneDisplay.doesNotExist()
    }

    fun doesNotExist() {
        root.doesNotExist()
        minutesUpButton.doesNotExist()
        minutesDownButton.doesNotExist()
        minutesDisplay.doesNotExist()
        colon.doesNotExist()
        secondsTenUpButton.doesNotExist()
        secondsTenDownButton.doesNotExist()
        secondsTenDisplay.doesNotExist()
        secondsOneUpButton.doesNotExist()
        secondsOneDownButton.doesNotExist()
        secondsOneDisplay.doesNotExist()
    }
}
