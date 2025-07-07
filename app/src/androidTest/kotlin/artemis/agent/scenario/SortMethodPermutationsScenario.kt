package artemis.agent.scenario

import artemis.agent.isCheckedIf
import com.kaspersky.kaspresso.testcases.api.scenario.Scenario
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import io.github.kakaocup.kakao.check.KCheckBox

class SortMethodPermutationsScenario(defaultSort: KCheckBox, vararg sortButtons: KCheckBox) :
    Scenario() {
    override val steps: TestContext<Unit>.() -> Unit = {
        val lastIndex = sortButtons.lastIndex
        sortButtons.forEachIndexed { index, sort ->
            step("Sort method selection #${index + 1}") { sort.click() }

            val isDefault = index == lastIndex
            step("Sort method should ${if (isDefault) "" else "not "}be default") {
                defaultSort.isCheckedIf(isDefault)
            }
        }
    }
}
