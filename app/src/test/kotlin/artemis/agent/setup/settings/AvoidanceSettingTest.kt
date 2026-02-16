package artemis.agent.setup.settings

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewbinding.ViewBindings
import artemis.agent.R
import artemis.agent.UserSettingsKt
import artemis.agent.UserSettingsOuterClass.UserSettings
import artemis.agent.copy
import artemis.agent.databinding.SettingsRoutingBinding
import artemis.agent.mockkViewBinding
import artemis.agent.userSettings
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.short
import io.kotest.property.checkAll
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll

class AvoidanceSettingTest :
    DescribeSpec({
        describe("AvoidanceSetting") {
            mockkStatic(ViewBindings::class)

            mockkViewBinding<ToggleButton>(R.id.blackHolesButton, 0)
            mockkViewBinding<ToggleButton>(R.id.minesButton, 1)
            mockkViewBinding<ToggleButton>(R.id.typhonsButton, 2)
            mockkViewBinding<EditText>(R.id.blackHolesClearanceField, 3)
            mockkViewBinding<EditText>(R.id.minesClearanceField, 4)
            mockkViewBinding<EditText>(R.id.typhonsClearanceField, 5)
            mockkViewBinding<TextView>(R.id.blackHolesClearanceKm, 6)
            mockkViewBinding<TextView>(R.id.minesClearanceKm, 7)
            mockkViewBinding<TextView>(R.id.typhonsClearanceKm, 8)

            mockkViewBinding<Button>(R.id.incentivesAllButton)
            mockkViewBinding<Button>(R.id.incentivesNoneButton)
            mockkViewBinding<ToggleButton>(R.id.incentivesMissionsButton)
            mockkViewBinding<ToggleButton>(R.id.incentivesNeedsDamConButton)
            mockkViewBinding<ToggleButton>(R.id.incentivesNeedsEnergyButton)
            mockkViewBinding<ToggleButton>(R.id.incentivesHasEnergyButton)
            mockkViewBinding<ToggleButton>(R.id.incentivesMalfunctionButton)
            mockkViewBinding<ToggleButton>(R.id.incentivesAmbassadorButton)
            mockkViewBinding<ToggleButton>(R.id.incentivesHostageButton)
            mockkViewBinding<ToggleButton>(R.id.incentivesCommandeeredButton)
            mockkViewBinding<TextView>(R.id.incentivesTitle)
            mockkViewBinding<View>(R.id.incentivesDivider)

            mockkViewBinding<Button>(R.id.avoidancesAllButton)
            mockkViewBinding<Button>(R.id.avoidancesNoneButton)
            mockkViewBinding<TextView>(R.id.avoidancesTitle)
            mockkViewBinding<View>(R.id.avoidancesDivider)
            mockkViewBinding<TextView>(R.id.blackHolesTitle)
            mockkViewBinding<TextView>(R.id.minesTitle)
            mockkViewBinding<TextView>(R.id.typhonsTitle)

            val mockBinding = SettingsRoutingBinding.bind(mockk<ConstraintLayout>())

            afterSpec {
                clearAllMocks()
                unmockkAll()
            }

            describe("Views") {
                withData(AvoidanceSetting.entries) { entry ->
                    withData(
                        nameFn = { it.first },
                        Triple("Toggle button", 0, entry::getToggleButton),
                        Triple("Number field", 3, entry::getClearanceField),
                        Triple("KM label", 6, entry::getKmLabel),
                    ) { (_, offset, getView) ->
                        getView(mockBinding).id.shouldBeEqual(entry.ordinal + offset)
                    }
                }
            }

            describe("User settings") {
                val baseSettings = userSettings {
                    avoidBlackHoles = false
                    avoidMines = false
                    avoidTyphon = false
                }

                val copies =
                    listOf(
                        baseSettings.copy { avoidBlackHoles = true },
                        baseSettings.copy { avoidMines = true },
                        baseSettings.copy { avoidTyphon = true },
                    )

                describe("Get enabled") {
                    withData(AvoidanceSetting.entries) { entry ->
                        it("Off") { entry.isEnabled(baseSettings).shouldBeFalse() }

                        it("On") { entry.isEnabled(copies[entry.ordinal]).shouldBeTrue() }
                    }
                }

                describe("Set enabled") {
                    withData(AvoidanceSetting.entries) { entry ->
                        var currentSettings = baseSettings

                        withData(nameFn = { it.first }, "On" to true, "Off" to false) {
                            (_, isChecked) ->
                            currentSettings =
                                currentSettings.copy { entry.setEnabled(this, isChecked) }
                            entry.isEnabled(currentSettings) shouldBeEqual isChecked
                        }
                    }
                }

                val arbClearance = Arb.short().map { it.toFloat() }

                describe("Get clearance") {
                    val clearanceSetters: List<UserSettingsKt.Dsl.(Float) -> Unit> =
                        listOf(
                            { clearance -> blackHoleClearance = clearance },
                            { clearance -> mineClearance = clearance },
                            { clearance -> typhonClearance = clearance },
                        )

                    withData(AvoidanceSetting.entries) { entry ->
                        arbClearance.checkAll { clearance ->
                            entry.getClearance(
                                baseSettings.copy { clearanceSetters[entry.ordinal](clearance) }
                            ) shouldBeEqual clearance
                        }
                    }
                }

                describe("Set clearance") {
                    val clearanceGetters: List<(UserSettings) -> Float> =
                        listOf(
                            { settings -> settings.blackHoleClearance },
                            { settings -> settings.mineClearance },
                            { settings -> settings.typhonClearance },
                        )

                    withData(AvoidanceSetting.entries) { entry ->
                        arbClearance.checkAll { clearance ->
                            clearanceGetters[entry.ordinal](
                                baseSettings.copy { entry.setClearance(this, clearance) }
                            ) shouldBeEqual clearance
                        }
                    }
                }
            }
        }
    })
