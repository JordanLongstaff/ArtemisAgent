package artemis.agent.setup.settings

import androidx.activity.viewModels
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

    private companion object {
        val routingIncentiveButtons = intArrayOf(
            R.id.incentivesNeedsEnergyButton,
            R.id.incentivesNeedsDamConButton,
            R.id.incentivesMalfunctionButton,
            R.id.incentivesAmbassadorButton,
            R.id.incentivesHostageButton,
            R.id.incentivesCommandeeredButton,
            R.id.incentivesHasEnergyButton,
            R.id.incentivesMissionsButton,
        )

        val routingIncentiveLabels = intArrayOf(
            R.string.route_incentive_needs_energy,
            R.string.route_incentive_needs_damcon,
            R.string.route_incentive_malfunction,
            R.string.route_incentive_ambassador,
            R.string.route_incentive_hostage,
            R.string.route_incentive_commandeered,
            R.string.route_incentive_has_energy,
            R.string.route_incentive_missions,
        )

        val routingAvoidanceButtons = intArrayOf(
            R.id.blackHolesButton,
            R.id.minesButton,
            R.id.typhonsButton,
        )

        val routingAvoidanceLabels = intArrayOf(
            R.id.blackHolesTitle,
            R.id.minesTitle,
            R.id.typhonsTitle,
        )

        val routingAvoidanceTitles = intArrayOf(
            R.string.avoidance_black_hole,
            R.string.avoidance_mine,
            R.string.avoidance_typhon,
        )

        val routingAvoidanceFields = intArrayOf(
            R.id.blackHolesClearanceField,
            R.id.minesClearanceField,
            R.id.typhonsClearanceField,
        )

        val routingAvoidanceKm = intArrayOf(
            R.id.blackHolesClearanceKm,
            R.id.minesClearanceKm,
            R.id.typhonsClearanceKm,
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

            routingIncentiveButtons.forEachIndexed { index, id ->
                assertDisplayed(id, routingIncentiveLabels[index])
                artemis.agent.ArtemisAgentTestHelpers.assertChecked(id, incentives[index])
            }

            if (incentives.all { it }) {
                SettingsFragmentTest.testAllEnabled(
                    R.id.incentivesAllButton,
                    R.id.incentivesNoneButton,
                    routingIncentiveButtons,
                    !shouldTest,
                )
            } else {
                SettingsFragmentTest.testNotAllEnabled(
                    R.id.incentivesAllButton,
                    R.id.incentivesNoneButton,
                    routingIncentiveButtons,
                    incentives,
                    !shouldTest,
                )
            }
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

            routingAvoidanceButtons.forEachIndexed { index, button ->
                assertDisplayed(routingAvoidanceLabels[index], routingAvoidanceTitles[index])
                assertDisplayed(button)

                testRoutingSubMenuAvoidance(index, enabled[index], clearances[index])

                if (!shouldTest) return@forEachIndexed

                clickOn(button)
                testRoutingSubMenuAvoidance(index, !enabled[index], clearances[index])
                clickOn(button)
                testRoutingSubMenuAvoidance(index, enabled[index], clearances[index])
            }

            if (enabled.all { it }) {
                SettingsFragmentTest.testAllEnabled(
                    R.id.avoidancesAllButton,
                    R.id.avoidancesNoneButton,
                    routingAvoidanceButtons,
                    !shouldTest,
                )
            } else {
                SettingsFragmentTest.testNotAllEnabled(
                    R.id.avoidancesAllButton,
                    R.id.avoidancesNoneButton,
                    routingAvoidanceButtons,
                    enabled,
                    !shouldTest,
                ) { index, on ->
                    if (on) {
                        assertDisplayed(routingAvoidanceFields[index], clearances[index].toString())
                        assertDisplayed(routingAvoidanceKm[index], R.string.kilometres)
                    } else {
                        assertNotDisplayed(routingAvoidanceFields[index])
                        assertNotDisplayed(routingAvoidanceKm[index])
                    }
                }
            }
        }

        fun testRoutingSubMenuAvoidance(index: Int, isEnabled: Boolean, clearance: Int) {
            val field = routingAvoidanceFields[index]
            val kmLabel = routingAvoidanceKm[index]
            val button = routingAvoidanceButtons[index]

            if (isEnabled) {
                assertChecked(button)
                assertDisplayed(field, clearance.toString())
                assertDisplayed(kmLabel, R.string.kilometres)
            } else {
                assertUnchecked(button)
                assertNotDisplayed(field)
                assertNotDisplayed(kmLabel)
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

            listOf(
                routingIncentiveButtons,
                routingAvoidanceLabels,
                routingAvoidanceButtons,
                routingAvoidanceFields,
                routingAvoidanceKm,
            ).forEach { list -> list.forEach { id -> assertNotExist(id) } }
        }
    }
}
