package artemis.agent

import androidx.annotation.StringRes
import io.github.kakaocup.kakao.check.CheckableAssertions
import io.github.kakaocup.kakao.common.assertions.BaseAssertions
import io.github.kakaocup.kakao.recycler.RecyclerAdapterAssertions
import io.github.kakaocup.kakao.text.TextViewAssertions

private const val MAX_DISTANCE_DIGITS = 7

fun CheckableAssertions.isCheckedIf(checked: Boolean) {
    if (checked) isChecked() else isNotChecked()
}

fun BaseAssertions.isEnabledIf(enabled: Boolean) {
    if (enabled) isEnabled() else isDisabled()
}

fun BaseAssertions.isHidden() {
    isInvisible()
    isNotDisplayed()
}

fun BaseAssertions.isRemoved() {
    isGone()
    isNotDisplayed()
}

fun TextViewAssertions.isDisplayedWithText(text: String) {
    isCompletelyDisplayed()
    hasText(text)
}

fun TextViewAssertions.isDisplayedWithText(@StringRes text: Int) {
    isCompletelyDisplayed()
    hasText(text)
}

fun TextViewAssertions.showsFormattedDistance(distance: Float) {
    hasText(distance.toString().take(MAX_DISTANCE_DIGITS))
}

fun <A> A.isDisplayedWithSize(size: Int) where A : RecyclerAdapterAssertions, A : BaseAssertions {
    isCompletelyDisplayed()
    hasSize(size)
}
