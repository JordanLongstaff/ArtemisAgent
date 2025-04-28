package artemis.agent.setup.settings

import androidx.activity.viewModels
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.AgentViewModel
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.isCheckedIf
import artemis.agent.isDisplayedWithText
import artemis.agent.isRemoved
import artemis.agent.scenario.SettingsMenuScenario
import artemis.agent.scenario.SettingsSubmenuOpenScenario
import artemis.agent.scenario.SortMethodPairScenario
import artemis.agent.scenario.SortMethodPermutationsScenario
import artemis.agent.scenario.SortMethodSingleScenario
import artemis.agent.screens.MainScreen.mainScreenTest
import artemis.agent.screens.SettingsPageScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class EnemySettingsFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun enemySettingsMutableTest() = testWithSettings { data ->
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

    @Test
    fun enemySettingsBackButtonTest() = testWithSettings { data ->
        if (data.enabled) testEnemySubMenuDisableFromMenu()

        testData(
            data = data,
            openWithToggle = true,
            testSettings = false,
            closeWithToggle = false,
            closeWithBack = true,
        )

        if (!data.enabled) testEnemySubMenuDisableFromMenu()
    }

    private fun testWithSettings(test: TestContext<Unit>.(Data) -> Unit) = run {
        mainScreenTest {
            val enemiesEnabled = AtomicBoolean()
            val maxSurrenderRange = AtomicInteger(-1)
            val showIntel = AtomicBoolean()
            val showTauntStatuses = AtomicBoolean()
            val disableIneffectiveTaunts = AtomicBoolean()

            val sortBySurrendered = AtomicBoolean()
            val sortByFaction = AtomicBoolean()
            val sortByFactionReversed = AtomicBoolean()
            val sortByName = AtomicBoolean()
            val sortByDistance = AtomicBoolean()

            step("Fetch settings") {
                activityScenarioRule.scenario.onActivity { activity ->
                    val viewModel = activity.viewModels<AgentViewModel>().value
                    val enemiesManager = viewModel.enemiesManager
                    val enemySorter = enemiesManager.sorter

                    enemiesEnabled.lazySet(enemiesManager.enabled)
                    enemiesManager.maxSurrenderDistance?.also {
                        maxSurrenderRange.lazySet(it.toInt())
                    }
                    showIntel.lazySet(enemiesManager.showIntel)
                    showTauntStatuses.lazySet(enemiesManager.showTauntStatuses)
                    disableIneffectiveTaunts.lazySet(enemiesManager.disableIneffectiveTaunts)

                    sortBySurrendered.lazySet(enemySorter.sortBySurrendered)
                    sortByFaction.lazySet(enemySorter.sortByFaction)
                    sortByFactionReversed.lazySet(enemySorter.sortByFactionReversed)
                    sortByName.lazySet(enemySorter.sortByName)
                    sortByDistance.lazySet(enemySorter.sortByDistance)
                }
            }

            scenario(SettingsMenuScenario)

            val sortMethods =
                SortMethods(
                    surrender = sortBySurrendered.get(),
                    faction = sortByFaction.get(),
                    factionReversed = sortByFactionReversed.get(),
                    name = sortByName.get(),
                    distance = sortByDistance.get(),
                )

            val data =
                Data(
                    enabled = enemiesEnabled.get(),
                    surrenderRange = maxSurrenderRange.get().takeIf { it >= 0 },
                    showIntel = showIntel.get(),
                    showTauntStatuses = showTauntStatuses.get(),
                    disableIneffectiveTaunts = disableIneffectiveTaunts.get(),
                    sortMethods = sortMethods,
                )
            test(data)
        }
    }

    private data class Data(
        val enabled: Boolean,
        val surrenderRange: Int?,
        val showIntel: Boolean,
        val showTauntStatuses: Boolean,
        val disableIneffectiveTaunts: Boolean,
        val sortMethods: SortMethods,
    ) {
        val singleToggles by lazy {
            booleanArrayOf(showIntel, showTauntStatuses, disableIneffectiveTaunts)
        }
    }

    private data class SortMethods(
        val surrender: Boolean,
        val faction: Boolean,
        val factionReversed: Boolean,
        val name: Boolean,
        val distance: Boolean,
    ) {
        private val array by lazy {
            booleanArrayOf(surrender, faction, factionReversed, name, distance)
        }

        fun toArray(): BooleanArray = array
    }

    private companion object {
        const val ENTRY_INDEX = 4

        fun TestContext<Unit>.testData(
            data: Data,
            openWithToggle: Boolean,
            testSettings: Boolean,
            closeWithToggle: Boolean,
            closeWithBack: Boolean,
        ) {
            scenario(SettingsSubmenuOpenScenario.Enemies(openWithToggle))
            testEnemySubMenuOpen(data, testSettings)

            step("Close submenu") {
                if (closeWithBack) SettingsPageScreen.backFromSubmenu()
                else SettingsPageScreen.closeSubmenu(closeWithToggle)
            }

            step("All settings should be gone") {
                SettingsPageScreen.Enemies.testScreenClosed(closeWithBack || !closeWithToggle)
            }
        }

        fun TestContext<Unit>.testEnemySubMenuOpen(data: Data, shouldTestSettings: Boolean) {
            testEnemySubMenuSortMethods(data.sortMethods, shouldTestSettings)
            testEnemySubMenuSurrenderRange(data.surrenderRange, shouldTestSettings)

            SettingsPageScreen.Enemies.singleToggleSettings.forEachIndexed { index, setting ->
                setting.testSingleToggle(data.singleToggles[index])
            }
        }

        fun TestContext<Unit>.testEnemySubMenuDisableFromMenu() {
            step("Deactivate submenu from main menu") {
                SettingsPageScreen.deactivateSubmenu(ENTRY_INDEX)
            }

            step("Submenu should not have been opened") {
                SettingsPageScreen.Enemies.testScreenClosed(false)
            }
        }

        fun TestContext<Unit>.testEnemySubMenuSortMethods(
            sortMethods: SortMethods,
            shouldTest: Boolean,
        ) {
            val sortMethodArray = sortMethods.toArray()

            SettingsPageScreen.Enemies {
                step("First line components displayed") {
                    sortDivider.scrollTo()
                    sortTitle.isDisplayedWithText(R.string.sort_methods)
                    sortDefaultButton.isDisplayedWithText(R.string.default_setting)
                }

                step("Initial state of sort method settings") {
                    sortMethodSettings.forEachIndexed { index, setting ->
                        val name = device.targetContext.getString(setting.text)
                        step(name) {
                            setting.button {
                                isDisplayedWithText(name)
                                isCheckedIf(sortMethodArray[index])
                            }
                        }
                    }

                    step("Default") { sortDefaultButton.isCheckedIf(sortMethodArray.none { it }) }
                }
            }

            if (!shouldTest) return

            SettingsPageScreen.Enemies {
                step("Default sort method should deactivate all others") {
                    sortDefaultButton.click()
                    sortMethodSettings.forEach { it.button.isNotChecked() }
                }

                scenario(SortMethodSingleScenario(sortSurrenderButton, sortDefaultButton))
            }

            testEnemySubMenuSortByRace(sortMethods.factionReversed)

            SettingsPageScreen.Enemies {
                scenario(SortMethodPairScenario(sortNameButton, sortRangeButton, sortDefaultButton))
                scenario(
                    SortMethodPermutationsScenario(
                        sortDefaultButton,
                        sortSurrenderButton,
                        sortRaceButton,
                        sortNameButton,
                        sortRangeButton,
                        sortSurrenderButton,
                        sortRaceButton,
                        sortRangeButton,
                    )
                )

                step("Restore sort methods from initial settings") {
                    sortMethodSettings.forEachIndexed { index, setting ->
                        if (sortMethodArray[index]) {
                            setting.button.click()
                        }
                    }
                }
            }
        }

        fun TestContext<*>.testEnemySubMenuSortByRace(sortByFactionReversed: Boolean) {
            SettingsPageScreen.Enemies {
                step("Sorting by race") {
                    step("Reverse sort setting should be hidden") {
                        reverseRaceSortSingleToggle.testHidden()
                    }

                    step("Activate") {
                        sortRaceButton {
                            click()
                            isChecked()
                        }
                    }

                    step("Sort method should not be default") { sortDefaultButton.isNotChecked() }

                    step("Reverse sort setting should be displayed") {
                        reverseRaceSortSingleToggle.testSingleToggle(sortByFactionReversed)
                    }

                    step("Deactivate") {
                        sortRaceButton {
                            click()
                            isNotChecked()
                        }
                    }

                    step("Sort method should be default") { sortDefaultButton.isChecked() }

                    step("Reverse sort setting should be hidden again") {
                        reverseRaceSortSingleToggle.testHidden()
                    }
                }
            }
        }

        fun TestContext<Unit>.testEnemySubMenuSurrenderRange(
            surrenderRange: Int?,
            shouldTest: Boolean,
        ) {
            step("Surrender range setting") {
                SettingsPageScreen.Enemies {
                    step("Toggle components displayed") {
                        surrenderRangeDivider.scrollTo()
                        surrenderRangeTitle.isDisplayedWithText(R.string.surrender_range)
                        surrenderRangeEnableButton.isDisplayed()
                    }
                }

                val isSurrenderRangeEnabled = surrenderRange != null

                testEnemySubMenuSurrenderRange(isSurrenderRangeEnabled, surrenderRange)

                if (!shouldTest) return@step

                listOf(false to "once", true to "again").forEach { (shouldBeEnabled, count) ->
                    step("Toggle setting $count") {
                        SettingsPageScreen.Enemies.surrenderRangeEnableButton.click()
                    }

                    testEnemySubMenuSurrenderRange(
                        isSurrenderRangeEnabled == shouldBeEnabled,
                        surrenderRange,
                    )
                }
            }
        }

        fun TestContext<Unit>.testEnemySubMenuSurrenderRange(
            isEnabled: Boolean,
            surrenderRange: Int?,
        ) {
            step("Surrender range input ${if (isEnabled) "" else "not "}displayed}") {
                SettingsPageScreen.Enemies {
                    if (isEnabled) {
                        surrenderRangeEnableButton.isChecked()
                        surrenderRangeInfinity.isRemoved()
                        surrenderRangeKm.isDisplayedWithText(R.string.kilometres)
                        surrenderRangeField.isDisplayed()
                        if (surrenderRange != null) {
                            surrenderRangeField.hasText(surrenderRange.toString())
                        }
                    } else {
                        surrenderRangeEnableButton.isNotChecked()
                        surrenderRangeInfinity.isDisplayedWithText(R.string.infinity)
                        surrenderRangeKm.isRemoved()
                        surrenderRangeField.isRemoved()
                    }
                }
            }
        }

        fun SettingsPageScreen.Enemies.testScreenClosed(isToggleOn: Boolean) {
            sortTitle.doesNotExist()
            sortDefaultButton.doesNotExist()
            sortMethodSettings.forEach { it.button.doesNotExist() }
            reverseRaceSortSingleToggle.testNotExist()
            surrenderRangeTitle.doesNotExist()
            surrenderRangeField.doesNotExist()
            surrenderRangeKm.doesNotExist()
            surrenderRangeEnableButton.doesNotExist()
            surrenderRangeInfinity.doesNotExist()
            surrenderRangeDivider.doesNotExist()
            singleToggleSettings.forEach { it.testNotExist() }

            SettingsPageScreen.Menu.testToggleState(ENTRY_INDEX, isToggleOn)
        }
    }
}
