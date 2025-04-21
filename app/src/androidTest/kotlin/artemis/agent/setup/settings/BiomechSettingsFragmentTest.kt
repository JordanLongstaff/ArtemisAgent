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
import artemis.agent.scenario.SortMethodPairScenario
import artemis.agent.scenario.SortMethodPermutationsScenario
import artemis.agent.scenario.SortMethodSingleScenario
import artemis.agent.scenario.TimeInputTestScenario
import artemis.agent.screens.MainScreen.mainScreenTest
import artemis.agent.screens.SettingsPageScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BiomechSettingsFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun biomechSettingsMutableTest() = testWithSettings { data ->
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
    fun biomechSettingsBackButtonTest() = testWithSettings { data ->
        if (data.enabled) testBiomechsSubMenuDisableFromMenu()

        testData(
            data = data,
            openWithToggle = true,
            testSettings = false,
            closeWithToggle = false,
            closeWithBack = true,
        )

        if (!data.enabled) testBiomechsSubMenuDisableFromMenu()
    }

    private fun testWithSettings(test: TestContext<Unit>.(Data) -> Unit) = run {
        mainScreenTest {
            val biomechsEnabled = AtomicBoolean()
            val freezeTime = AtomicLong()

            val sortByClassFirst = AtomicBoolean()
            val sortByStatus = AtomicBoolean()
            val sortByClassSecond = AtomicBoolean()
            val sortByName = AtomicBoolean()

            step("Fetch settings") {
                activityScenarioRule.scenario.onActivity { activity ->
                    val viewModel = activity.viewModels<AgentViewModel>().value
                    val biomechManager = viewModel.biomechManager
                    val biomechSorter = biomechManager.sorter

                    sortByClassFirst.lazySet(biomechSorter.sortByClassFirst)
                    sortByStatus.lazySet(biomechSorter.sortByStatus)
                    sortByClassSecond.lazySet(biomechSorter.sortByClassSecond)
                    sortByName.lazySet(biomechSorter.sortByName)

                    biomechsEnabled.lazySet(biomechManager.enabled)
                    freezeTime.lazySet(biomechManager.freezeTime)
                }
            }

            scenario(SettingsMenuScenario)

            val enabled = biomechsEnabled.get()
            val freezeSeconds = freezeTime.get().milliseconds.inWholeSeconds.toInt()
            val sortMethods =
                SortMethods(
                    classFirst = sortByClassFirst.get(),
                    status = sortByStatus.get(),
                    classSecond = sortByClassSecond.get(),
                    name = sortByName.get(),
                )

            test(Data(enabled, freezeSeconds, sortMethods))
        }
    }

    private data class Data(
        val enabled: Boolean,
        val freezeSeconds: Int,
        val sortMethods: SortMethods,
    )

    private data class SortMethods(
        val classFirst: Boolean,
        val status: Boolean,
        val classSecond: Boolean,
        val name: Boolean,
    ) {
        private val array by lazy { booleanArrayOf(classFirst, status, classSecond, name) }

        fun toArray(): BooleanArray = array
    }

    private companion object {
        const val ENTRY_INDEX = 5

        fun TestContext<Unit>.testData(
            data: Data,
            openWithToggle: Boolean,
            testSettings: Boolean,
            closeWithToggle: Boolean,
            closeWithBack: Boolean,
        ) {
            scenario(SettingsSubmenuOpenScenario.Biomechs(openWithToggle))
            testBiomechsSubMenuOpen(data.sortMethods, data.freezeSeconds, testSettings)

            step("Close submenu") {
                if (closeWithBack) SettingsPageScreen.backFromSubmenu()
                else SettingsPageScreen.closeSubmenu(closeWithToggle)
            }

            step("All settings should be gone") {
                SettingsPageScreen.Biomechs.testScreenClosed(closeWithBack || !closeWithToggle)
            }
        }

        fun TestContext<Unit>.testBiomechsSubMenuOpen(
            sortMethods: SortMethods,
            freezeSeconds: Int,
            shouldTestSettings: Boolean,
        ) {
            testBiomechSubMenuSortMethods(sortMethods, shouldTestSettings)

            SettingsPageScreen.Biomechs {
                step("Freeze duration setting displayed") {
                    freezeDurationDivider.scrollTo()
                    freezeDurationTitle.isDisplayedWithText(R.string.freeze_duration)
                    freezeDurationTimeInput.isDisplayed(withMinutes = true)
                }

                if (shouldTestSettings) {
                    step("Freeze duration changing test") {
                        scenario(
                            TimeInputTestScenario(freezeDurationTimeInput, freezeSeconds, true)
                        )
                    }
                }
            }
        }

        fun TestContext<Unit>.testBiomechsSubMenuDisableFromMenu() {
            step("Deactivate submenu from main menu") {
                SettingsPageScreen.deactivateSubmenu(ENTRY_INDEX)
            }

            step("Submenu should not have been opened") {
                SettingsPageScreen.Biomechs.testScreenClosed(false)
            }
        }

        fun TestContext<Unit>.testBiomechSubMenuSortMethods(
            sortMethods: SortMethods,
            shouldTest: Boolean,
        ) {
            SettingsPageScreen.Biomechs {
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

                if (!shouldTest) return@Biomechs

                step("Default sort method should deactivate all others") {
                    sortDefaultButton.click()
                    sortMethodSettings.forEach { it.button.isNotChecked() }
                }

                scenario(
                    SortMethodPairScenario(sortClassButton1, sortClassButton2, sortDefaultButton)
                )
                scenario(SortMethodSingleScenario(sortStatusButton, sortDefaultButton))
                scenario(SortMethodSingleScenario(sortNameButton, sortDefaultButton))
                scenario(
                    SortMethodPermutationsScenario(
                        sortDefaultButton,
                        sortClassButton1,
                        sortStatusButton,
                        sortClassButton2,
                        sortNameButton,
                        sortStatusButton,
                        sortClassButton2,
                        sortNameButton,
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

        fun SettingsPageScreen.Biomechs.testScreenClosed(isToggleOn: Boolean) {
            sortTitle.doesNotExist()
            sortDefaultButton.doesNotExist()
            sortMethodSettings.forEach { it.button.doesNotExist() }
            sortDivider.doesNotExist()
            freezeDurationTitle.doesNotExist()
            freezeDurationTimeInput.doesNotExist()
            freezeDurationDivider.doesNotExist()
            SettingsPageScreen.Menu.testToggleState(ENTRY_INDEX, isToggleOn)
        }
    }
}
