package artemis.agent.setup.settings

import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.ToggleButton
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewbinding.ViewBindings
import artemis.agent.R
import artemis.agent.UserSettingsOuterClass
import artemis.agent.copy
import artemis.agent.databinding.SecondsInputBinding
import artemis.agent.databinding.SettingsBiomechsBinding
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

class BiomechSortToggleTest :
    DescribeSpec({
        describe("BiomechSortToggle") {
            mockkStatic(ViewBindings::class)
            mockkStatic(SecondsInputBinding::class)

            mockkViewBinding<ToggleButton>(R.id.biomechSortingClassButton1, 0)
            mockkViewBinding<ToggleButton>(R.id.biomechSortingStatusButton, 1)
            mockkViewBinding<ToggleButton>(R.id.biomechSortingClassButton2, 2)
            mockkViewBinding<ToggleButton>(R.id.biomechSortingNameButton, 3)

            mockkViewBinding<RadioButton>(R.id.biomechSortingDefaultButton)
            mockkViewBinding<RadioButton>(R.id.biomechSortingDefaultOffButton)
            mockkViewBinding<RadioGroup>(R.id.biomechSortingDefaultButtonGroup)
            mockkViewBinding<Flow>(R.id.biomechSortingFlow)
            mockkViewBinding<TextView>(R.id.biomechSortingTitle)
            mockkViewBinding<View>(R.id.biomechSortingDivider)
            mockkViewBinding<TextView>(R.id.freezeDurationTitle)
            mockkViewBinding<View>(R.id.freezeDurationDivider)

            mockkViewBinding<View>(R.id.freezeDurationTimeInput)
            every { SecondsInputBinding.bind(any()) } returns mockk()

            val mockBinding = SettingsBiomechsBinding.bind(mockk<ConstraintLayout>())

            afterSpec {
                clearAllMocks()
                unmockkAll()
            }

            describe("Toggle buttons") {
                withData(BiomechSortToggle.entries) { entry ->
                    entry.getButton(mockBinding).id shouldBeEqual entry.ordinal
                }
            }

            describe("User settings") {
                val baseSettings = userSettings {
                    biomechSortClassFirst = false
                    biomechSortClassSecond = false
                    biomechSortName = false
                    biomechSortStatus = false
                }

                val copies =
                    listOf(
                        baseSettings.copy { biomechSortClassFirst = true },
                        baseSettings.copy { biomechSortStatus = true },
                        baseSettings.copy { biomechSortClassSecond = true },
                        baseSettings.copy { biomechSortName = true },
                    )

                describe("Get") {
                    withData(BiomechSortToggle.entries) { entry ->
                        it("Off") { entry.isChecked(baseSettings).shouldBeFalse() }

                        it("On") { entry.isChecked(copies[entry.ordinal]).shouldBeTrue() }
                    }
                }

                describe("Set") {
                    data class ToggleTestCase(
                        val toggle: BiomechSortToggle,
                        val initialSettings: UserSettingsOuterClass.UserSettings,
                        val testFn: (UserSettingsOuterClass.UserSettings, Boolean) -> Unit,
                    )

                    withData(
                        nameFn = { it.toggle.name },
                        ToggleTestCase(
                            toggle = BiomechSortToggle.SORT_CLASS_FIRST,
                            initialSettings = copies[BiomechSortToggle.SORT_CLASS_SECOND.ordinal],
                        ) { settings, isChecked ->
                            settings.biomechSortClassFirst shouldBeEqual isChecked
                            settings.biomechSortClassSecond.shouldBeFalse()
                        },
                        ToggleTestCase(
                            toggle = BiomechSortToggle.SORT_STATUS,
                            initialSettings = baseSettings,
                        ) { settings, isChecked ->
                            settings.biomechSortStatus shouldBeEqual isChecked
                        },
                        ToggleTestCase(
                            toggle = BiomechSortToggle.SORT_CLASS_SECOND,
                            initialSettings = copies[BiomechSortToggle.SORT_CLASS_FIRST.ordinal],
                        ) { settings, isChecked ->
                            settings.biomechSortClassSecond shouldBeEqual isChecked
                            settings.biomechSortClassFirst.shouldBeFalse()
                        },
                        ToggleTestCase(
                            toggle = BiomechSortToggle.SORT_NAME,
                            initialSettings = baseSettings,
                        ) { settings, isChecked ->
                            settings.biomechSortName shouldBeEqual isChecked
                        },
                    ) { (toggle, initialSettings, testFn) ->
                        var currentSettings = initialSettings

                        withData(nameFn = { it.first }, "On" to true, "Off" to false) {
                            (_, isChecked) ->
                            currentSettings =
                                currentSettings.copy { toggle.onCheckedChanged(this, isChecked) }
                            testFn(currentSettings, isChecked)
                        }
                    }
                }
            }
        }
    })
