package artemis.agent.scenario

import artemis.agent.isCheckedIf
import artemis.agent.isEnabledIf
import com.kaspersky.kaspresso.testcases.api.scenario.Scenario
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import io.github.kakaocup.kakao.check.KCheckBox

class AllAndNoneSettingsScenario(
    allButton: KCheckBox,
    noneButton: KCheckBox,
    settingsButtons: List<Pair<KCheckBox, Boolean>>,
    shouldTest: Boolean,
    ifEnabled: (TestContext<Unit>.(Int, Boolean) -> Unit)? = null,
) : Scenario() {
    override val steps: TestContext<Unit>.() -> Unit = {
        val anyEnabled = settingsButtons.any { it.second }
        val allEnabled = settingsButtons.all { it.second }

        step("All button initial state") { allButton.isEnabledIf(!allEnabled) }
        step("None button initial state") { noneButton.isEnabledIf(anyEnabled) }

        step("Initial states of setting buttons") {
            settingsButtons.forEachIndexed { index, (button, isChecked) ->
                step("Setting button #${index + 1}") { button.isCheckedIf(isChecked) }
            }
        }

        if (shouldTest) {
            val allPair = allButton to "All"
            val nonePair = noneButton to "None"
            listOf(Triple(allPair, nonePair, true), Triple(nonePair, allPair, false))
                .let { if (allEnabled) it.reversed() else it }
                .forEach { (clicked, other, checked) ->
                    val (clickedButton, clickedName) = clicked
                    val (otherButton, otherName) = other

                    step("Click $clickedName button") { clickedButton.click() }
                    step("Setting buttons should all be ${if (checked) "" else "un"}checked") {
                        settingsButtons.forEachIndexed { index, (button, _) ->
                            button.isCheckedIf(checked)
                            ifEnabled?.invoke(this, index, checked)
                        }
                    }
                    step("$otherName button should be enabled") { otherButton.isEnabled() }
                    step("$clickedName button should be disabled") { clickedButton.isDisabled() }

                    settingsButtons.forEachIndexed { index, (button, _) ->
                        booleanArrayOf(true, false).forEach { on ->
                            step("Click setting button #${index + 1}") {
                                button {
                                    click()
                                    isCheckedIf(checked != on)
                                }
                            }

                            step("$otherName button should still be enabled") {
                                otherButton.isEnabled()
                            }

                            step("All button should now be ${if (on) "en" else "dis"}abled") {
                                clickedButton.isEnabledIf(on)
                            }
                        }
                    }
                }

            if (anyEnabled && !allEnabled) {
                step("Revert all settings") {
                    settingsButtons.filter { it.second }.forEach { (button) -> button.click() }
                }

                step("All button should still be enabled") { allButton.isEnabled() }

                step("None button should be enabled again") { noneButton.isEnabled() }
            }
        }
    }
}
