package artemis.agent.game.status

import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.MainActivity
import artemis.agent.isDisplayedWithSize
import artemis.agent.isDisplayedWithText
import artemis.agent.scenario.ConnectScenario
import artemis.agent.screens.GamePageScreen
import artemis.agent.screens.MainScreen.mainScreenTest
import artemis.agent.screens.SelectorPopupScreen
import artemis.agent.screens.SetupPageScreen
import artemis.agent.screens.ShipsPageScreen
import artemis.agent.screens.StatusPageScreen
import artemis.agent.setup.ConnectFragmentTest
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.walkertribe.ian.world.Artemis
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class StatusFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = activityScenarioRule<MainActivity>()

    @Test
    fun statusPageTest() {
        run {
            mainScreenTest(false) {
                scenario(
                    ConnectScenario(
                        ConnectFragmentTest.FAKE_SERVER_IP,
                        activityScenarioRule.scenario,
                    ) {
                        SetupPageScreen.shipsPageButton.isChecked()
                        ShipsPageScreen.shipsList.isDisplayedWithSize(Artemis.SHIP_COUNT)
                    }
                )

                step("Select ship") {
                    ShipsPageScreen.shipsList.childAt<ShipsPageScreen.ShipItem>(0) { click() }
                }

                step("Game page opened") {
                    GamePageScreen {
                        shipNumberLabel.isDisplayedWithText("Ship 1")
                        gamePageSelectorButton.isCompletelyDisplayed()
                    }
                }

                step("Open page selector") { GamePageScreen.gamePageSelectorButton.click() }

                step("Page selector opened") { SelectorPopupScreen.selectorList.isDisplayed() }

                step("Open Status page") {
                    SelectorPopupScreen.selectorList.childAt<SelectorPopupScreen.PageEntry>(0) {
                        click()
                    }
                }

                step("Status report displayed") {
                    StatusPageScreen { statusReportView.isDisplayed() }
                }
            }
        }
    }
}
