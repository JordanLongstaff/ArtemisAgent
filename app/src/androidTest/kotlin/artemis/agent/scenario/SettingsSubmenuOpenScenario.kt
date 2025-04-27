package artemis.agent.scenario

import androidx.annotation.StringRes
import artemis.agent.R
import artemis.agent.screens.SettingsPageScreen
import artemis.agent.screens.SettingsPageScreen.Menu
import com.kaspersky.kaspresso.testcases.api.scenario.Scenario
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext

sealed class SettingsSubmenuOpenScenario
private constructor(
    index: Int,
    @StringRes title: Int,
    usingToggle: Boolean = false,
    toggleDisplayed: Boolean = usingToggle,
) : Scenario() {
    override val steps: TestContext<Unit>.() -> Unit = {
        val name = device.targetContext.getString(title)
        step("Open $name submenu") {
            SettingsPageScreen {
                Menu.settingsPageMenu.childAt<Menu.Entry>(index) {
                    if (usingToggle) toggle.click() else click()
                }

                assertSubmenuDisplayed(index)

                settingsOnOff {
                    if (toggleDisplayed) {
                        isDisplayed()
                        isChecked()
                    } else {
                        isNotDisplayed()
                    }
                }
            }
        }
    }

    data object Client : SettingsSubmenuOpenScenario(0, R.string.settings_menu_client)

    data object Connection : SettingsSubmenuOpenScenario(1, R.string.settings_menu_connection)

    class Missions(usingToggle: Boolean = false) :
        SettingsSubmenuOpenScenario(2, R.string.settings_menu_missions, usingToggle, true)

    class Allies(usingToggle: Boolean = false) :
        SettingsSubmenuOpenScenario(3, R.string.settings_menu_allies, usingToggle, true)

    class Enemies(usingToggle: Boolean = false) :
        SettingsSubmenuOpenScenario(4, R.string.settings_menu_enemies, usingToggle, true)

    class Biomechs(usingToggle: Boolean = false) :
        SettingsSubmenuOpenScenario(5, R.string.settings_menu_biomechs, usingToggle, true)

    class Routing(usingToggle: Boolean = false) :
        SettingsSubmenuOpenScenario(6, R.string.settings_menu_routing, usingToggle, true)

    data object Personal : SettingsSubmenuOpenScenario(7, R.string.settings_menu_personal)
}
