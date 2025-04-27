package artemis.agent

import androidx.annotation.StringRes
import io.github.kakaocup.kakao.check.CheckableAssertions
import io.github.kakaocup.kakao.common.assertions.BaseAssertions
import io.github.kakaocup.kakao.recycler.RecyclerAdapterAssertions
import io.github.kakaocup.kakao.text.TextViewAssertions

fun CheckableAssertions.isCheckedIf(checked: Boolean) {
    if (checked) isChecked() else isNotChecked()
}

fun BaseAssertions.isDisplayedIf(displayed: Boolean) {
    if (displayed) isDisplayed() else isHidden()
}

fun BaseAssertions.isEnabledIf(enabled: Boolean) {
    if (enabled) isEnabled() else isDisabled()
}

fun BaseAssertions.isHidden() {
    isGone()
    isNotDisplayed()
}

fun TextViewAssertions.isDisplayedWithText(text: String) {
    isDisplayed()
    hasText(text)
}

fun TextViewAssertions.isDisplayedWithText(@StringRes text: Int) {
    isDisplayed()
    hasText(text)
}

fun <A> A.isDisplayedWithSize(size: Int) where A : RecyclerAdapterAssertions, A : BaseAssertions {
    isDisplayed()
    hasSize(size)
}
