package artemis.agent.scenario

import artemis.agent.isCheckedIf
import com.kaspersky.kaspresso.testcases.api.scenario.Scenario
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import io.github.kakaocup.kakao.check.KCheckBox

class SortMethodDependentScenario(
    dependentSort: Pair<KCheckBox, String>,
    independentSort: Pair<KCheckBox, String>,
    defaultSort: KCheckBox,
) : Scenario() {
    override val steps: TestContext<Unit>.() -> Unit = {
        listOf(dependentSort to true, independentSort to false).forEach {
            (firstSort, dependentFirst) ->
            val (firstButton, firstName) = firstSort
            step("Select sort by $firstName") { firstButton.click() }

            step("Sort by ${independentSort.second} should be active") {
                independentSort.first.isChecked()
            }

            step(
                "Sort by ${dependentSort.second} should be ${if (dependentFirst) "" else "in"}active"
            ) {
                dependentSort.first.isCheckedIf(dependentFirst)
            }

            step("Default sort should be inactive") { defaultSort.isNotChecked() }

            step("${if (dependentFirst) "Des" else "S"}elect sort by ${dependentSort.second}") {
                dependentSort.first.click()
            }

            step("Sort by ${independentSort.second} should still be active") {
                independentSort.first.isChecked()
            }

            step(
                "Sort by ${dependentSort.second} should now be ${if (dependentFirst) "in" else ""}active"
            ) {
                dependentSort.first.isCheckedIf(!dependentFirst)
            }

            step("Default sort should still be inactive") { defaultSort.isNotChecked() }

            step("Deselect sort by ${independentSort.second}") { independentSort.first.click() }

            step("Sort by ${independentSort.second} should now be inactive") {
                independentSort.first.isNotChecked()
            }

            step("Sort by ${dependentSort.second} should still be inactive") {
                dependentSort.first.isNotChecked()
            }

            step("Default sort should now be active") { defaultSort.isChecked() }
        }
    }
}
