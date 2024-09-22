package artemis.agent.setup.settings

import androidx.activity.viewModels
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.AgentViewModel
import artemis.agent.ArtemisAgentTestHelpers
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertChecked
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
class AllySettingsFragmentTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun allySettingsTest() {
        val alliesEnabled = AtomicBoolean()
        val showingDestroyed = AtomicBoolean()
        val manuallyReturning = AtomicBoolean()

        val sortByClassFirst = AtomicBoolean()
        val sortByEnergy = AtomicBoolean()
        val sortByStatus = AtomicBoolean()
        val sortByClassSecond = AtomicBoolean()
        val sortByName = AtomicBoolean()

        activityScenarioRule.scenario.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value
            val allySorter = viewModel.allySorter

            sortByClassFirst.lazySet(allySorter.sortByClassFirst)
            sortByEnergy.lazySet(allySorter.sortByEnergy)
            sortByStatus.lazySet(allySorter.sortByStatus)
            sortByClassSecond.lazySet(allySorter.sortByClassSecond)
            sortByName.lazySet(allySorter.sortByName)

            alliesEnabled.lazySet(viewModel.alliesEnabled)
            showingDestroyed.lazySet(viewModel.showAllySelector)
            manuallyReturning.lazySet(viewModel.manuallyReturnFromCommands)
        }

        SettingsFragmentTest.openSettingsMenu()

        val enabled = alliesEnabled.get()
        val showDestroyed = showingDestroyed.get()
        val manualReturn = manuallyReturning.get()

        val sortSettings = booleanArrayOf(
            sortByClassFirst.get(),
            sortByEnergy.get(),
            sortByStatus.get(),
            sortByClassSecond.get(),
            sortByName.get(),
        )

        booleanArrayOf(!enabled, enabled).forEach { usingToggle ->
            SettingsFragmentTest.openSettingsSubMenu(3, usingToggle, true)
            testAlliesSubMenuOpen(sortSettings, !usingToggle, showDestroyed, manualReturn)

            SettingsFragmentTest.closeSettingsSubMenu(!usingToggle)
            testAlliesSubMenuClosed()
        }
    }

    private companion object {
        val allySortMethodSettings = arrayOf(
            GroupedToggleButtonSetting(
                R.id.allySortingClassButton1,
                R.string.sort_by_class,
            ),
            GroupedToggleButtonSetting(
                R.id.allySortingEnergyButton,
                R.string.sort_by_energy,
            ),
            GroupedToggleButtonSetting(
                R.id.allySortingStatusButton,
                R.string.sort_by_status,
            ),
            GroupedToggleButtonSetting(
                R.id.allySortingClassButton2,
                R.string.sort_by_class,
            ),
            GroupedToggleButtonSetting(
                R.id.allySortingStatusButton,
                R.string.sort_by_status,
            ),
        )

        val allySingleToggleSettings = arrayOf(
            SingleToggleButtonSetting(
                R.id.showDestroyedAlliesDivider,
                R.id.showDestroyedAlliesTitle,
                R.string.show_destroyed_allies,
                R.id.showDestroyedAlliesButton,
            ),
            SingleToggleButtonSetting(
                R.id.manuallyReturnDivider,
                R.id.manuallyReturnTitle,
                R.string.manually_return_from_commands,
                R.id.manuallyReturnButton,
            ),
        )

        fun testAlliesSubMenuOpen(
            sortMethods: BooleanArray,
            shouldTestSortMethods: Boolean,
            showingDestroyed: Boolean,
            manuallyReturning: Boolean,
        ) {
            testAllySubMenuSortMethods(sortMethods, shouldTestSortMethods)

            allySingleToggleSettings.zip(
                listOf(showingDestroyed, manuallyReturning),
            ).forEach { (setting, isChecked) -> setting.testSingleToggle(isChecked) }
        }

        fun testAlliesSubMenuClosed() {
            assertNotExist(R.id.allySortingTitle)
            assertNotExist(R.id.allySortingDefaultButton)
            allySortMethodSettings.forEach { assertNotExist(it.button) }
            assertNotExist(R.id.allySortingDivider)
            allySingleToggleSettings.forEach {
                assertNotExist(it.button)
                assertNotExist(it.text)
                assertNotExist(it.divider)
            }
        }

        fun testAllySubMenuSortMethods(sortMethods: BooleanArray, shouldTest: Boolean) {
            scrollTo(R.id.allySortingDivider)
            assertDisplayed(R.id.allySortingTitle, R.string.sort_methods)
            assertDisplayed(R.id.allySortingDefaultButton, R.string.default_setting)

            allySortMethodSettings.forEachIndexed { index, setting ->
                assertDisplayed(setting.button, setting.text)
                ArtemisAgentTestHelpers.assertChecked(setting.button, sortMethods[index])
            }

            ArtemisAgentTestHelpers.assertChecked(
                R.id.allySortingDefaultButton,
                sortMethods.none { it },
            )

            if (!shouldTest) return

            clickOn(R.id.allySortingDefaultButton)
            allySortMethodSettings.forEach { assertUnchecked(it.button) }

            testAllySubMenuSortByClass()
            testAllySubMenuSortByEnergyAndStatus()
            testAllySubMenuSortByName()
            testAllySubMenuSortPermutations()

            for (index in allySortMethodSettings.indices.reversed()) {
                if (sortMethods[index]) {
                    clickOn(allySortMethodSettings[index].button)
                }
            }
        }

        fun testAllySubMenuSortByClass() {
            SettingsFragmentTest.testSortPair(
                R.id.allySortingClassButton1,
                R.id.allySortingClassButton2,
                R.id.allySortingDefaultButton,
            )
        }

        fun testAllySubMenuSortByEnergyAndStatus() {
            listOf(
                R.id.allySortingEnergyButton to true,
                R.id.allySortingStatusButton to false,
            ).forEach { (firstButton, energyFirst) ->
                clickOn(firstButton)
                assertChecked(R.id.allySortingStatusButton)
                ArtemisAgentTestHelpers.assertChecked(R.id.allySortingEnergyButton, energyFirst)
                assertUnchecked(R.id.allySortingDefaultButton)

                clickOn(R.id.allySortingEnergyButton)
                assertChecked(R.id.allySortingStatusButton)
                ArtemisAgentTestHelpers.assertChecked(R.id.allySortingEnergyButton, !energyFirst)

                clickOn(R.id.allySortingStatusButton)
                assertUnchecked(R.id.allySortingStatusButton)
                assertUnchecked(R.id.allySortingEnergyButton)
                assertChecked(R.id.allySortingDefaultButton)
            }
        }

        fun testAllySubMenuSortByName() {
            SettingsFragmentTest.testSortSingle(
                R.id.allySortingNameButton,
                R.id.allySortingDefaultButton,
            )
        }

        fun testAllySubMenuSortPermutations() {
            SettingsFragmentTest.testSortPermutations(
                R.id.allySortingDefaultButton,
                R.id.allySortingClassButton1,
                R.id.allySortingEnergyButton,
                R.id.allySortingClassButton2,
                R.id.allySortingNameButton,
                R.id.allySortingEnergyButton,
                R.id.allySortingStatusButton,
                R.id.allySortingClassButton2,
                R.id.allySortingNameButton,
            )
        }
    }
}
