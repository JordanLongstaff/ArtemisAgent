package artemis.agent.setup.settings

import androidx.annotation.IdRes
import androidx.test.espresso.matcher.ViewMatchers.withId
import artemis.agent.R
import artemis.agent.isHidden
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.image.KImageView
import io.github.kakaocup.kakao.text.KTextView

class KTimeInputBinder(@IdRes parentId: Int) {
    private val parentMatcher = withId(parentId)
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

    private val minutesChildren by lazy {
        listOf(minutesUpButton, minutesDownButton, minutesDisplay, colon)
    }

    private val secondsChildren by lazy {
        listOf(
            secondsTenUpButton,
            secondsTenDownButton,
            secondsTenDisplay,
            secondsOneUpButton,
            secondsOneDownButton,
            secondsOneDisplay,
        )
    }

    private val allChildren by lazy { minutesChildren + secondsChildren }

    operator fun invoke(function: KTimeInputBinder.() -> Unit) {
        function(this)
    }

    fun isDisplayed(withMinutes: Boolean) {
        root.isDisplayed()
        minutesChildren.forEach { if (withMinutes) it.isDisplayed() else it.isHidden() }
        secondsChildren.forEach { it.isDisplayed() }
    }

    fun isHidden() {
        root.isHidden()
        allChildren.forEach { it.isHidden() }
    }

    fun doesNotExist() {
        root.doesNotExist()
        allChildren.forEach { it.doesNotExist() }
    }
}
