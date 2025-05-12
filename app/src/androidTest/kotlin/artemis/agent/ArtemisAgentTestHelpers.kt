package artemis.agent

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.viewmodel.testing.viewModelScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.kakaocup.kakao.check.CheckableAssertions
import io.github.kakaocup.kakao.common.assertions.BaseAssertions
import io.github.kakaocup.kakao.recycler.RecyclerAdapterAssertions
import io.github.kakaocup.kakao.text.TextViewAssertions

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

inline fun withViewModel(crossinline test: (AgentViewModel) -> Unit) {
    viewModelScenario {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val application = checkNotNull(context.applicationContext) as Application
            AgentViewModel(application)
        }
        .use { test(it.viewModel) }
}
