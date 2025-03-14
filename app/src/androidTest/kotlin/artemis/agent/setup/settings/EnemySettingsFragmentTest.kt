package artemis.agent.setup.settings

import android.Manifest
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
import com.adevinta.android.barista.interaction.PermissionGranter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class EnemySettingsFragmentTest {
    @get:Rule val activityScenarioManager = ActivityScenarioManager.forActivity<MainActivity>()

    @Test
    fun enemySettingsTest() {
        val enemiesEnabled = AtomicBoolean()
        val maxSurrenderRange = AtomicInteger(-1)

        val sortSettings = Array(5) { AtomicBoolean() }
        val toggleSettings = Array(3) { AtomicBoolean() }

        activityScenarioManager.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value
            val enemiesManager = viewModel.enemiesManager
            val enemySorter = enemiesManager.sorter

            enemiesEnabled.lazySet(enemiesManager.enabled)
            enemiesManager.maxSurrenderDistance?.also { maxSurrenderRange.lazySet(it.toInt()) }

            booleanArrayOf(
                    enemySorter.sortBySurrendered,
                    enemySorter.sortByFaction,
                    enemySorter.sortByFactionReversed,
                    enemySorter.sortByName,
                    enemySorter.sortByDistance,
                )
                .forEachIndexed { index, sort -> sortSettings[index].lazySet(sort) }

            booleanArrayOf(
                    enemiesManager.showIntel,
                    enemiesManager.showTauntStatuses,
                    enemiesManager.disableIneffectiveTaunts,
                )
                .forEachIndexed { index, toggle -> toggleSettings[index].lazySet(toggle) }
        }

        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

        SettingsFragmentTest.openSettingsMenu()

        val enabled = enemiesEnabled.get()
        val surrenderRange = maxSurrenderRange.get()

        val sortMethods = sortSettings.map { it.get() }.toBooleanArray()
        val singleToggles = toggleSettings.map { it.get() }.toBooleanArray()

        booleanArrayOf(!enabled, enabled).forEach { usingToggle ->
            SettingsFragmentTest.openSettingsSubMenu(ENTRY_INDEX, usingToggle, true)
            testEnemySubMenuOpen(
                sortMethods,
                surrenderRange = surrenderRange.takeIf { it >= 0 },
                shouldTestSettings = !usingToggle,
                singleToggles = singleToggles,
            )

            SettingsFragmentTest.closeSettingsSubMenu(usingToggle = !usingToggle)
            testEnemySubMenuClosed(usingToggle)

            if (usingToggle) {
                SettingsFragmentTest.openSettingsSubMenu(
                    index = ENTRY_INDEX,
                    usingToggle = false,
                    toggleDisplayed = true,
                )
                testEnemySubMenuOpen(
                    sortMethods,
                    surrenderRange = surrenderRange.takeIf { it >= 0 },
                    shouldTestSettings = false,
                    singleToggles = singleToggles,
                )

                SettingsFragmentTest.backFromSubMenu()
                testEnemySubMenuClosed(true)
            }
        }
    }

    private companion object {
        const val ENTRY_INDEX = 4

        val enemySortMethodSettings =
            arrayOf(
                GroupedToggleButtonSetting(R.id.enemySortingSurrenderButton, R.string.surrender),
                GroupedToggleButtonSetting(R.id.enemySortingRaceButton, R.string.sort_by_race),
                GroupedToggleButtonSetting(R.id.enemySortingNameButton, R.string.sort_by_name),
                GroupedToggleButtonSetting(R.id.enemySortingRangeButton, R.string.sort_by_range),
            )

        val enemySingleToggleSettings =
            arrayOf(
                SingleToggleButtonSetting(
                    divider = R.id.showIntelDivider,
                    label = R.id.showIntelTitle,
                    text = R.string.show_intel,
                    button = R.id.showIntelButton,
                ),
                SingleToggleButtonSetting(
                    divider = R.id.showTauntStatusDivider,
                    label = R.id.showTauntStatusTitle,
                    text = R.string.show_taunt_status,
                    button = R.id.showTauntStatusButton,
                ),
                SingleToggleButtonSetting(
                    divider = R.id.disableIneffectiveDivider,
                    label = R.id.disableIneffectiveTitle,
                    text = R.string.disable_ineffective_taunts,
                    button = R.id.disableIneffectiveButton,
                ),
            )

        fun testEnemySubMenuOpen(
            sortMethods: BooleanArray,
            surrenderRange: Int?,
            shouldTestSettings: Boolean,
            singleToggles: BooleanArray,
        ) {
            testEnemySubMenuSortMethods(sortMethods, shouldTestSettings)
            testEnemySubMenuSurrenderRange(surrenderRange, shouldTestSettings)

            enemySingleToggleSettings.forEachIndexed { index, setting ->
                setting.testSingleToggle(singleToggles[index])
            }
        }

        fun testEnemySubMenuClosed(isToggleOn: Boolean) {
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

            SettingsFragmentTest.assertSettingsMenuEntryToggleState(ENTRY_INDEX, isToggleOn)
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

            val isSurrenderRangeEnabled = surrenderRange != null

            testEnemySubMenuSurrenderRange(isSurrenderRangeEnabled, surrenderRange)

            if (!shouldTest) return

            booleanArrayOf(false, true).forEach { isEnabled ->
                clickOn(R.id.surrenderRangeEnableButton)
                testEnemySubMenuSurrenderRange(isSurrenderRangeEnabled == isEnabled, surrenderRange)
            }
        }

        fun testEnemySubMenuSurrenderRange(isEnabled: Boolean, surrenderRange: Int?) {
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
