package artemis.agent.setup.settings

import android.Manifest
import androidx.activity.viewModels
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.ActivityScenarioManager
import artemis.agent.AgentViewModel
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.game.missions.RewardType
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo
import com.adevinta.android.barista.interaction.PermissionGranter
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MissionSettingsFragmentTest {
    @get:Rule val activityScenarioManager = ActivityScenarioManager.forActivity<MainActivity>()

    @Test
    fun missionSettingsMutableTest() {
        testWithSettings { data ->
            booleanArrayOf(true, false).forEach { testSettings ->
                data.testMenu(
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

            data.testMenu(
                openWithToggle = true,
                testSettings = false,
                closeWithToggle = false,
                closeWithBack = true,
            )

            if (!data.enabled) testMissionsSubMenuDisableFromMenu()
        }
    }

    private fun testWithSettings(test: (Data) -> Unit) {
        val missionsEnabled = AtomicBoolean()
        val autoDismissal = AtomicBoolean()
        val rewardsEnabled = Array(RewardType.entries.size) { AtomicBoolean() }

        activityScenarioManager.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value
            val missionManager = viewModel.missionManager

            missionsEnabled.lazySet(missionManager.enabled)
            autoDismissal.lazySet(missionManager.autoDismissCompletedMissions)

            missionManager.displayedRewards.forEach { rewardsEnabled[it.ordinal].lazySet(true) }
        }

        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

        SettingsFragmentTest.openSettingsMenu()

        val enabled = missionsEnabled.get()
        val autoDismissalOn = autoDismissal.get()

        val rewardSettings =
            RewardSettings(
                battery = rewardsEnabled[RewardType.BATTERY.ordinal].get(),
                coolant = rewardsEnabled[RewardType.COOLANT.ordinal].get(),
                nuke = rewardsEnabled[RewardType.NUKE.ordinal].get(),
                production = rewardsEnabled[RewardType.PRODUCTION.ordinal].get(),
                shield = rewardsEnabled[RewardType.SHIELD.ordinal].get(),
            )

        test(Data(enabled, autoDismissalOn, rewardSettings))
    }

    private data class Data(
        val enabled: Boolean,
        val autoDismissal: Boolean,
        val rewards: RewardSettings,
    ) {
        fun testMenu(
            openWithToggle: Boolean,
            testSettings: Boolean,
            closeWithToggle: Boolean,
            closeWithBack: Boolean,
        ) {
            SettingsFragmentTest.openSettingsSubMenu(ENTRY_INDEX, openWithToggle, true)
            testMissionsSubMenuOpen(autoDismissal, rewards.toArray(), testSettings)

            val isToggleOn =
                if (closeWithBack) {
                    SettingsFragmentTest.backFromSubMenu()
                    true
                } else {
                    SettingsFragmentTest.closeSettingsSubMenu(closeWithToggle)
                    !closeWithToggle
                }
            testMissionsSubMenuClosed(isToggleOn)
        }
    }

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

        val rewardSettings =
            arrayOf(
                GroupedToggleButtonSetting(R.id.rewardsBatteryButton, R.string.mission_battery),
                GroupedToggleButtonSetting(R.id.rewardsCoolantButton, R.string.mission_coolant),
                GroupedToggleButtonSetting(R.id.rewardsNukeButton, R.string.mission_nuke),
                GroupedToggleButtonSetting(
                    R.id.rewardsProductionButton,
                    R.string.mission_production,
                ),
                GroupedToggleButtonSetting(R.id.rewardsShieldButton, R.string.mission_shield),
            )

        fun testMissionsSubMenuOpen(
            autoDismissal: Boolean,
            rewardsEnabled: BooleanArray,
            shouldTestSettings: Boolean,
        ) {
            testMissionsSubMenuRewards(rewardsEnabled, shouldTestSettings)
            testMissionsSubMenuAutoDismissal(autoDismissal, shouldTestSettings)
        }

        fun testMissionsSubMenuRewards(rewardsEnabled: BooleanArray, shouldTest: Boolean) {
            scrollTo(R.id.rewardsDivider)
            assertDisplayed(R.id.rewardsTitle, R.string.displayed_rewards)
            assertDisplayed(R.id.rewardsAllButton, R.string.all)
            assertDisplayed(R.id.rewardsNoneButton, R.string.none)

            rewardSettings.forEach { assertDisplayed(it.button, it.text) }

            SettingsFragmentTest.testSettingsWithAllAndNone(
                allButton = R.id.rewardsAllButton,
                noneButton = R.id.rewardsNoneButton,
                settingsButtons =
                    rewardSettings.mapIndexed { index, setting ->
                        setting.button to rewardsEnabled[index]
                    },
                skipToggleTest = !shouldTest,
            )
        }

        fun testMissionsSubMenuAutoDismissal(isOn: Boolean, shouldTest: Boolean) {
            scrollTo(R.id.autoDismissalDivider)
            assertDisplayed(R.id.autoDismissalTitle, R.string.auto_dismissal)
            assertDisplayed(R.id.autoDismissalButton)

            testMissionsSubMenuAutoDismissalButtons(isOn)

            if (!shouldTest) return

            booleanArrayOf(!isOn, isOn).forEach { isChecked ->
                clickOn(R.id.autoDismissalButton)
                testMissionsSubMenuAutoDismissalButtons(isChecked)
            }
        }

        fun testMissionsSubMenuAutoDismissalButtons(isOn: Boolean) {
            if (isOn) {
                assertChecked(R.id.autoDismissalButton)
                assertDisplayed(R.id.autoDismissalSecondsLabel, R.string.seconds)
                assertDisplayed(R.id.autoDismissalTimeInput)
            } else {
                assertUnchecked(R.id.autoDismissalButton)
                assertNotDisplayed(R.id.autoDismissalSecondsLabel)
                assertNotDisplayed(R.id.autoDismissalTimeInput)
            }
        }

        fun testMissionsSubMenuClosed(isToggleOn: Boolean) {
            assertNotExist(R.id.rewardsTitle)
            assertNotExist(R.id.rewardsAllButton)
            assertNotExist(R.id.rewardsNoneButton)
            rewardSettings.forEach { assertNotExist(it.button) }
            assertNotExist(R.id.rewardsDivider)
            assertNotExist(R.id.autoDismissalButton)
            assertNotExist(R.id.autoDismissalTitle)
            assertNotExist(R.id.autoDismissalTimeInput)
            assertNotExist(R.id.autoDismissalSecondsLabel)
            assertNotExist(R.id.autoDismissalDivider)

            SettingsFragmentTest.assertSettingsMenuEntryToggleState(ENTRY_INDEX, isToggleOn)
        }

        fun testMissionsSubMenuDisableFromMenu() {
            SettingsFragmentTest.toggleSettingsSubMenu(ENTRY_INDEX)
            testMissionsSubMenuClosed(false)
        }
    }
}
