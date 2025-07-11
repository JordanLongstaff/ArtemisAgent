package artemis.agent.scenario

import artemis.agent.isRemoved
import artemis.agent.screens.SettingsPageScreen
import artemis.agent.screens.SettingsPageScreen.Menu
import com.kaspersky.kaspresso.testcases.api.scenario.Scenario
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext

sealed class SettingsSubmenuOpenScenario(
    page: SettingsPageScreen.Page,
    usingToggle: Boolean = false,
) : Scenario() {
    override val steps: TestContext<Unit>.() -> Unit = {
        val name = device.targetContext.getString(page.title)
        step("Open $name submenu") {
            SettingsPageScreen {
                Menu.settingsPageMenu.childAt<Menu.Entry>(page.ordinal) {
                    if (usingToggle) toggle.click() else click()
                }

                assertSubmenuDisplayed(page)

                settingsOnOff {
                    if (page.toggleDisplayed) {
                        isCompletelyDisplayed()
                        isChecked()
                    } else {
                        isRemoved()
                    }
                }
            }
        }
    }

    data object Client : SettingsSubmenuOpenScenario(SettingsPageScreen.Page.CLIENT)

    data object Connection : SettingsSubmenuOpenScenario(SettingsPageScreen.Page.CONNECTION)

    class Missions(usingToggle: Boolean = false) :
        SettingsSubmenuOpenScenario(SettingsPageScreen.Page.MISSIONS, usingToggle)

    class Allies(usingToggle: Boolean = false) :
        SettingsSubmenuOpenScenario(SettingsPageScreen.Page.ALLIES, usingToggle)

    class Enemies(usingToggle: Boolean = false) :
        SettingsSubmenuOpenScenario(SettingsPageScreen.Page.ENEMIES, usingToggle)

    class Biomechs(usingToggle: Boolean = false) :
        SettingsSubmenuOpenScenario(SettingsPageScreen.Page.BIOMECHS, usingToggle)

    class Routing(usingToggle: Boolean = false) :
        SettingsSubmenuOpenScenario(SettingsPageScreen.Page.ROUTING, usingToggle)

    data object Personal : SettingsSubmenuOpenScenario(SettingsPageScreen.Page.PERSONAL)
}
