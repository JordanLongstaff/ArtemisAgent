package artemis.agent.setup.settings

import androidx.activity.viewModels
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.AgentViewModel
import artemis.agent.ArtemisAgentTestHelpers
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
@LargeTest
class BiomechSettingsFragmentTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun biomechSettingsTest() {
        val biomechsEnabled = AtomicBoolean()

        val sortByClassFirst = AtomicBoolean()
        val sortByStatus = AtomicBoolean()
        val sortByClassSecond = AtomicBoolean()
        val sortByName = AtomicBoolean()

        activityScenarioRule.scenario.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value
            val biomechSorter = viewModel.biomechSorter

            sortByClassFirst.lazySet(biomechSorter.sortByClassFirst)
            sortByStatus.lazySet(biomechSorter.sortByStatus)
            sortByClassSecond.lazySet(biomechSorter.sortByClassSecond)
            sortByName.lazySet(biomechSorter.sortByName)

            biomechsEnabled.lazySet(viewModel.biomechsEnabled)
        }

        SettingsFragmentTest.openSettingsMenu()

        val enabled = biomechsEnabled.get()
        val sortSettings = booleanArrayOf(
            sortByClassFirst.get(),
            sortByStatus.get(),
            sortByClassSecond.get(),
            sortByName.get(),
        )

        booleanArrayOf(!enabled, enabled).forEach { usingToggle ->
            SettingsFragmentTest.openSettingsSubMenu(5, usingToggle, true)
            testBiomechsSubMenuOpen(sortSettings, !usingToggle)

            SettingsFragmentTest.closeSettingsSubMenu(!usingToggle)
            testBiomechsSubMenuClosed()
        }
    }

    private companion object {
        val biomechSortMethodSettings = arrayOf(
            GroupedToggleButtonSetting(
                R.id.biomechSortingClassButton1,
                R.string.sort_by_class,
            ),
            GroupedToggleButtonSetting(
                R.id.biomechSortingStatusButton,
                R.string.sort_by_status,
            ),
            GroupedToggleButtonSetting(
                R.id.biomechSortingClassButton2,
                R.string.sort_by_class,
            ),
            GroupedToggleButtonSetting(
                R.id.biomechSortingNameButton,
                R.string.sort_by_name,
            ),
        )

        fun testBiomechsSubMenuOpen(sortMethods: BooleanArray, shouldTestSortMethods: Boolean) {
            testBiomechSubMenuSortMethods(sortMethods, shouldTestSortMethods)

            scrollTo(R.id.freezeDurationDivider)
            assertDisplayed(R.id.freezeDurationTitle, R.string.freeze_duration)
            assertDisplayed(R.id.freezeDurationTimeInput)
        }

        fun testBiomechsSubMenuClosed() {
            assertNotExist(R.id.biomechSortingTitle)
            assertNotExist(R.id.biomechSortingDefaultButton)
            biomechSortMethodSettings.forEach { assertNotExist(it.button) }
            assertNotExist(R.id.biomechSortingDivider)
            assertNotExist(R.id.freezeDurationTitle)
            assertNotExist(R.id.freezeDurationTimeInput)
            assertNotExist(R.id.freezeDurationDivider)
        }

        fun testBiomechSubMenuSortMethods(sortMethods: BooleanArray, shouldTest: Boolean) {
            scrollTo(R.id.biomechSortingDivider)
            assertDisplayed(R.id.biomechSortingTitle, R.string.sort_methods)
            assertDisplayed(R.id.biomechSortingDefaultButton, R.string.default_setting)

            biomechSortMethodSettings.forEachIndexed { index, setting ->
                assertDisplayed(setting.button, setting.text)
                ArtemisAgentTestHelpers.assertChecked(setting.button, sortMethods[index])
            }

            ArtemisAgentTestHelpers.assertChecked(
                R.id.biomechSortingDefaultButton,
                sortMethods.none { it },
            )

            if (!shouldTest) return

            clickOn(R.id.biomechSortingDefaultButton)
            biomechSortMethodSettings.forEach { assertUnchecked(it.button) }

            testBiomechsSubMenuSortByClass()
            testBiomechsSubMenuSortByStatus()
            testBiomechsSubMenuSortByName()
            testBiomechsSubMenuSortPermutations()

            biomechSortMethodSettings.forEachIndexed { index, setting ->
                if (sortMethods[index]) {
                    clickOn(setting.button)
                }
            }
        }

        fun testBiomechsSubMenuSortByClass() {
            SettingsFragmentTest.testSortPair(
                R.id.biomechSortingClassButton1,
                R.id.biomechSortingClassButton2,
                R.id.biomechSortingDefaultButton,
            )
        }

        fun testBiomechsSubMenuSortByStatus() {
            SettingsFragmentTest.testSortSingle(
                R.id.biomechSortingStatusButton,
                R.id.biomechSortingDefaultButton,
            )
        }

        fun testBiomechsSubMenuSortByName() {
            SettingsFragmentTest.testSortSingle(
                R.id.biomechSortingNameButton,
                R.id.biomechSortingDefaultButton,
            )
        }

        fun testBiomechsSubMenuSortPermutations() {
            SettingsFragmentTest.testSortPermutations(
                R.id.biomechSortingDefaultButton,
                R.id.biomechSortingClassButton1,
                R.id.biomechSortingStatusButton,
                R.id.biomechSortingClassButton2,
                R.id.biomechSortingNameButton,
                R.id.biomechSortingStatusButton,
                R.id.biomechSortingClassButton2,
                R.id.biomechSortingNameButton,
            )
        }
    }
}
