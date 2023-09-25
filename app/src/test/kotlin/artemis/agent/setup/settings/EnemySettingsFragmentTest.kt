package artemis.agent.setup.settings

import androidx.activity.viewModels
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.AgentViewModel
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
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

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

        activityScenarioRule.scenario.onActivity { activity ->
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
                intel,
                tauntStatuses,
                disableIneffective,
                !usingToggle,
            )

            SettingsFragmentTest.closeSettingsSubMenu(usingToggle = !usingToggle)
            testEnemySubMenuClosed()
        }
    }

    private companion object {
        val enemySortingButtonIDs = intArrayOf(
            R.id.enemySortingSurrenderButton,
            R.id.enemySortingRaceButton,
            R.id.enemySortingNameButton,
            R.id.enemySortingRangeButton,
        )

        val enemySortingLabels = intArrayOf(
            R.string.surrender,
            R.string.sort_by_race,
            R.string.sort_by_name,
            R.string.sort_by_range,
        )

        fun testEnemySubMenuOpen(
            sortMethods: BooleanArray,
            surrenderRange: Int?,
            showIntel: Boolean,
            showTauntStatus: Boolean,
            disableIneffectiveTaunts: Boolean,
            shouldTestSettings: Boolean,
        ) {
            testEnemySubMenuSortMethods(sortMethods, shouldTestSettings)
            testEnemySubMenuSurrenderRange(surrenderRange, shouldTestSettings)

            SettingsFragmentTest.testSingleToggleSetting(
                R.id.showIntelDivider,
                R.id.showIntelTitle,
                R.string.show_intel,
                R.id.showIntelButton,
                showIntel,
            )
            SettingsFragmentTest.testSingleToggleSetting(
                R.id.showTauntStatusDivider,
                R.id.showTauntStatusTitle,
                R.string.show_taunt_status,
                R.id.showTauntStatusButton,
                showTauntStatus,
            )
            SettingsFragmentTest.testSingleToggleSetting(
                R.id.disableIneffectiveDivider,
                R.id.disableIneffectiveTitle,
                R.string.disable_ineffective_taunts,
                R.id.disableIneffectiveButton,
                disableIneffectiveTaunts,
            )
        }

        fun testEnemySubMenuClosed() {
            assertNotExist(R.id.enemySortingTitle)
            assertNotExist(R.id.enemySortingDefaultButton)
            enemySortingButtonIDs.forEach { assertNotExist(it) }
            assertNotExist(R.id.reverseRaceSortTitle)
            assertNotExist(R.id.reverseRaceSortButton)
            assertNotExist(R.id.enemySortingDivider)
            assertNotExist(R.id.surrenderRangeTitle)
            assertNotExist(R.id.surrenderRangeField)
            assertNotExist(R.id.surrenderRangeKm)
            assertNotExist(R.id.surrenderRangeEnableButton)
            assertNotExist(R.id.surrenderRangeInfinity)
            assertNotExist(R.id.surrenderRangeDivider)
            assertNotExist(R.id.showIntelTitle)
            assertNotExist(R.id.showIntelButton)
            assertNotExist(R.id.showIntelDivider)
            assertNotExist(R.id.showTauntStatusTitle)
            assertNotExist(R.id.showTauntStatusButton)
            assertNotExist(R.id.showTauntStatusDivider)
            assertNotExist(R.id.disableIneffectiveTitle)
            assertNotExist(R.id.disableIneffectiveButton)
            assertNotExist(R.id.disableIneffectiveDivider)
        }

        fun testEnemySubMenuSortMethods(sortMethods: BooleanArray, shouldTest: Boolean) {
            scrollTo(R.id.enemySortingDivider)
            assertDisplayed(R.id.enemySortingTitle, R.string.sort_methods)
            assertDisplayed(R.id.enemySortingDefaultButton, R.string.default_setting)

            enemySortingButtonIDs.forEachIndexed { index, id ->
                assertDisplayed(id, enemySortingLabels[index])
                artemis.agent.ArtemisAgentTestHelpers.assertChecked(id, sortMethods[index])
            }

            artemis.agent.ArtemisAgentTestHelpers.assertChecked(
                R.id.enemySortingDefaultButton,
                sortMethods.none { it },
            )

            if (!shouldTest) return

            clickOn(R.id.enemySortingDefaultButton)
            enemySortingButtonIDs.forEach { assertUnchecked(it) }

            testEnemySubMenuSortBySurrender()
            testEnemySubMenuSortByRace()
            testEnemySubMenuSortByNameAndRange()
            testEnemySubMenuSortPermutations()

            enemySortingButtonIDs.forEachIndexed { index, id ->
                if (sortMethods[index]) {
                    clickOn(id)
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

            clickOn(R.id.surrenderRangeEnableButton)
            testEnemySubMenuSurrenderRange(surrenderRange == null, null)
            clickOn(R.id.surrenderRangeEnableButton)
            testEnemySubMenuSurrenderRange(surrenderRange != null, surrenderRange)
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
