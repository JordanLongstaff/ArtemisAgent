package artemis.agent.setup.settings

import androidx.annotation.StringRes
import artemis.agent.isCheckedIf
import artemis.agent.isDisplayedWithText
import artemis.agent.isRemoved
import io.github.kakaocup.kakao.check.KCheckBox
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KTextView

data class SingleToggleButtonSetting(
    val divider: KView,
    val label: KTextView,
    @StringRes val text: Int,
    val button: KCheckBox,
) {
    fun testSingleToggle(isChecked: Boolean) {
        divider.scrollTo()
        label.isDisplayedWithText(text)
        button {
            isDisplayed()
            isCheckedIf(isChecked)
        }
    }

    fun testHidden() {
        label.isRemoved()
        button.isRemoved()
    }

    fun testNotExist() {
        label.doesNotExist()
        button.doesNotExist()
        divider.doesNotExist()
    }
}
