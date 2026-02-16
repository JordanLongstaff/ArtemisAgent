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
import artemis.agent.databinding.SettingsAlliesBinding
import artemis.agent.mockkViewBinding
import artemis.agent.userSettings
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll

class AllySettingsToggleTest :
    DescribeSpec({
        describe("AllySettingsToggle") {
            mockkStatic(ViewBindings::class)

            mockkViewBinding<ToggleButton>(R.id.allySortingClassButton1, 0)
            mockkViewBinding<ToggleButton>(R.id.allySortingStatusButton, 1)
            mockkViewBinding<ToggleButton>(R.id.allySortingClassButton2, 2)
            mockkViewBinding<ToggleButton>(R.id.allySortingNameButton, 3)
            mockkViewBinding<ToggleButton>(R.id.allySortingEnergyButton, 4)
            mockkViewBinding<ToggleButton>(R.id.showDestroyedAlliesButton, 5)
            mockkViewBinding<ToggleButton>(R.id.manuallyReturnButton, 6)
            mockkViewBinding<ToggleButton>(R.id.enableRecapsButton, 7)
            mockkViewBinding<ToggleButton>(R.id.backButtonCancelButton, 8)

            mockkViewBinding<RadioButton>(R.id.allySortingDefaultButton)
            mockkViewBinding<RadioButton>(R.id.allySortingDefaultOffButton)
            mockkViewBinding<RadioGroup>(R.id.allySortingDefaultButtonGroup)
            mockkViewBinding<Flow>(R.id.allySortingFlow)
            mockkViewBinding<TextView>(R.id.allySortingTitle)
            mockkViewBinding<View>(R.id.allySortingDivider)
            mockkViewBinding<TextView>(R.id.backButtonCancelTitle)
            mockkViewBinding<View>(R.id.backButtonCancelDivider)
            mockkViewBinding<TextView>(R.id.enableRecapsTitle)
            mockkViewBinding<View>(R.id.enableRecapsDivider)
            mockkViewBinding<TextView>(R.id.manuallyReturnTitle)
            mockkViewBinding<View>(R.id.manuallyReturnDivider)
            mockkViewBinding<TextView>(R.id.showDestroyedAlliesTitle)
            mockkViewBinding<View>(R.id.showDestroyedAlliesDivider)

            val mockBinding = SettingsAlliesBinding.bind(mockk<ConstraintLayout>())

            afterSpec {
                clearAllMocks()
                unmockkAll()
            }

            describe("Toggle buttons") {
                withData(AllySettingsToggle.entries) { entry ->
                    entry.getButton(mockBinding).id shouldBeEqual entry.ordinal
                }
            }

            describe("User settings") {
                val baseSettings = userSettings {
                    allySortClassFirst = false
                    allySortClassSecond = false
                    allySortEnergyFirst = false
                    allySortName = false
                    allySortStatus = false
                    showDestroyedAllies = false
                    allyCommandManualReturn = false
                    allyRecapsEnabled = false
                    allyBackEnabled = false
                }

                val copies =
                    listOf(
                        baseSettings.copy { allySortClassFirst = true },
                        baseSettings.copy { allySortStatus = true },
                        baseSettings.copy { allySortClassSecond = true },
                        baseSettings.copy { allySortName = true },
                        baseSettings.copy {
                            allySortEnergyFirst = true
                            allySortStatus = true
                        },
                        baseSettings.copy { showDestroyedAllies = true },
                        baseSettings.copy { allyCommandManualReturn = true },
                        baseSettings.copy { allyRecapsEnabled = true },
                        baseSettings.copy { allyBackEnabled = true },
                    )

                describe("Get") {
                    withData(AllySettingsToggle.entries) { entry ->
                        it("Off") { entry.isChecked(baseSettings).shouldBeFalse() }

                        it("On") { entry.isChecked(copies[entry.ordinal]).shouldBeTrue() }
                    }
                }

                describe("Set") {
                    data class ToggleTestCase(
                        val toggle: AllySettingsToggle,
                        val initialSettings: UserSettingsOuterClass.UserSettings,
                        val testFn: (UserSettingsOuterClass.UserSettings, Boolean) -> Unit,
                    )

                    withData(
                        nameFn = { it.toggle.name },
                        ToggleTestCase(
                            toggle = AllySettingsToggle.SORT_CLASS_FIRST,
                            initialSettings = copies[AllySettingsToggle.SORT_CLASS_SECOND.ordinal],
                        ) { settings, isChecked ->
                            settings.allySortClassFirst shouldBeEqual isChecked
                            settings.allySortClassSecond.shouldBeFalse()
                        },
                        ToggleTestCase(
                            toggle = AllySettingsToggle.SORT_STATUS,
                            initialSettings = copies[AllySettingsToggle.SORT_ENERGY_FIRST.ordinal],
                        ) { settings, isChecked ->
                            settings.allySortStatus shouldBeEqual isChecked
                            settings.allySortEnergyFirst.shouldBeFalse()
                        },
                        ToggleTestCase(
                            toggle = AllySettingsToggle.SORT_CLASS_SECOND,
                            initialSettings = copies[AllySettingsToggle.SORT_CLASS_FIRST.ordinal],
                        ) { settings, isChecked ->
                            settings.allySortClassSecond shouldBeEqual isChecked
                            settings.allySortClassFirst.shouldBeFalse()
                        },
                        ToggleTestCase(
                            toggle = AllySettingsToggle.SORT_NAME,
                            initialSettings = baseSettings,
                        ) { settings, isChecked ->
                            settings.allySortName shouldBeEqual isChecked
                        },
                        ToggleTestCase(
                            toggle = AllySettingsToggle.SORT_ENERGY_FIRST,
                            initialSettings = baseSettings,
                        ) { settings, isChecked ->
                            settings.allySortEnergyFirst shouldBeEqual isChecked
                            settings.allySortStatus.shouldBeTrue()
                        },
                        ToggleTestCase(
                            toggle = AllySettingsToggle.TOGGLE_SHOW_DESTROYED,
                            initialSettings = baseSettings,
                        ) { settings, isChecked ->
                            settings.showDestroyedAllies shouldBeEqual isChecked
                        },
                        ToggleTestCase(
                            toggle = AllySettingsToggle.TOGGLE_MANUAL_RETURN,
                            initialSettings = baseSettings,
                        ) { settings, isChecked ->
                            settings.allyCommandManualReturn shouldBeEqual isChecked
                        },
                        ToggleTestCase(
                            toggle = AllySettingsToggle.TOGGLE_RECAPS,
                            initialSettings = baseSettings,
                        ) { settings, isChecked ->
                            settings.allyRecapsEnabled shouldBeEqual isChecked
                        },
                        ToggleTestCase(
                            toggle = AllySettingsToggle.TOGGLE_BACK_CANCEL,
                            initialSettings = baseSettings,
                        ) { settings, isChecked ->
                            settings.allyBackEnabled shouldBeEqual isChecked
                        },
                    ) { (toggle, initialSettings, testFn) ->
                        var currentSettings = initialSettings

                        var changes = listOf("On" to true, "Off" to false)
                        if (toggle.isChecked(initialSettings)) changes = changes.reversed()

                        withData(nameFn = { it.first }, changes) { (_, isChecked) ->
                            currentSettings =
                                currentSettings.copy { toggle.onCheckedChanged(this, isChecked) }
                            testFn(currentSettings, isChecked)
                        }
                    }
                }
            }

            describe("Sort entries") {
                val expectedSize = 5

                it("Contains $expectedSize entries") {
                    AllySettingsToggle.sortEntries shouldHaveSize expectedSize
                }

                AllySettingsToggle.entries.forEachIndexed { index, entry ->
                    val isSort = entry.isSort
                    val shouldSort = index < expectedSize

                    it("${entry.name}: $isSort") { isSort shouldBeEqual shouldSort }
                }
            }
        }
    })
