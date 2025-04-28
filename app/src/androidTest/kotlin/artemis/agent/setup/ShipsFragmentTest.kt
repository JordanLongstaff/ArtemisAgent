package artemis.agent.setup

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.isDisplayedWithSize
import artemis.agent.isDisplayedWithText
import artemis.agent.isRemoved
import artemis.agent.scenario.ConnectScenario
import artemis.agent.screens.GamePageScreen
import artemis.agent.screens.MainScreen.mainScreenTest
import artemis.agent.screens.SetupPageScreen
import artemis.agent.screens.ShipsPageScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.walkertribe.ian.world.Artemis
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ShipsFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun noShipsTest() = run {
        mainScreenTest {
            step("Open Ships page") { SetupPageScreen.shipsPageButton.click() }

            step("No ships") {
                ShipsPageScreen {
                    noShipsLabel.isDisplayedWithText(R.string.no_ships)
                    shipsList.isDisplayedWithSize(0)
                }
            }
        }
    }

    @Test
    fun connectedTest() = run {
        mainScreenTest(false) {
            scenario(
                ConnectScenario(ConnectFragmentTest.FAKE_SERVER_IP, activityScenarioRule.scenario)
            )

            step("Ships page opened") { SetupPageScreen.shipsPageButton.isChecked() }

            ShipsPageScreen {
                step("Ships list populated") {
                    noShipsLabel.isRemoved()
                    shipsList {
                        isDisplayedWithSize(Artemis.SHIP_COUNT)
                        children<ShipsPageScreen.ShipItem> {
                            selectedShipLabel.isRemoved()
                            nameLabel.isDisplayed()
                            vesselLabel.isDisplayed()
                            driveTypeLabel.isDisplayed()
                            descriptionLabel.isDisplayed()
                        }
                    }
                }
            }

            step("Select ship") {
                ShipsPageScreen.shipsList.childAt<ShipsPageScreen.ShipItem>(0) { click() }
            }

            step("Game page opened") {
                setupPageButton.isNotChecked()
                SetupPageScreen.shipsPageButton.doesNotExist()
                gamePageButton.isChecked()
                GamePageScreen.shipNumberLabel.isDisplayedWithText("Ship 1")
            }

            step("Return to Setup page") { setupPageButton.click() }

            step("Ships page still open") {
                SetupPageScreen.shipsPageButton {
                    isDisplayed()
                    isChecked()
                }
                ShipsPageScreen.shipsList.isDisplayedWithSize(Artemis.SHIP_COUNT)
            }

            step("Selected ship is marked") {
                ShipsPageScreen.shipsList.childAt<ShipsPageScreen.ShipItem>(0) {
                    selectedShipLabel.isDisplayedWithText(R.string.selected)
                }
            }
        }
    }
}
