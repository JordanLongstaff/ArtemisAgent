package artemis.agent.scenario

import com.kaspersky.kaspresso.testcases.api.scenario.Scenario
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import io.github.kakaocup.kakao.check.KCheckBox

class SortMethodSingleScenario(sortButton: KCheckBox, defaultSortButton: KCheckBox) : Scenario() {
    override val steps: TestContext<Unit>.() -> Unit = {
        step("Select sort method") {
            sortButton {
                click()
                isChecked()
            }
        }

        step("Sort method should not be default") { defaultSortButton.isNotChecked() }

        step("Deselect sort method") {
            sortButton {
                click()
                isNotChecked()
            }
        }

        step("Sort method should be default") { defaultSortButton.isChecked() }
    }
}
