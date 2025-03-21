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
    fun missionSettingsTest() {
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
        val rewardValues = rewardsEnabled.map { it.get() }.toBooleanArray()

        booleanArrayOf(!enabled, enabled).forEach { usingToggle ->
            SettingsFragmentTest.openSettingsSubMenu(ENTRY_INDEX, usingToggle, true)
            testMissionsSubMenuOpen(autoDismissalOn, rewardValues, !usingToggle)

            SettingsFragmentTest.closeSettingsSubMenu(!usingToggle)
            testMissionsSubMenuClosed(usingToggle)

            if (usingToggle) {
                SettingsFragmentTest.openSettingsSubMenu(
                    index = ENTRY_INDEX,
                    usingToggle = false,
                    toggleDisplayed = true,
                )
                testMissionsSubMenuOpen(autoDismissalOn, rewardValues, false)

                SettingsFragmentTest.backFromSubMenu()
                testMissionsSubMenuClosed(true)
            }
        }
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

        fun testMissionsSubMenuAutoDismissal(autoDismissal: Boolean, shouldTestToggle: Boolean) {
            scrollTo(R.id.autoDismissalDivider)
            assertDisplayed(R.id.autoDismissalTitle, R.string.auto_dismissal)
            assertDisplayed(R.id.autoDismissalButton)

            testMissionsSubMenuAutoDismissal(autoDismissal)

            if (!shouldTestToggle) return

            booleanArrayOf(!autoDismissal, autoDismissal).forEach { isChecked ->
                clickOn(R.id.autoDismissalButton)
                testMissionsSubMenuAutoDismissal(isChecked)
            }
        }

        fun testMissionsSubMenuAutoDismissal(autoDismissal: Boolean) {
            if (autoDismissal) {
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
    }
}
