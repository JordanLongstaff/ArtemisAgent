package artemis.agent.setup.settings

import android.Manifest
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.ActivityScenarioManager
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
import com.adevinta.android.barista.interaction.PermissionGranter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class RoutingSettingsFragmentTest {
    @get:Rule val activityScenarioManager = ActivityScenarioManager.forActivity<MainActivity>()

    @Test
    fun routingSettingsMutableTest() {
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
    fun routingSettingsBackButtonTest() {
        testWithSettings { data ->
            if (data.enabled) testRoutingSubMenuDisableFromMenu()

            data.testMenu(
                openWithToggle = true,
                testSettings = false,
                closeWithToggle = false,
                closeWithBack = true,
            )

            if (!data.enabled) testRoutingSubMenuDisableFromMenu()
        }
    }

    private fun testWithSettings(test: (Data) -> Unit) {
        val routingEnabled = AtomicBoolean()

        val blackHoleAvoidance = AtomicBoolean()
        val mineAvoidance = AtomicBoolean()
        val typhonAvoidance = AtomicBoolean()

        val blackHoleClearance = AtomicInteger()
        val mineClearance = AtomicInteger()
        val typhonClearance = AtomicInteger()

        val incentives = Array(RouteTaskIncentive.entries.size + 1) { AtomicBoolean() }

        activityScenarioManager.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value
            routingEnabled.lazySet(viewModel.routingEnabled)

            blackHoleAvoidance.lazySet(viewModel.avoidBlackHoles)
            mineAvoidance.lazySet(viewModel.avoidMines)
            typhonAvoidance.lazySet(viewModel.avoidTyphons)

            arrayOf(
                    blackHoleClearance to viewModel.blackHoleClearance,
                    mineClearance to viewModel.mineClearance,
                    typhonClearance to viewModel.typhonClearance,
                )
                .forEach { (holder, value) -> holder.lazySet(value.toInt()) }

            viewModel.routeIncentives.forEach { incentive ->
                incentives[incentive.ordinal].lazySet(true)
            }
            incentives.last().lazySet(viewModel.routeIncludesMissions)
        }

        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

        SettingsFragmentTest.openSettingsMenu()

        val enabled = routingEnabled.get()

        val incentivesData =
            IncentivesData(
                needsEnergy = incentives[RouteTaskIncentive.NEEDS_ENERGY.ordinal].get(),
                needsDamCon = incentives[RouteTaskIncentive.NEEDS_DAMCON.ordinal].get(),
                malfunction = incentives[RouteTaskIncentive.RESET_COMPUTER.ordinal].get(),
                ambassador = incentives[RouteTaskIncentive.AMBASSADOR_PICKUP.ordinal].get(),
                hostage = incentives[RouteTaskIncentive.HOSTAGE.ordinal].get(),
                commandeered = incentives[RouteTaskIncentive.COMMANDEERED.ordinal].get(),
                hasEnergy = incentives[RouteTaskIncentive.HAS_ENERGY.ordinal].get(),
                hasMissions = incentives.last().get(),
            )

        val avoidanceData =
            AvoidanceData(
                blackHolesEnabled = blackHoleAvoidance.get(),
                blackHolesClearance = blackHoleClearance.get(),
                minesEnabled = mineAvoidance.get(),
                minesClearance = mineClearance.get(),
                typhonsEnabled = typhonAvoidance.get(),
                typhonsClearance = typhonClearance.get(),
            )

        test(Data(enabled, incentivesData, avoidanceData))
    }

    private data class Data(
        val enabled: Boolean,
        val incentives: IncentivesData,
        val avoidances: AvoidanceData,
    ) {
        fun testMenu(
            openWithToggle: Boolean,
            testSettings: Boolean,
            closeWithToggle: Boolean,
            closeWithBack: Boolean,
        ) {
            SettingsFragmentTest.openSettingsSubMenu(ENTRY_INDEX, openWithToggle, true)
            testRoutingSubMenuOpen(incentives, avoidances, testSettings)

            val isToggleOn =
                if (closeWithBack) {
                    SettingsFragmentTest.backFromSubMenu()
                    true
                } else {
                    SettingsFragmentTest.closeSettingsSubMenu(closeWithToggle)
                    !closeWithToggle
                }
            testRoutingSubMenuClosed(isToggleOn)
        }
    }

    private data class IncentivesData(
        val needsEnergy: Boolean,
        val needsDamCon: Boolean,
        val malfunction: Boolean,
        val ambassador: Boolean,
        val hostage: Boolean,
        val commandeered: Boolean,
        val hasEnergy: Boolean,
        val hasMissions: Boolean,
    ) {
        private val array by lazy {
            booleanArrayOf(
                needsEnergy,
                needsDamCon,
                malfunction,
                ambassador,
                hostage,
                commandeered,
                hasEnergy,
                hasMissions,
            )
        }

        fun toArray(): BooleanArray = array
    }

    private data class AvoidanceData(
        val blackHolesEnabled: Boolean,
        val blackHolesClearance: Int,
        val minesEnabled: Boolean,
        val minesClearance: Int,
        val typhonsEnabled: Boolean,
        val typhonsClearance: Int,
    ) {
        val arrayEnabled by lazy { booleanArrayOf(blackHolesEnabled, minesEnabled, typhonsEnabled) }
        val clearances by lazy { intArrayOf(blackHolesClearance, minesClearance, typhonsClearance) }
    }

    private data class RoutingAvoidanceSetting(
        @IdRes val button: Int,
        @IdRes val label: Int,
        @StringRes val text: Int,
        @IdRes val input: Int,
        @IdRes val kmLabel: Int,
    )

    private companion object {
        const val ENTRY_INDEX = 6

        val routingIncentiveSettings by lazy {
            arrayOf(
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
        }

        val routingAvoidanceSettings by lazy {
            arrayOf(
                RoutingAvoidanceSetting(
                    button = R.id.blackHolesButton,
                    label = R.id.blackHolesTitle,
                    text = R.string.avoidance_black_hole,
                    input = R.id.blackHolesClearanceField,
                    kmLabel = R.id.blackHolesClearanceKm,
                ),
                RoutingAvoidanceSetting(
                    button = R.id.minesButton,
                    label = R.id.minesTitle,
                    text = R.string.avoidance_mine,
                    input = R.id.minesClearanceField,
                    kmLabel = R.id.minesClearanceKm,
                ),
                RoutingAvoidanceSetting(
                    button = R.id.typhonsButton,
                    label = R.id.typhonsTitle,
                    text = R.string.avoidance_typhon,
                    input = R.id.typhonsClearanceField,
                    kmLabel = R.id.typhonsClearanceKm,
                ),
            )
        }

        fun testRoutingSubMenuOpen(
            incentives: IncentivesData,
            avoidances: AvoidanceData,
            shouldTestSettings: Boolean,
        ) {
            testRoutingSubMenuIncentives(incentives, shouldTestSettings)
            testRoutingSubMenuAvoidances(avoidances, shouldTestSettings)
        }

        fun testRoutingSubMenuIncentives(incentives: IncentivesData, shouldTest: Boolean) {
            scrollTo(R.id.incentivesDivider)
            assertDisplayed(R.id.incentivesTitle, R.string.included_incentives)
            assertDisplayed(R.id.incentivesAllButton, R.string.all)
            assertDisplayed(R.id.incentivesNoneButton, R.string.none)

            val incentivesArray = incentives.toArray()
            routingIncentiveSettings.forEach { assertDisplayed(it.button, it.text) }

            SettingsFragmentTest.testSettingsWithAllAndNone(
                allButton = R.id.incentivesAllButton,
                noneButton = R.id.incentivesNoneButton,
                settingsButtons =
                    routingIncentiveSettings.mapIndexed { index, setting ->
                        setting.button to incentivesArray[index]
                    },
                skipToggleTest = !shouldTest,
            )
        }

        fun testRoutingSubMenuAvoidances(data: AvoidanceData, shouldTest: Boolean) {
            scrollTo(R.id.avoidancesDivider)
            assertDisplayed(R.id.avoidancesTitle, R.string.avoidances)
            assertDisplayed(R.id.avoidancesAllButton, R.string.all)
            assertDisplayed(R.id.avoidancesNoneButton, R.string.none)

            val enabled = data.arrayEnabled
            val clearances = data.clearances

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
                allButton = R.id.avoidancesAllButton,
                noneButton = R.id.avoidancesNoneButton,
                settingsButtons =
                    routingAvoidanceSettings.mapIndexed { index, setting ->
                        setting.button to enabled[index]
                    },
                skipToggleTest = !shouldTest,
            ) { index, on ->
                if (on) {
                    assertDisplayed(
                        routingAvoidanceSettings[index].input,
                        clearances[index].toString(),
                    )
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

        fun testRoutingSubMenuClosed(isToggleOn: Boolean) {
            assertNotExist(R.id.incentivesTitle)
            assertNotExist(R.id.incentivesAllButton)
            assertNotExist(R.id.incentivesNoneButton)
            assertNotExist(R.id.incentivesDivider)

            assertNotExist(R.id.avoidancesTitle)
            assertNotExist(R.id.avoidancesAllButton)
            assertNotExist(R.id.avoidancesNoneButton)
            assertNotExist(R.id.avoidancesDivider)

            routingIncentiveSettings.forEach { assertNotExist(it.button) }
            routingAvoidanceSettings.forEach { avoidanceSetting ->
                assertNotExist(avoidanceSetting.button)
                assertNotExist(avoidanceSetting.label)
                assertNotExist(avoidanceSetting.input)
                assertNotExist(avoidanceSetting.kmLabel)
            }

            SettingsFragmentTest.assertSettingsMenuEntryToggleState(ENTRY_INDEX, isToggleOn)
        }

        fun testRoutingSubMenuDisableFromMenu() {
            SettingsFragmentTest.toggleSettingsSubMenu(ENTRY_INDEX)
            testRoutingSubMenuClosed(false)
        }
    }
}
