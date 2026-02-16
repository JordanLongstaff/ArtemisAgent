package artemis.agent.setup.settings

import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewbinding.ViewBindings
import artemis.agent.R
import artemis.agent.copy
import artemis.agent.databinding.SecondsInputBinding
import artemis.agent.databinding.SettingsMissionsBinding
import artemis.agent.mockkViewBinding
import artemis.agent.userSettings
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll

class DisplayRewardSettingTest :
    DescribeSpec({
        describe("DisplayRewardSetting") {
            mockkStatic(ViewBindings::class)
            mockkStatic(SecondsInputBinding::class)

            mockkViewBinding<ToggleButton>(R.id.rewardsBatteryButton, 0)
            mockkViewBinding<ToggleButton>(R.id.rewardsCoolantButton, 1)
            mockkViewBinding<ToggleButton>(R.id.rewardsNukeButton, 2)
            mockkViewBinding<ToggleButton>(R.id.rewardsProductionButton, 3)
            mockkViewBinding<ToggleButton>(R.id.rewardsShieldButton, 4)

            mockkViewBinding<Button>(R.id.rewardsAllButton)
            mockkViewBinding<Button>(R.id.rewardsNoneButton)
            mockkViewBinding<TextView>(R.id.rewardsTitle)
            mockkViewBinding<View>(R.id.rewardsDivider)
            mockkViewBinding<ToggleButton>(R.id.autoDismissalButton)
            mockkViewBinding<TextView>(R.id.autoDismissalSecondsLabel)
            mockkViewBinding<TextView>(R.id.autoDismissalTitle)
            mockkViewBinding<View>(R.id.autoDismissalDivider)

            mockkViewBinding<View>(R.id.autoDismissalTimeInput)
            every { SecondsInputBinding.bind(any()) } returns mockk()

            val mockBinding = SettingsMissionsBinding.bind(mockk<ConstraintLayout>())

            afterSpec {
                clearAllMocks()
                unmockkAll()
            }

            describe("Toggle buttons") {
                withData(DisplayRewardSetting.entries) { entry ->
                    entry.getButton(mockBinding).id shouldBeEqual entry.ordinal
                }
            }

            describe("User settings") {
                val baseSettings = userSettings {
                    displayRewardBattery = false
                    displayRewardCoolant = false
                    displayRewardNukes = false
                    displayRewardProduction = false
                    displayRewardShield = false
                }

                val copies =
                    listOf(
                        baseSettings.copy { displayRewardBattery = true },
                        baseSettings.copy { displayRewardCoolant = true },
                        baseSettings.copy { displayRewardNukes = true },
                        baseSettings.copy { displayRewardProduction = true },
                        baseSettings.copy { displayRewardShield = true },
                    )

                describe("Get") {
                    withData(DisplayRewardSetting.entries) { entry ->
                        it("Off") { entry.isChecked(baseSettings).shouldBeFalse() }

                        it("On") { entry.isChecked(copies[entry.ordinal]).shouldBeTrue() }
                    }
                }

                describe("Set") {
                    withData(DisplayRewardSetting.entries) { entry ->
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
