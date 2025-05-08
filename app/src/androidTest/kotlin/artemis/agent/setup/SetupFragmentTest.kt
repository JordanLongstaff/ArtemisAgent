package artemis.agent.setup

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.MainActivity
import artemis.agent.RadioButtonsTestCase
import artemis.agent.screens.ConnectPageScreen
import artemis.agent.screens.MainScreen.mainScreenTest
import artemis.agent.screens.SettingsPageScreen
import artemis.agent.screens.SetupPageScreen
import artemis.agent.screens.ShipsPageScreen
import artemis.agent.testRadioButtons
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SetupFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun radioButtonsTest() {
        run {
            mainScreenTest {
                SetupPageScreen {
                    testRadioButtons(
                        RadioButtonsTestCase(
                            "Connect",
                            connectPageButton,
                            ConnectPageScreen.addressBar,
                        ),
                        RadioButtonsTestCase("Ships", shipsPageButton, ShipsPageScreen.shipsList),
                        RadioButtonsTestCase(
                            "Settings",
                            settingsPageButton,
                            SettingsPageScreen.settingsReset,
                        ),
                    )
                }
            }
        }
    }
}
