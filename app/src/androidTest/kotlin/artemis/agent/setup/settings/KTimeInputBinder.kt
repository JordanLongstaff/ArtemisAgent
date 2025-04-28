package artemis.agent.setup.settings

import androidx.annotation.IdRes
import androidx.test.espresso.matcher.ViewMatchers.withId
import artemis.agent.R
import artemis.agent.isDisplayedIf
import artemis.agent.isHidden
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.text.KTextView

class KTimeInputBinder(@IdRes parentId: Int) {
    private val parentMatcher by lazy { withId(parentId) }
    private val root = KView { withMatcher(parentMatcher) }

    val minutesUpButton = KImageView(parentMatcher) { withId(R.id.minutesUpButton) }
    val minutesDownButton = KImageView(parentMatcher) { withId(R.id.minutesDownButton) }
    val minutesDisplay = KTextView(parentMatcher) { withId(R.id.minutes) }

    val colon = KTextView(parentMatcher) { withId(R.id.colon) }

    val secondsTenUpButton = KImageView(parentMatcher) { withId(R.id.secondsTenUpButton) }
    val secondsTenDownButton = KImageView(parentMatcher) { withId(R.id.secondsTenDownButton) }
    val secondsTenDisplay = KTextView(parentMatcher) { withId(R.id.secondsTen) }

    val secondsOneUpButton = KImageView(parentMatcher) { withId(R.id.secondsOneUpButton) }
    val secondsOneDownButton = KImageView(parentMatcher) { withId(R.id.secondsOneDownButton) }
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
        root {
            isNotDisplayed()
            isInvisible()
        }
        minutesUpButton.isHidden()
        minutesDownButton.isHidden()
        minutesDisplay.isHidden()
        colon.isHidden()
        secondsTenUpButton.isHidden()
        secondsTenDownButton.isHidden()
        secondsTenDisplay.isHidden()
        secondsOneUpButton.isHidden()
        secondsOneDownButton.isHidden()
        secondsOneDisplay.isHidden()
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
