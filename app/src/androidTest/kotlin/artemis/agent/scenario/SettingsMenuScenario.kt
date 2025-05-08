package artemis.agent.scenario

import artemis.agent.screens.SettingsPageScreen
import artemis.agent.screens.SetupPageScreen
import com.kaspersky.kaspresso.testcases.api.scenario.Scenario
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext

object SettingsMenuScenario : Scenario() {
    override val steps: TestContext<Unit>.() -> Unit = {
        step("Navigate to Settings page") { SetupPageScreen.settingsPageButton.click() }

        step("Initial state: menu") { SettingsPageScreen.assertMainMenuDisplayed() }
    }
}
