package artemis.agent.setup.settings

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.game.route.RouteTaskIncentive
import artemis.agent.isDisplayedWithText
import artemis.agent.scenario.AllAndNoneSettingsScenario
import artemis.agent.scenario.SettingsMenuScenario
import artemis.agent.scenario.SettingsSubmenuOpenScenario
import artemis.agent.screens.MainScreen.mainScreenTest
import artemis.agent.screens.SettingsPageScreen
import artemis.agent.withViewModel
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class RoutingSettingsFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun routingSettingsMutableTest() {
        testWithSettings { data ->
            booleanArrayOf(true, false).forEach { testSettings ->
                testData(
                    data = data,
                    openWithToggle = data.enabled != testSettings,
                    testSettings = testSettings,
                    closeWithToggle = data.enabled == testSettings,
                    closeWithBack = false,
                )
            }
        }
    }

    @Test
    fun routingSettingsBackButtonTest() {
        testWithSettings { data ->
            if (data.enabled) testRoutingSubMenuDisableFromMenu()

            testData(
                data = data,
                openWithToggle = true,
                testSettings = false,
                closeWithToggle = false,
                closeWithBack = true,
            )

            if (!data.enabled) testRoutingSubMenuDisableFromMenu()
        }
    }

    private fun testWithSettings(test: TestContext<Unit>.(Data) -> Unit) {
        run {
            mainScreenTest {
                withViewModel { viewModel ->
                    val routingEnabled = viewModel.routingEnabled

                    val incentives = BooleanArray(RouteTaskIncentive.entries.size) { false }
                    viewModel.routeIncentives.forEach { incentive ->
                        incentives[incentive.ordinal] = true
                    }

                    val incentivesData =
                        IncentivesData(
                            needsEnergy = incentives[RouteTaskIncentive.NEEDS_ENERGY.ordinal],
                            needsDamCon = incentives[RouteTaskIncentive.NEEDS_DAMCON.ordinal],
                            malfunction = incentives[RouteTaskIncentive.RESET_COMPUTER.ordinal],
                            ambassador = incentives[RouteTaskIncentive.AMBASSADOR_PICKUP.ordinal],
                            hostage = incentives[RouteTaskIncentive.HOSTAGE.ordinal],
                            commandeered = incentives[RouteTaskIncentive.COMMANDEERED.ordinal],
                            hasEnergy = incentives[RouteTaskIncentive.HAS_ENERGY.ordinal],
                            hasMissions = viewModel.routeIncludesMissions,
                        )

                    val avoidanceData =
                        AvoidanceData(
                            blackHolesEnabled = viewModel.avoidBlackHoles,
                            blackHolesClearance = viewModel.blackHoleClearance.toInt(),
                            minesEnabled = viewModel.avoidMines,
                            minesClearance = viewModel.mineClearance.toInt(),
                            typhonsEnabled = viewModel.avoidTyphons,
                            typhonsClearance = viewModel.typhonClearance.toInt(),
                        )

                    scenario(SettingsMenuScenario)

                    test(Data(routingEnabled, incentivesData, avoidanceData))
                }
            }
        }
    }

    private data class Data(
        val enabled: Boolean,
        val incentives: IncentivesData,
        val avoidances: AvoidanceData,
    )

    private data class IncentivesData(
        val needsEnergy: Boolean,
        val needsDamCon: Boolean,
        val malfunction: Boolean,
        val ambassador: Boolean,
        val hostage: Boolean,
        val commandeered: Boolean,
        val hasEnergy: Boolean,
        val hasMissions: Boolean,
    ) {
        private val array by lazy {
            booleanArrayOf(
                needsEnergy,
                needsDamCon,
                malfunction,
                ambassador,
                hostage,
                commandeered,
                hasEnergy,
                hasMissions,
            )
        }

        fun toArray(): BooleanArray = array
    }

    private data class AvoidanceData(
        val blackHolesEnabled: Boolean,
        val blackHolesClearance: Int,
        val minesEnabled: Boolean,
        val minesClearance: Int,
        val typhonsEnabled: Boolean,
        val typhonsClearance: Int,
    ) {
        val arrayEnabled by lazy { booleanArrayOf(blackHolesEnabled, minesEnabled, typhonsEnabled) }
        val clearances by lazy { intArrayOf(blackHolesClearance, minesClearance, typhonsClearance) }
    }

    private companion object {
        const val ENTRY_INDEX = 6

        fun TestContext<Unit>.testData(
            data: Data,
            openWithToggle: Boolean,
            testSettings: Boolean,
            closeWithToggle: Boolean,
            closeWithBack: Boolean,
        ) {
            scenario(SettingsSubmenuOpenScenario.Routing(openWithToggle))
            testRoutingSubMenuOpen(
                incentives = data.incentives,
                avoidances = data.avoidances,
                shouldTestSettings = testSettings,
            )

            step("Close submenu") {
                if (closeWithBack) SettingsPageScreen.backFromSubmenu()
                else SettingsPageScreen.closeSubmenu(closeWithToggle)
            }

            step("All settings should be gone") {
                testScreenClosed(closeWithBack || !closeWithToggle)
            }
        }

        fun TestContext<Unit>.testRoutingSubMenuOpen(
            incentives: IncentivesData,
            avoidances: AvoidanceData,
            shouldTestSettings: Boolean,
        ) {
            testRoutingSubMenuIncentives(incentives, shouldTestSettings)
            testRoutingSubMenuAvoidances(avoidances, shouldTestSettings)
        }

        fun TestContext<Unit>.testRoutingSubMenuIncentives(
            incentives: IncentivesData,
            shouldTest: Boolean,
        ) {
            SettingsPageScreen.Routing {
                step("Incentive settings") {
                    val incentivesArray = incentives.toArray()

                    step("All components displayed") {
                        incentivesDivider.scrollTo()
                        incentivesTitle.isDisplayedWithText(R.string.included_incentives)
                        incentivesAllButton.isDisplayedWithText(R.string.all)
                        incentivesNoneButton.isDisplayedWithText(R.string.none)
                        incentiveSettings.forEach { setting ->
                            setting.button.isDisplayedWithText(setting.text)
                        }
                    }

                    scenario(
                        AllAndNoneSettingsScenario(
                            allButton = incentivesAllButton,
                            noneButton = incentivesNoneButton,
                            settingsButtons =
                                incentiveSettings.mapIndexed { index, setting ->
                                    setting.button to incentivesArray[index]
                                },
                            shouldTest = shouldTest,
                        )
                    )
                }
            }
        }

        fun TestContext<Unit>.testRoutingSubMenuAvoidances(
            data: AvoidanceData,
            shouldTest: Boolean,
        ) {
            SettingsPageScreen.Routing {
                step("Avoidance settings") {
                    step("First line components") {
                        avoidancesDivider.scrollTo()
                        avoidancesTitle.isDisplayedWithText(R.string.avoidances)
                        avoidancesAllButton.isDisplayedWithText(R.string.all)
                        avoidancesNoneButton.isDisplayedWithText(R.string.none)
                    }

                    val enabled = data.arrayEnabled
                    val clearances = data.clearances

                    avoidanceSettings.forEachIndexed { index, setting ->
                        testRoutingSubMenuAvoidanceSetting(
                            setting = setting,
                            isEnabled = enabled[index],
                            clearance = clearances[index],
                            shouldTest = shouldTest,
                        )
                    }

                    scenario(
                        AllAndNoneSettingsScenario(
                            allButton = avoidancesAllButton,
                            noneButton = avoidancesNoneButton,
                            settingsButtons =
                                avoidanceSettings.mapIndexed { index, setting ->
                                    setting.button to enabled[index]
                                },
                            shouldTest = shouldTest,
                        ) { index, on ->
                            val setting = avoidanceSettings[index]
                            step(
                                "Clearance input #${index + 1} should ${if (on) "" else "not "}be displayed"
                            ) {
                                if (on) {
                                    setting.input.isDisplayedWithText(clearances[index].toString())
                                    setting.kmLabel.isDisplayedWithText(R.string.kilometres)
                                } else {
                                    setting.input.isNotDisplayed()
                                    setting.kmLabel.isNotDisplayed()
                                }
                            }
                        }
                    )
                }
            }
        }

        fun TestContext<Unit>.testRoutingSubMenuAvoidanceSetting(
            setting: SettingsPageScreen.Routing.AvoidanceSetting,
            isEnabled: Boolean,
            clearance: Int,
            shouldTest: Boolean,
        ) {
            val title = device.targetContext.getString(setting.text)
            step(title) {
                step("Base components displayed") {
                    setting.label.isDisplayedWithText(title)
                    setting.button.isDisplayed()
                }

                testRoutingSubMenuAvoidanceSettingState(setting, isEnabled, clearance)

                if (!shouldTest) return@step

                listOf(false to "once", true to "again").forEach { (shouldBeEnabled, count) ->
                    step("Toggle setting $count") { setting.button.click() }

                    testRoutingSubMenuAvoidanceSettingState(
                        setting,
                        isEnabled == shouldBeEnabled,
                        clearance,
                    )
                }
            }
        }

        fun TestContext<Unit>.testRoutingSubMenuAvoidanceSettingState(
            setting: SettingsPageScreen.Routing.AvoidanceSetting,
            isEnabled: Boolean,
            clearance: Int,
        ) {
            step("Clearance input ${if (isEnabled) "" else "not "}displayed") {
                if (isEnabled) {
                    setting.button.isChecked()
                    setting.input.isDisplayedWithText(clearance.toString())
                    setting.kmLabel.isDisplayedWithText(R.string.kilometres)
                } else {
                    setting.button.isNotChecked()
                    setting.input.isNotDisplayed()
                    setting.kmLabel.isNotDisplayed()
                }
            }
        }

        fun TestContext<Unit>.testRoutingSubMenuDisableFromMenu() {
            step("Deactivate submenu from main menu") {
                SettingsPageScreen.deactivateSubmenu(ENTRY_INDEX)
            }

            step("Submenu should not have been opened") { testScreenClosed(false) }
        }

        fun TestContext<Unit>.testScreenClosed(isToggleOn: Boolean) {
            SettingsPageScreen.Routing {
                incentivesTitle.doesNotExist()
                incentivesAllButton.doesNotExist()
                incentivesNoneButton.doesNotExist()
                incentivesDivider.doesNotExist()

                avoidancesTitle.doesNotExist()
                avoidancesAllButton.doesNotExist()
                avoidancesNoneButton.doesNotExist()
                avoidancesDivider.doesNotExist()

                incentiveSettings.forEach { it.button.doesNotExist() }
                avoidanceSettings.forEach { it.doesNotExist() }
            }

            flakySafely { SettingsPageScreen.Menu.testToggleState(ENTRY_INDEX, isToggleOn) }
        }
    }
}
