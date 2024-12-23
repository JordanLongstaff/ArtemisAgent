package artemis.agent.setup.settings

import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.AgentViewModel
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.game.route.RouteTaskIncentive
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
class RoutingSettingsFragmentTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun routingSettingsTest() {
        val routingEnabled = AtomicBoolean()

        val avoidances = Array(3) { AtomicBoolean() }
        val clearances = Array(3) { AtomicInteger() }
        val incentives = Array(RouteTaskIncentive.entries.size + 1) { AtomicBoolean() }

        activityScenarioRule.scenario.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value
            routingEnabled.lazySet(viewModel.routingEnabled)

            arrayOf(
                viewModel.avoidBlackHoles,
                viewModel.avoidMines,
                viewModel.avoidTyphons,
            ).forEachIndexed { index, avoid -> avoidances[index].lazySet(avoid) }

            arrayOf(
                viewModel.blackHoleClearance,
                viewModel.mineClearance,
                viewModel.typhonClearance,
            ).forEachIndexed { index, clearance -> clearances[index].lazySet(clearance.toInt()) }

            viewModel.routeIncentives.forEach { incentive ->
                incentives[incentive.ordinal].lazySet(true)
            }
            incentives.last().lazySet(viewModel.routeIncludesMissions)
        }

        SettingsFragmentTest.openSettingsMenu()

        val enabled = routingEnabled.get()

        val incentivesEnabled = incentives.map { it.get() }.toBooleanArray()
        val avoidancesEnabled = avoidances.map { it.get() }.toBooleanArray()
        val clearanceValues = clearances.map { it.get() }.toIntArray()

        booleanArrayOf(!enabled, enabled).forEach { usingToggle ->
            SettingsFragmentTest.openSettingsSubMenu(6, usingToggle, true)
            testRoutingSubMenuOpen(
                incentivesEnabled,
                avoidancesEnabled,
                clearanceValues,
                !usingToggle,
            )

            SettingsFragmentTest.closeSettingsSubMenu(!usingToggle)
            testRoutingSubMenuClosed()
        }
    }

    class RoutingAvoidanceSetting(
        @IdRes val button: Int,
        @IdRes val label: Int,
        @StringRes val text: Int,
        @IdRes val input: Int,
        @IdRes val kmLabel: Int,
    )

    private companion object {
        val routingIncentiveSettings = arrayOf(
            GroupedToggleButtonSetting(
                R.id.incentivesNeedsEnergyButton,
                R.string.route_incentive_needs_energy,
            ),
            GroupedToggleButtonSetting(
                R.id.incentivesNeedsDamConButton,
                R.string.route_incentive_needs_damcon,
            ),
            GroupedToggleButtonSetting(
                R.id.incentivesMalfunctionButton,
                R.string.route_incentive_malfunction,
            ),
            GroupedToggleButtonSetting(
                R.id.incentivesAmbassadorButton,
                R.string.route_incentive_ambassador,
            ),
            GroupedToggleButtonSetting(
                R.id.incentivesHostageButton,
                R.string.route_incentive_hostage,
            ),
            GroupedToggleButtonSetting(
                R.id.incentivesCommandeeredButton,
                R.string.route_incentive_commandeered,
            ),
            GroupedToggleButtonSetting(
                R.id.incentivesHasEnergyButton,
                R.string.route_incentive_has_energy,
            ),
            GroupedToggleButtonSetting(
                R.id.incentivesMissionsButton,
                R.string.route_incentive_missions,
            ),
        )

        val routingAvoidanceSettings = arrayOf(
            RoutingAvoidanceSetting(
                R.id.blackHolesButton,
                R.id.blackHolesTitle,
                R.string.avoidance_black_hole,
                R.id.blackHolesClearanceField,
                R.id.blackHolesClearanceKm,
            ),
            RoutingAvoidanceSetting(
                R.id.minesButton,
                R.id.minesTitle,
                R.string.avoidance_mine,
                R.id.minesClearanceField,
                R.id.minesClearanceKm,
            ),
            RoutingAvoidanceSetting(
                R.id.typhonsButton,
                R.id.typhonsTitle,
                R.string.avoidance_typhon,
                R.id.typhonsClearanceField,
                R.id.typhonsClearanceKm,
            ),
        )

        fun testRoutingSubMenuOpen(
            incentives: BooleanArray,
            avoidances: BooleanArray,
            clearances: IntArray,
            shouldTestSettings: Boolean,
        ) {
            testRoutingSubMenuIncentives(incentives, shouldTestSettings)
            testRoutingSubMenuAvoidances(avoidances, clearances, shouldTestSettings)
        }

        fun testRoutingSubMenuIncentives(incentives: BooleanArray, shouldTest: Boolean) {
            scrollTo(R.id.incentivesDivider)
            assertDisplayed(R.id.incentivesTitle, R.string.included_incentives)
            assertDisplayed(R.id.incentivesAllButton, R.string.all)
            assertDisplayed(R.id.incentivesNoneButton, R.string.none)

            routingIncentiveSettings.forEach { assertDisplayed(it.button, it.text) }

            SettingsFragmentTest.testSettingsWithAllAndNone(
                R.id.incentivesAllButton,
                R.id.incentivesNoneButton,
                routingIncentiveSettings.mapIndexed { index, setting ->
                    setting.button to incentives[index]
                },
                !shouldTest,
            )
        }

        fun testRoutingSubMenuAvoidances(
            enabled: BooleanArray,
            clearances: IntArray,
            shouldTest: Boolean,
        ) {
            scrollTo(R.id.avoidancesDivider)
            assertDisplayed(R.id.avoidancesTitle, R.string.avoidances)
            assertDisplayed(R.id.avoidancesAllButton, R.string.all)
            assertDisplayed(R.id.avoidancesNoneButton, R.string.none)

            routingAvoidanceSettings.forEachIndexed { index, setting ->
                assertDisplayed(setting.label, setting.text)
                assertDisplayed(setting.button)

                testRoutingSubMenuAvoidance(setting, enabled[index], clearances[index])

                if (!shouldTest) return@forEachIndexed

                clickOn(setting.button)
                testRoutingSubMenuAvoidance(setting, !enabled[index], clearances[index])
                clickOn(setting.button)
                testRoutingSubMenuAvoidance(setting, enabled[index], clearances[index])
            }

            SettingsFragmentTest.testSettingsWithAllAndNone(
                R.id.avoidancesAllButton,
                R.id.avoidancesNoneButton,
                routingAvoidanceSettings.mapIndexed { index, setting ->
                    setting.button to enabled[index]
                },
                !shouldTest,
            ) { index, on ->
                if (on) {
                    assertDisplayed(routingAvoidanceSettings[index].input, clearances[index].toString())
                    assertDisplayed(routingAvoidanceSettings[index].kmLabel, R.string.kilometres)
                } else {
                    assertNotDisplayed(routingAvoidanceSettings[index].input)
                    assertNotDisplayed(routingAvoidanceSettings[index].kmLabel)
                }
            }
        }

        fun testRoutingSubMenuAvoidance(
            setting: RoutingAvoidanceSetting,
            isEnabled: Boolean,
            clearance: Int,
        ) {
            if (isEnabled) {
                assertChecked(setting.button)
                assertDisplayed(setting.input, clearance.toString())
                assertDisplayed(setting.kmLabel, R.string.kilometres)
            } else {
                assertUnchecked(setting.button)
                assertNotDisplayed(setting.input)
                assertNotDisplayed(setting.kmLabel)
            }
        }

        fun testRoutingSubMenuClosed() {
            assertNotExist(R.id.incentivesTitle)
            assertNotExist(R.id.incentivesAllButton)
            assertNotExist(R.id.incentivesNoneButton)
            assertNotExist(R.id.incentivesDivider)

            assertNotExist(R.id.avoidancesTitle)
            assertNotExist(R.id.avoidancesAllButton)
            assertNotExist(R.id.avoidancesNoneButton)
            assertNotExist(R.id.avoidancesDivider)

            routingIncentiveSettings.forEach { assertNotExist(it.button) }
            routingAvoidanceSettings.forEach {
                assertNotExist(it.button)
                assertNotExist(it.label)
                assertNotExist(it.input)
                assertNotExist(it.kmLabel)
            }
        }
    }
}
