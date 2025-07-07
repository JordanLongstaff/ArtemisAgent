package artemis.agent.scenario

import com.kaspersky.kaspresso.testcases.api.scenario.Scenario
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import io.github.kakaocup.kakao.check.KCheckBox

class SortMethodPairScenario(sortFirst: KCheckBox, sortSecond: KCheckBox, defaultSort: KCheckBox) :
    Scenario() {
    override val steps: TestContext<Unit>.() -> Unit = {
        listOf(sortFirst to sortSecond, sortSecond to sortFirst).forEach { (first, second) ->
            step("Select first sort method") {
                first {
                    click()
                    isChecked()
                }
            }

            step("Default sort method should not be selected") { defaultSort.isNotChecked() }

            step("Select second sort method") {
                second {
                    click()
                    isChecked()
                }
            }

            step("First sort method should not be selected") { first.isNotChecked() }

            step("Deselect second sort method") {
                second {
                    click()
                    isNotChecked()
                }
            }

            step("Default sort method should be selected") { defaultSort.isChecked() }
        }
    }
}
