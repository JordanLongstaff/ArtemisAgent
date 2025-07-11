package artemis.agent.setup.settings

import androidx.activity.viewModels
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.AgentViewModel
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.game.missions.RewardType
import artemis.agent.isDisplayedWithText
import artemis.agent.isHidden
import artemis.agent.scenario.AllAndNoneSettingsScenario
import artemis.agent.scenario.SettingsMenuScenario
import artemis.agent.scenario.SettingsSubmenuOpenScenario
import artemis.agent.scenario.TimeInputTestScenario
import artemis.agent.screens.MainScreen.mainScreenTest
import artemis.agent.screens.SettingsPageScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MissionSettingsFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = activityScenarioRule<MainActivity>()

    @Test
    fun missionSettingsMutableTest() {
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
    fun missionSettingsBackButtonTest() {
        testWithSettings { data ->
            if (data.enabled) testMissionsSubMenuDisableFromMenu()

            testData(
                data = data,
                openWithToggle = true,
                testSettings = false,
                closeWithToggle = false,
                closeWithBack = true,
            )

            if (!data.enabled) testMissionsSubMenuDisableFromMenu()
        }
    }

    private fun testWithSettings(test: TestContext<Unit>.(Data) -> Unit) {
        run {
            mainScreenTest {
                val missionsEnabled = AtomicBoolean()
                val autoDismissal = AtomicBoolean()
                val autoDismissalTime = AtomicLong()
                val rewardsEnabled = Array(RewardType.entries.size) { AtomicBoolean() }

                step("Fetch settings") {
                    activityScenarioRule.scenario.onActivity { activity ->
                        val viewModel = activity.viewModels<AgentViewModel>().value
                        val missionManager = viewModel.missionManager

                        missionsEnabled.lazySet(missionManager.enabled)
                        autoDismissalTime.lazySet(
                            missionManager.completedDismissalSeconds.inWholeSeconds
                        )
                        autoDismissal.lazySet(missionManager.autoDismissCompletedMissions)

                        missionManager.displayedRewards.forEach {
                            rewardsEnabled[it.ordinal].lazySet(true)
                        }
                    }
                }

                scenario(SettingsMenuScenario)

                val enabled = missionsEnabled.get()
                val autoDismissalOn = autoDismissal.get()
                val autoDismissalSeconds = autoDismissalTime.toInt()

                val rewardSettings =
                    RewardSettings(
                        battery = rewardsEnabled[RewardType.BATTERY.ordinal].get(),
                        coolant = rewardsEnabled[RewardType.COOLANT.ordinal].get(),
                        nuke = rewardsEnabled[RewardType.NUKE.ordinal].get(),
                        production = rewardsEnabled[RewardType.PRODUCTION.ordinal].get(),
                        shield = rewardsEnabled[RewardType.SHIELD.ordinal].get(),
                    )

                test(
                    Data(
                        enabled = enabled,
                        autoDismissal = autoDismissalOn,
                        autoDismissalSeconds = autoDismissalSeconds,
                        rewards = rewardSettings,
                    )
                )
            }
        }
    }

    private data class Data(
        val enabled: Boolean,
        val autoDismissal: Boolean,
        val autoDismissalSeconds: Int,
        val rewards: RewardSettings,
    )

    private data class RewardSettings(
        val battery: Boolean,
        val coolant: Boolean,
        val nuke: Boolean,
        val production: Boolean,
        val shield: Boolean,
    ) {
        private val array by lazy { booleanArrayOf(battery, coolant, nuke, production, shield) }

        fun toArray(): BooleanArray = array
    }

    private companion object {
        const val ENTRY_INDEX = 2

        fun TestContext<Unit>.testData(
            data: Data,
            openWithToggle: Boolean,
            testSettings: Boolean,
            closeWithToggle: Boolean,
            closeWithBack: Boolean,
        ) {
            scenario(SettingsSubmenuOpenScenario.Missions(openWithToggle))
            testMissionsSubMenuOpen(
                autoDismissal = data.autoDismissal,
                autoDismissalSeconds = data.autoDismissalSeconds,
                rewardsEnabled = data.rewards.toArray(),
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

        fun TestContext<Unit>.testMissionsSubMenuOpen(
            autoDismissal: Boolean,
            autoDismissalSeconds: Int,
            rewardsEnabled: BooleanArray,
            shouldTestSettings: Boolean,
        ) {
            testMissionsSubMenuRewards(rewardsEnabled, shouldTestSettings)

            step("Auto-dismissal base setting components displayed") {
                SettingsPageScreen.Missions {
                    autoDismissalDivider.scrollTo()
                    autoDismissalTitle.isDisplayedWithText(R.string.auto_dismissal)
                    autoDismissalButton.isCompletelyDisplayed()
                }
            }

            testMissionsSubMenuAutoDismissal(
                autoDismissalSeconds,
                autoDismissal,
                shouldTestSettings,
            )

            if (!shouldTestSettings) return

            booleanArrayOf(!autoDismissal, autoDismissal).forEach { isChecked ->
                SettingsPageScreen.Missions.autoDismissalButton.click()
                testMissionsSubMenuAutoDismissal(autoDismissalSeconds, isChecked, !autoDismissal)
            }
        }

        fun TestContext<Unit>.testMissionsSubMenuRewards(
            rewardsEnabled: BooleanArray,
            shouldTest: Boolean,
        ) {
            SettingsPageScreen.Missions {
                step("Reward setting components displayed") {
                    rewardsDivider.scrollTo()
                    rewardsTitle.isDisplayedWithText(R.string.displayed_rewards)
                    rewardsAllButton.isDisplayedWithText(R.string.all)
                    rewardsNoneButton.isDisplayedWithText(R.string.none)
                    rewardSettings.forEach { setting ->
                        setting.button.isDisplayedWithText(setting.text)
                    }
                }

                scenario(
                    AllAndNoneSettingsScenario(
                        allButton = rewardsAllButton,
                        noneButton = rewardsNoneButton,
                        settingsButtons =
                            rewardSettings.mapIndexed { index, setting ->
                                setting.button to rewardsEnabled[index]
                            },
                        shouldTest = shouldTest,
                    )
                )
            }
        }

        fun TestContext<Unit>.testMissionsSubMenuAutoDismissal(
            seconds: Int,
            isOn: Boolean,
            shouldTestTime: Boolean,
        ) {
            val stepNameBase = "Auto-dismissal time input components"
            SettingsPageScreen.Missions {
                if (isOn) {
                    step("$stepNameBase displayed") {
                        autoDismissalButton.isChecked()
                        autoDismissalSecondsLabel.isDisplayedWithText(R.string.seconds)
                        autoDismissalTimeInput.isDisplayed(withMinutes = false)
                    }

                    if (shouldTestTime) {
                        scenario(TimeInputTestScenario(autoDismissalTimeInput, seconds, false))
                    }
                } else {
                    step("$stepNameBase not displayed") {
                        autoDismissalButton.isNotChecked()
                        autoDismissalSecondsLabel.isHidden()
                        autoDismissalTimeInput.isHidden()
                    }
                }
            }
        }

        fun TestContext<Unit>.testMissionsSubMenuDisableFromMenu() {
            step("Deactivate submenu from main menu") {
                SettingsPageScreen.deactivateSubmenu(ENTRY_INDEX)
            }

            step("Submenu should not have been opened") { testScreenClosed(false) }
        }

        fun TestContext<Unit>.testScreenClosed(isToggleOn: Boolean) {
            SettingsPageScreen.Missions {
                rewardsTitle.doesNotExist()
                rewardsAllButton.doesNotExist()
                rewardsNoneButton.doesNotExist()
                rewardSettings.forEach { it.button.doesNotExist() }
                rewardsDivider.doesNotExist()
                autoDismissalButton.doesNotExist()
                autoDismissalTitle.doesNotExist()
                autoDismissalTimeInput.doesNotExist()
                autoDismissalSecondsLabel.doesNotExist()
                autoDismissalDivider.doesNotExist()
            }

            flakySafely { SettingsPageScreen.Menu.testToggleState(ENTRY_INDEX, isToggleOn) }
        }
    }
}
