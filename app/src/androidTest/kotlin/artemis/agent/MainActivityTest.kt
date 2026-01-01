package artemis.agent

import androidx.test.espresso.NoActivityResumedException
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.scenario.ConnectScenario
import artemis.agent.screens.GamePageScreen
import artemis.agent.screens.HelpPageScreen
import artemis.agent.screens.MainScreen
import artemis.agent.screens.MainScreen.mainScreenTest
import artemis.agent.screens.SetupPageScreen
import artemis.agent.setup.ConnectFragmentTest
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest : TestCase() {
    @get:Rule val activityScenarioRule = activityScenarioRule<MainActivity>()

    @Test
    fun changelogTest() {
        run {
            MainScreen {
                step("Accept permissions") { acceptPermissions(device) }

                step("Check that dialog showing changelog is visible") { assertChangelogOpen() }
            }
        }
    }

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

    @Test
    fun exitWarningTest() {
        run {
            mainScreenTest(false) {
                scenario(
                    ConnectScenario(
                        ConnectFragmentTest.FAKE_SERVER_IP,
                        activityScenarioRule.scenario,
                    )
                )

                step("Press back") { pressBack() }

                step("Exit message should be displayed") { assertExitWarningOpen() }

                step("No - app should not close") { alertDialog.negativeButton.click() }

                step("Press back again") { pressBack() }

                step("Exit message should be displayed again") { assertExitWarningOpen() }

                step("Yes - app should close") {
                    try {
                        alertDialog.positiveButton.click()
                        setupPageButton.doesNotExist()
                        Assert.fail("App should have closed")
                    } catch (_: NoActivityResumedException) {
                        // Success
                    }
                }
            }
        }
    }
}
