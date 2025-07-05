package artemis.agent

import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import io.github.kakaocup.kakao.check.KCheckBox
import io.github.kakaocup.kakao.common.assertions.BaseAssertions

fun TestContext<*>.testRadioButtons(vararg cases: RadioButtonsTestCase) {
    val iteratedCases = cases.toList() + cases.first()
    iteratedCases.forEachIndexed { i, case ->
        step(if (i == 0) "Initial state" else "Open ${case.name} page") {
            if (i > 0) case.button.click()
            case.testChecked()
            cases.forEachIndexed { j, otherCase ->
                if (i % cases.size != j) otherCase.testUnchecked()
            }
        }
    }
}

data class RadioButtonsTestCase(
    val name: String,
    val button: KCheckBox,
    val testView: BaseAssertions,
) {
    fun testChecked() {
        button.isChecked()
        testView.isCompletelyDisplayed()
    }

    fun testUnchecked() {
        button.isNotChecked()
        testView.doesNotExist()
    }
}
