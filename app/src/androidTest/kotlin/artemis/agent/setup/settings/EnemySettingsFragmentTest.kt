package artemis.agent.setup.settings

import androidx.activity.viewModels
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.ActivityScenarioManager
import artemis.agent.AgentViewModel
import artemis.agent.ArtemisAgentTestHelpers
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
@LargeTest
class EnemySettingsFragmentTest {
    @get:Rule
    val activityScenarioManager = ActivityScenarioManager.forActivity<MainActivity>()

    @Test
    fun enemySettingsTest() {
        val enemiesEnabled = AtomicBoolean()
        val maxSurrenderRange = AtomicInteger(-1)
        val showIntel = AtomicBoolean()
        val showTauntStatus = AtomicBoolean()
        val disableIneffectiveTaunts = AtomicBoolean()

        val sortBySurrendered = AtomicBoolean()
        val sortByFaction = AtomicBoolean()
        val sortByFactionReversed = AtomicBoolean()
        val sortByName = AtomicBoolean()
        val sortByDistance = AtomicBoolean()

        activityScenarioManager.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value
            val enemySorter = viewModel.enemySorter

            sortBySurrendered.lazySet(enemySorter.sortBySurrendered)
            sortByFaction.lazySet(enemySorter.sortByFaction)
            sortByFactionReversed.lazySet(enemySorter.sortByFactionReversed)
            sortByName.lazySet(enemySorter.sortByName)
            sortByDistance.lazySet(enemySorter.sortByDistance)

            enemiesEnabled.lazySet(viewModel.enemiesEnabled)
            viewModel.maxSurrenderDistance?.also { maxSurrenderRange.lazySet(it.toInt()) }
            showIntel.lazySet(viewModel.showEnemyIntel)
            showTauntStatus.lazySet(viewModel.showTauntStatuses)
            disableIneffectiveTaunts.lazySet(viewModel.disableIneffectiveTaunts)
        }

        SettingsFragmentTest.openSettingsMenu()

        val enabled = enemiesEnabled.get()
        val surrenderRange = maxSurrenderRange.get()
        val intel = showIntel.get()
        val tauntStatuses = showTauntStatus.get()
        val disableIneffective = disableIneffectiveTaunts.get()

        val sortSettings = booleanArrayOf(
            sortBySurrendered.get(),
            sortByFaction.get(),
            sortByFactionReversed.get(),
            sortByName.get(),
            sortByDistance.get(),
        )

        booleanArrayOf(!enabled, enabled).forEach { usingToggle ->
            SettingsFragmentTest.openSettingsSubMenu(4, usingToggle = usingToggle, toggleDisplayed = true)
            testEnemySubMenuOpen(
                sortSettings,
                surrenderRange.takeIf { it >= 0 },
                !usingToggle,
                intel,
                tauntStatuses,
                disableIneffective,
            )

            SettingsFragmentTest.closeSettingsSubMenu(usingToggle = !usingToggle)
            testEnemySubMenuClosed()
        }
    }

    private companion object {
        val enemySortMethodSettings = arrayOf(
            GroupedToggleButtonSetting(
                R.id.enemySortingSurrenderButton,
                R.string.surrender,
            ),
            GroupedToggleButtonSetting(
                R.id.enemySortingRaceButton,
                R.string.sort_by_race,
            ),
            GroupedToggleButtonSetting(
                R.id.enemySortingNameButton,
                R.string.sort_by_name,
            ),
            GroupedToggleButtonSetting(
                R.id.enemySortingRangeButton,
                R.string.sort_by_range,
            ),
        )

        val enemySingleToggleSettings = arrayOf(
            SingleToggleButtonSetting(
                R.id.showIntelDivider,
                R.id.showIntelTitle,
                R.string.show_intel,
                R.id.showIntelButton,
            ),
            SingleToggleButtonSetting(
                R.id.showTauntStatusDivider,
                R.id.showTauntStatusTitle,
                R.string.show_taunt_status,
                R.id.showTauntStatusButton,
            ),
            SingleToggleButtonSetting(
                R.id.disableIneffectiveDivider,
                R.id.disableIneffectiveTitle,
                R.string.disable_ineffective_taunts,
                R.id.disableIneffectiveButton,
            ),
        )

        fun testEnemySubMenuOpen(
            sortMethods: BooleanArray,
            surrenderRange: Int?,
            shouldTestSettings: Boolean,
            vararg singleToggles: Boolean,
        ) {
            testEnemySubMenuSortMethods(sortMethods, shouldTestSettings)
            testEnemySubMenuSurrenderRange(surrenderRange, shouldTestSettings)

            enemySingleToggleSettings.forEachIndexed { index, setting ->
                setting.testSingleToggle(singleToggles[index])
            }
        }

        fun testEnemySubMenuClosed() {
            assertNotExist(R.id.enemySortingTitle)
            assertNotExist(R.id.enemySortingDefaultButton)
            enemySortMethodSettings.forEach { assertNotExist(it.button) }
            assertNotExist(R.id.reverseRaceSortTitle)
            assertNotExist(R.id.reverseRaceSortButton)
            assertNotExist(R.id.enemySortingDivider)
            assertNotExist(R.id.surrenderRangeTitle)
            assertNotExist(R.id.surrenderRangeField)
            assertNotExist(R.id.surrenderRangeKm)
            assertNotExist(R.id.surrenderRangeEnableButton)
            assertNotExist(R.id.surrenderRangeInfinity)
            assertNotExist(R.id.surrenderRangeDivider)
            enemySingleToggleSettings.forEach { it.testNotExist() }
        }

        fun testEnemySubMenuSortMethods(sortMethods: BooleanArray, shouldTest: Boolean) {
            scrollTo(R.id.enemySortingDivider)
            assertDisplayed(R.id.enemySortingTitle, R.string.sort_methods)
            assertDisplayed(R.id.enemySortingDefaultButton, R.string.default_setting)

            enemySortMethodSettings.forEachIndexed { index, setting ->
                assertDisplayed(setting.button, setting.text)
                ArtemisAgentTestHelpers.assertChecked(setting.button, sortMethods[index])
            }

            ArtemisAgentTestHelpers.assertChecked(
                R.id.enemySortingDefaultButton,
                sortMethods.none { it },
            )

            if (!shouldTest) return

            clickOn(R.id.enemySortingDefaultButton)
            enemySortMethodSettings.forEach { assertUnchecked(it.button) }

            testEnemySubMenuSortBySurrender()
            testEnemySubMenuSortByRace()
            testEnemySubMenuSortByNameAndRange()
            testEnemySubMenuSortPermutations()

            enemySortMethodSettings.forEachIndexed { index, setting ->
                if (sortMethods[index]) {
                    clickOn(setting.button)
                }
            }
        }

        fun testEnemySubMenuSortBySurrender() {
            SettingsFragmentTest.testSortSingle(
                R.id.enemySortingSurrenderButton,
                R.id.enemySortingDefaultButton,
            )
        }

        fun testEnemySubMenuSortByRace() {
            clickOn(R.id.enemySortingRaceButton)
            assertChecked(R.id.enemySortingRaceButton)
            assertUnchecked(R.id.enemySortingDefaultButton)

            assertDisplayed(R.id.reverseRaceSortTitle, R.string.reverse_sorting_by_race)
            assertDisplayed(R.id.reverseRaceSortButton)

            clickOn(R.id.enemySortingRaceButton)
            assertUnchecked(R.id.enemySortingRaceButton)
            assertChecked(R.id.enemySortingDefaultButton)

            assertNotDisplayed(R.id.reverseRaceSortTitle)
            assertNotDisplayed(R.id.reverseRaceSortButton)
        }

        fun testEnemySubMenuSortByNameAndRange() {
            SettingsFragmentTest.testSortPair(
                R.id.enemySortingNameButton,
                R.id.enemySortingRangeButton,
                R.id.enemySortingDefaultButton,
            )
        }

        fun testEnemySubMenuSortPermutations() {
            SettingsFragmentTest.testSortPermutations(
                R.id.enemySortingDefaultButton,
                R.id.enemySortingSurrenderButton,
                R.id.enemySortingRaceButton,
                R.id.enemySortingNameButton,
                R.id.enemySortingRangeButton,
                R.id.enemySortingSurrenderButton,
                R.id.enemySortingRaceButton,
                R.id.enemySortingRangeButton,
            )
        }

        fun testEnemySubMenuSurrenderRange(surrenderRange: Int?, shouldTest: Boolean) {
            scrollTo(R.id.surrenderRangeDivider)
            assertDisplayed(R.id.surrenderRangeTitle, R.string.surrender_range)
            assertDisplayed(R.id.surrenderRangeEnableButton)

            testEnemySubMenuSurrenderRange(surrenderRange != null, surrenderRange)

            if (!shouldTest) return

            booleanArrayOf(true, false).forEach {
                clickOn(R.id.surrenderRangeEnableButton)
                testEnemySubMenuSurrenderRange((surrenderRange == null) == it, surrenderRange)
            }
        }

        fun testEnemySubMenuSurrenderRange(
            isEnabled: Boolean,
            surrenderRange: Int?,
        ) {
            if (isEnabled) {
                assertChecked(R.id.surrenderRangeEnableButton)
                assertNotDisplayed(R.id.surrenderRangeInfinity)
                assertDisplayed(R.id.surrenderRangeKm, R.string.kilometres)
                if (surrenderRange == null) {
                    assertDisplayed(R.id.surrenderRangeField)
                } else {
                    assertDisplayed(R.id.surrenderRangeField, surrenderRange.toString())
                }
            } else {
                assertUnchecked(R.id.surrenderRangeEnableButton)
                assertDisplayed(R.id.surrenderRangeInfinity, R.string.infinity)
                assertNotDisplayed(R.id.surrenderRangeKm)
                assertNotDisplayed(R.id.surrenderRangeField)
            }
        }
    }
}
