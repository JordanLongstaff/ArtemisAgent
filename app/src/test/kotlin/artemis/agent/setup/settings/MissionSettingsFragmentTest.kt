package artemis.agent.setup.settings

import androidx.activity.viewModels
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.AgentViewModel
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.game.missions.RewardType
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
@LargeTest
class MissionSettingsFragmentTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun missionSettingsTest() {
        val missionsEnabled = AtomicBoolean()
        val autoDismissal = AtomicBoolean()
        val rewardsEnabled = Array(RewardType.entries.size) { AtomicBoolean() }
        activityScenarioRule.scenario.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value
            missionsEnabled.lazySet(viewModel.missionsEnabled)
            autoDismissal.lazySet(viewModel.autoDismissCompletedMissions)

            viewModel.displayedRewards.forEach {
                rewardsEnabled[it.ordinal].lazySet(true)
            }
        }

        SettingsFragmentTest.openSettingsMenu()

        val enabled = missionsEnabled.get()
        val autoDismissalOn = autoDismissal.get()
        val rewardValues = rewardsEnabled.map { it.get() }.toBooleanArray()

        booleanArrayOf(!enabled, enabled).forEach { usingToggle ->
            SettingsFragmentTest.openSettingsSubMenu(2, usingToggle, true)
            testMissionsSubMenuOpen(autoDismissalOn, rewardValues, !usingToggle)

            SettingsFragmentTest.closeSettingsSubMenu(!usingToggle)
            testMissionsSubMenuClosed()
        }
    }

    private companion object {
        val rewardButtonIDs = intArrayOf(
            R.id.rewardsBatteryButton,
            R.id.rewardsCoolantButton,
            R.id.rewardsNukeButton,
            R.id.rewardsProductionButton,
            R.id.rewardsShieldButton,
        )

        val rewardButtonLabels = intArrayOf(
            R.string.mission_battery,
            R.string.mission_coolant,
            R.string.mission_nuke,
            R.string.mission_production,
            R.string.mission_shield,
        )

        fun testMissionsSubMenuOpen(
            autoDismissal: Boolean,
            rewardsEnabled: BooleanArray,
            shouldTestRewards: Boolean,
        ) {
            testMissionsSubMenuRewards(rewardsEnabled, shouldTestRewards)
            testMissionsSubMenuAutoDismissal(autoDismissal)
        }

        fun testMissionsSubMenuRewards(rewardsEnabled: BooleanArray, shouldTest: Boolean) {
            scrollTo(R.id.rewardsDivider)
            assertDisplayed(R.id.rewardsTitle, R.string.displayed_rewards)
            assertDisplayed(R.id.rewardsAllButton, R.string.all)
            assertDisplayed(R.id.rewardsNoneButton, R.string.none)

            rewardButtonIDs.forEachIndexed { index, id ->
                assertDisplayed(id, rewardButtonLabels[index])
                artemis.agent.ArtemisAgentTestHelpers.assertChecked(id, rewardsEnabled[index])
            }

            if (rewardsEnabled.all { it }) {
                SettingsFragmentTest.testAllEnabled(
                    R.id.rewardsAllButton,
                    R.id.rewardsNoneButton,
                    rewardButtonIDs,
                    !shouldTest,
                )
            } else {
                SettingsFragmentTest.testNotAllEnabled(
                    R.id.rewardsAllButton,
                    R.id.rewardsNoneButton,
                    rewardButtonIDs,
                    rewardsEnabled,
                    !shouldTest,
                )
            }
        }

        fun testMissionsSubMenuAutoDismissal(autoDismissal: Boolean) {
            scrollTo(R.id.autoDismissalDivider)
            assertDisplayed(R.id.autoDismissalTitle, R.string.auto_dismissal)
            assertDisplayed(R.id.autoDismissalButton)

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

        fun testMissionsSubMenuClosed() {
            assertNotExist(R.id.rewardsTitle)
            assertNotExist(R.id.rewardsAllButton)
            assertNotExist(R.id.rewardsNoneButton)
            rewardButtonIDs.forEach { assertNotExist(it) }
            assertNotExist(R.id.rewardsDivider)
            assertNotExist(R.id.autoDismissalButton)
            assertNotExist(R.id.autoDismissalTitle)
            assertNotExist(R.id.autoDismissalTimeInput)
            assertNotExist(R.id.autoDismissalSecondsLabel)
            assertNotExist(R.id.autoDismissalDivider)
        }
    }
}
