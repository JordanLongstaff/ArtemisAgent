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
import artemis.agent.scenario.SettingsMenuScenario
import artemis.agent.scenario.SettingsSubmenuOpenScenario
import artemis.agent.scenario.SortMethodDependentScenario
import artemis.agent.scenario.SortMethodPairScenario
import artemis.agent.scenario.SortMethodPermutationsScenario
import artemis.agent.scenario.SortMethodSingleScenario
import artemis.agent.screens.MainScreen.mainScreenTest
import artemis.agent.screens.SettingsPageScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AllySettingsFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun allySettingsMutableTest() = testWithSettings { data ->
        booleanArrayOf(true, false).forEach { testSortMethods ->
            testData(
                data = data,
                openWithToggle = data.enabled != testSortMethods,
                testSortMethods = testSortMethods,
                closeWithToggle = data.enabled == testSortMethods,
                closeWithBack = false,
            )
        }
    }

    @Test
    fun allySettingsBackButtonTest() = testWithSettings { data ->
        if (data.enabled) testAlliesSubMenuDisableFromMenu()

        testData(
            data = data,
            openWithToggle = true,
            testSortMethods = false,
            closeWithToggle = false,
            closeWithBack = true,
        )

        if (!data.enabled) testAlliesSubMenuDisableFromMenu()
    }

    private fun testWithSettings(test: TestContext<Unit>.(Data) -> Unit) = run {
        mainScreenTest {
            val alliesEnabled = AtomicBoolean()
            val showingDestroyed = AtomicBoolean()
            val manuallyReturning = AtomicBoolean()

            val sortByClassFirst = AtomicBoolean()
            val sortByEnergy = AtomicBoolean()
            val sortByStatus = AtomicBoolean()
            val sortByClassSecond = AtomicBoolean()
            val sortByName = AtomicBoolean()

            step("Fetch settings") {
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
            }

            scenario(SettingsMenuScenario)

            val enabled = alliesEnabled.get()
            val showDestroyed = showingDestroyed.get()
            val manualReturn = manuallyReturning.get()

            val sortMethods =
                SortMethods(
                    classFirst = sortByClassFirst.get(),
                    energy = sortByEnergy.get(),
                    status = sortByStatus.get(),
                    classSecond = sortByClassSecond.get(),
                    name = sortByName.get(),
                )

            test(Data(enabled, showDestroyed, manualReturn, sortMethods))
        }
    }

    private data class Data(
        val enabled: Boolean,
        val showingDestroyed: Boolean,
        val manuallyReturning: Boolean,
        val sortMethods: SortMethods,
    )

    private data class SortMethods(
        val classFirst: Boolean,
        val energy: Boolean,
        val status: Boolean,
        val classSecond: Boolean,
        val name: Boolean,
    ) {
        private val array by lazy { booleanArrayOf(classFirst, energy, status, classSecond, name) }

        fun toArray(): BooleanArray = array
    }

    private companion object {
        const val ENTRY_INDEX = 3

        fun TestContext<Unit>.testData(
            data: Data,
            openWithToggle: Boolean,
            testSortMethods: Boolean,
            closeWithToggle: Boolean,
            closeWithBack: Boolean,
        ) {
            scenario(SettingsSubmenuOpenScenario.Allies(openWithToggle))
            testAlliesSubMenuOpen(
                data.sortMethods,
                testSortMethods,
                data.showingDestroyed,
                data.manuallyReturning,
            )

            step("Close submenu") {
                if (closeWithBack) SettingsPageScreen.backFromSubmenu()
                else SettingsPageScreen.closeSubmenu(closeWithToggle)
            }

            step("All settings should be gone") {
                SettingsPageScreen.Allies.testScreenClosed(closeWithBack || !closeWithToggle)
            }
        }

        fun TestContext<Unit>.testAlliesSubMenuOpen(
            sortMethods: SortMethods,
            shouldTestSortMethods: Boolean,
            showingDestroyed: Boolean,
            manuallyReturning: Boolean,
        ) {
            testAllySubMenuSortMethods(sortMethods, shouldTestSortMethods)

            step("Test single toggle settings") {
                SettingsPageScreen.Allies.singleToggleSettings
                    .zip(listOf(showingDestroyed, manuallyReturning))
                    .forEach { (setting, isChecked) -> setting.testSingleToggle(isChecked) }
            }
        }

        fun TestContext<Unit>.testAlliesSubMenuDisableFromMenu() {
            step("Deactivate submenu from main menu") {
                SettingsPageScreen.deactivateSubmenu(ENTRY_INDEX)
            }

            step("Submenu should not have been opened") {
                SettingsPageScreen.Allies.testScreenClosed(false)
            }
        }

        fun TestContext<Unit>.testAllySubMenuSortMethods(
            sortMethods: SortMethods,
            shouldTest: Boolean,
        ) {
            SettingsPageScreen.Allies {
                step("First line components displayed") {
                    sortDivider.scrollTo()
                    sortTitle.isDisplayedWithText(R.string.sort_methods)
                    sortDefaultButton.isDisplayedWithText(R.string.default_setting)
                }

                val sortMethodArray = sortMethods.toArray()
                step("Initial state of sort method settings") {
                    sortMethodSettings.forEachIndexed { index, setting ->
                        val name = device.context.getString(setting.text)
                        step(name) {
                            setting.button {
                                isDisplayedWithText(setting.text)
                                isCheckedIf(sortMethodArray[index])
                            }
                        }
                    }

                    step("Default") { sortDefaultButton.isCheckedIf(sortMethodArray.none { it }) }
                }

                if (!shouldTest) return@Allies

                step("Default sort method should deactivate all others") {
                    sortDefaultButton.click()
                    sortMethodSettings.forEach { it.button.isNotChecked() }
                }

                scenario(
                    SortMethodPairScenario(sortClassButton1, sortClassButton2, sortDefaultButton)
                )
                scenario(
                    SortMethodDependentScenario(
                        sortEnergyButton to "energy",
                        sortStatusButton to "status",
                        sortDefaultButton,
                    )
                )
                scenario(SortMethodSingleScenario(sortNameButton, sortDefaultButton))
                scenario(
                    SortMethodPermutationsScenario(
                        sortDefaultButton,
                        sortClassButton1,
                        sortEnergyButton,
                        sortClassButton2,
                        sortNameButton,
                        sortEnergyButton,
                        sortStatusButton,
                        sortClassButton2,
                        sortNameButton,
                    )
                )

                step("Restore sort methods from initial settings") {
                    for (index in sortMethodSettings.indices.reversed()) {
                        if (sortMethodArray[index]) {
                            sortMethodSettings[index].button.click()
                        }
                    }
                }
            }
        }

        fun SettingsPageScreen.Allies.testScreenClosed(isToggleOn: Boolean) {
            sortTitle.doesNotExist()
            sortDefaultButton.doesNotExist()
            sortMethodSettings.forEach { it.button.doesNotExist() }
            sortDivider.doesNotExist()
            singleToggleSettings.forEach { it.testNotExist() }
            SettingsPageScreen.Menu.testToggleState(ENTRY_INDEX, isToggleOn)
        }
    }
}
