package artemis.agent.setup.settings

import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewbinding.ViewBindings
import artemis.agent.R
import artemis.agent.copy
import artemis.agent.databinding.SettingsRoutingBinding
import artemis.agent.mockkViewBinding
import artemis.agent.userSettings
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll

class IncentiveSettingTest :
    DescribeSpec({
        describe("IncentiveSetting") {
            mockkStatic(ViewBindings::class)

            mockkViewBinding<ToggleButton>(R.id.incentivesMissionsButton, 0)
            mockkViewBinding<ToggleButton>(R.id.incentivesNeedsDamConButton, 1)
            mockkViewBinding<ToggleButton>(R.id.incentivesNeedsEnergyButton, 2)
            mockkViewBinding<ToggleButton>(R.id.incentivesHasEnergyButton, 3)
            mockkViewBinding<ToggleButton>(R.id.incentivesMalfunctionButton, 4)
            mockkViewBinding<ToggleButton>(R.id.incentivesAmbassadorButton, 5)
            mockkViewBinding<ToggleButton>(R.id.incentivesHostageButton, 6)
            mockkViewBinding<ToggleButton>(R.id.incentivesCommandeeredButton, 7)

            mockkViewBinding<Button>(R.id.incentivesAllButton)
            mockkViewBinding<Button>(R.id.incentivesNoneButton)
            mockkViewBinding<TextView>(R.id.incentivesTitle)
            mockkViewBinding<View>(R.id.incentivesDivider)

            mockkViewBinding<Button>(R.id.avoidancesAllButton)
            mockkViewBinding<Button>(R.id.avoidancesNoneButton)
            mockkViewBinding<TextView>(R.id.avoidancesTitle)
            mockkViewBinding<View>(R.id.avoidancesDivider)
            mockkViewBinding<ToggleButton>(R.id.blackHolesButton)
            mockkViewBinding<ToggleButton>(R.id.minesButton)
            mockkViewBinding<ToggleButton>(R.id.typhonsButton)
            mockkViewBinding<ToggleButton>(R.id.blackHolesClearanceField)
            mockkViewBinding<ToggleButton>(R.id.minesClearanceField)
            mockkViewBinding<ToggleButton>(R.id.typhonsClearanceField)
            mockkViewBinding<ToggleButton>(R.id.blackHolesClearanceKm)
            mockkViewBinding<ToggleButton>(R.id.minesClearanceKm)
            mockkViewBinding<ToggleButton>(R.id.typhonsClearanceKm)
            mockkViewBinding<TextView>(R.id.blackHolesTitle)
            mockkViewBinding<TextView>(R.id.minesTitle)
            mockkViewBinding<TextView>(R.id.typhonsTitle)

            val mockBinding = SettingsRoutingBinding.bind(mockk<ConstraintLayout>())

            afterSpec {
                clearAllMocks()
                unmockkAll()
            }

            describe("Toggle buttons") {
                withData(IncentiveSetting.entries) { entry ->
                    entry.getButton(mockBinding).id shouldBeEqual entry.ordinal
                }
            }

            describe("User settings") {
                val baseSettings = userSettings {
                    routeMissions = false
                    routeNeedsDamcon = false
                    routeNeedsEnergy = false
                    routeHasEnergy = false
                    routeMalfunction = false
                    routeAmbassador = false
                    routeHostage = false
                    routeCommandeered = false
                }

                val copies =
                    listOf(
                        baseSettings.copy { routeMissions = true },
                        baseSettings.copy { routeNeedsDamcon = true },
                        baseSettings.copy { routeNeedsEnergy = true },
                        baseSettings.copy { routeHasEnergy = true },
                        baseSettings.copy { routeMalfunction = true },
                        baseSettings.copy { routeAmbassador = true },
                        baseSettings.copy { routeHostage = true },
                        baseSettings.copy { routeCommandeered = true },
                    )

                describe("Get") {
                    withData(IncentiveSetting.entries) { entry ->
                        it("Off") { entry.isChecked(baseSettings).shouldBeFalse() }

                        it("On") { entry.isChecked(copies[entry.ordinal]).shouldBeTrue() }
                    }
                }

                describe("Set") {
                    withData(IncentiveSetting.entries) { entry ->
                        var currentSettings = baseSettings

                        withData(nameFn = { it.first }, "On" to true, "Off" to false) {
                            (_, isChecked) ->
                            currentSettings =
                                currentSettings.copy { entry.onCheckedChanged(this, isChecked) }
                            entry.isChecked(currentSettings) shouldBeEqual isChecked
                        }
                    }
                }
            }
        }
    })
