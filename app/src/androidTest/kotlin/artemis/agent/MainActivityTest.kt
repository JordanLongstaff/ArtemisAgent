package artemis.agent

import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.screens.GamePageScreen
import artemis.agent.screens.HelpPageScreen
import artemis.agent.screens.MainScreen.mainScreenTest
import artemis.agent.screens.SetupPageScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest : TestCase() {
    @get:Rule val activityScenarioRule = activityScenarioRule<MainActivity>()

    @Test
    fun radioButtonsTest() {
        run {
            mainScreenTest {
                testRadioButtons(
                    RadioButtonsTestCase(
                        "Setup",
                        setupPageButton,
                        SetupPageScreen.connectPageButton,
                    ),
                    RadioButtonsTestCase("Game", gamePageButton, GamePageScreen.shipNumberLabel),
                    RadioButtonsTestCase("Help", helpPageButton, HelpPageScreen.helpTopicContent),
                )
            }
        }
    }
}
