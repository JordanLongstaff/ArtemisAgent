package artemis.agent.setup.settings

import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ToggleButton
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.viewbinding.ViewBindings
import artemis.agent.R
import artemis.agent.UserSettingsOuterClass
import artemis.agent.copy
import artemis.agent.databinding.SettingsEnemiesBinding
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

class EnemySettingsToggleTest :
    DescribeSpec({
        describe("EnemySettingsToggle") {
            mockkStatic(ViewBindings::class)

            mockkViewBinding<ToggleButton>(R.id.enemySortingSurrenderButton, 0)
            mockkViewBinding<ToggleButton>(R.id.enemySortingRaceButton, 1)
            mockkViewBinding<ToggleButton>(R.id.enemySortingNameButton, 2)
            mockkViewBinding<ToggleButton>(R.id.enemySortingRangeButton, 3)
            mockkViewBinding<ToggleButton>(R.id.reverseRaceSortButton, 4)
            mockkViewBinding<ToggleButton>(R.id.showIntelButton, 5)
            mockkViewBinding<ToggleButton>(R.id.showTauntStatusButton, 6)
            mockkViewBinding<ToggleButton>(R.id.disableIneffectiveButton, 7)
            mockkViewBinding<ToggleButton>(R.id.surrenderRangeEnableButton, 8)

            mockkViewBinding<RadioButton>(R.id.enemySortingDefaultButton)
            mockkViewBinding<RadioButton>(R.id.enemySortingDefaultOffButton)
            mockkViewBinding<RadioGroup>(R.id.enemySortingDefaultButtonGroup)
            mockkViewBinding<Flow>(R.id.enemySortingFlow)
            mockkViewBinding<TextView>(R.id.enemySortingTitle)
            mockkViewBinding<View>(R.id.enemySortingDivider)
            mockkViewBinding<TextView>(R.id.surrenderRangeTitle)
            mockkViewBinding<EditText>(R.id.surrenderRangeField)
            mockkViewBinding<TextView>(R.id.surrenderRangeInfinity)
            mockkViewBinding<TextView>(R.id.surrenderRangeKm)
            mockkViewBinding<View>(R.id.surrenderRangeDivider)
            mockkViewBinding<TextView>(R.id.surrenderBurstCountTitle)
            mockkViewBinding<TextView>(R.id.surrenderBurstCountLabel)
            mockkViewBinding<SeekBar>(R.id.surrenderBurstCountBar)
            mockkViewBinding<View>(R.id.surrenderBurstCountDivider)
            mockkViewBinding<TextView>(R.id.surrenderBurstIntervalTitle)
            mockkViewBinding<TextView>(R.id.surrenderBurstIntervalLabel)
            mockkViewBinding<TextView>(R.id.surrenderBurstIntervalMilliseconds)
            mockkViewBinding<SeekBar>(R.id.surrenderBurstIntervalBar)
            mockkViewBinding<View>(R.id.surrenderBurstIntervalDivider)
            mockkViewBinding<TextView>(R.id.reverseRaceSortTitle)
            mockkViewBinding<TextView>(R.id.showIntelTitle)
            mockkViewBinding<View>(R.id.showIntelDivider)
            mockkViewBinding<TextView>(R.id.showTauntStatusTitle)
            mockkViewBinding<View>(R.id.showTauntStatusDivider)
            mockkViewBinding<TextView>(R.id.disableIneffectiveTitle)
            mockkViewBinding<View>(R.id.disableIneffectiveDivider)

            val mockBinding = SettingsEnemiesBinding.bind(mockk<ConstraintLayout>())

            afterSpec {
                clearAllMocks()
                unmockkAll()
            }

            describe("Toggle buttons") {
                withData(EnemySettingsToggle.entries) { entry ->
                    entry.getButton(mockBinding).id shouldBeEqual entry.ordinal
                }
            }

            describe("User settings") {
                val baseSettings = userSettings {
                    enemySortSurrendered = false
                    enemySortFaction = false
                    enemySortName = false
                    enemySortDistance = false
                    enemySortFactionReversed = false
                    showEnemyIntel = false
                    showTauntStatuses = false
                    disableIneffectiveTaunts = false
                    surrenderRangeEnabled = false
                }

                val copies =
                    listOf(
                        baseSettings.copy { enemySortSurrendered = true },
                        baseSettings.copy { enemySortFaction = true },
                        baseSettings.copy { enemySortName = true },
                        baseSettings.copy { enemySortDistance = true },
                        baseSettings.copy { enemySortFactionReversed = true },
                        baseSettings.copy { showEnemyIntel = true },
                        baseSettings.copy { showTauntStatuses = true },
                        baseSettings.copy { disableIneffectiveTaunts = true },
                        baseSettings.copy { surrenderRangeEnabled = true },
                    )

                describe("Get") {
                    withData(EnemySettingsToggle.entries) { entry ->
                        it("Off") { entry.isChecked(baseSettings).shouldBeFalse() }

                        it("On") { entry.isChecked(copies[entry.ordinal]).shouldBeTrue() }
                    }
                }

                describe("Set") {
                    data class ToggleTestCase(
                        val toggle: EnemySettingsToggle,
                        val initialSettings: UserSettingsOuterClass.UserSettings,
                        val testFn: (UserSettingsOuterClass.UserSettings, Boolean) -> Unit,
                    )

                    withData(
                        nameFn = { it.toggle.name },
                        ToggleTestCase(
                            toggle = EnemySettingsToggle.SORT_SURRENDER,
                            initialSettings = baseSettings,
                        ) { settings, isChecked ->
                            settings.enemySortSurrendered shouldBeEqual isChecked
                        },
                        ToggleTestCase(
                            toggle = EnemySettingsToggle.SORT_RACE,
                            initialSettings = baseSettings,
                        ) { settings, isChecked ->
                            settings.enemySortFaction shouldBeEqual isChecked
                        },
                        ToggleTestCase(
                            toggle = EnemySettingsToggle.SORT_NAME,
                            initialSettings = copies[EnemySettingsToggle.SORT_RANGE.ordinal],
                        ) { settings, isChecked ->
                            settings.enemySortName shouldBeEqual isChecked
                            settings.enemySortDistance.shouldBeFalse()
                        },
                        ToggleTestCase(
                            toggle = EnemySettingsToggle.SORT_RANGE,
                            initialSettings = copies[EnemySettingsToggle.SORT_NAME.ordinal],
                        ) { settings, isChecked ->
                            settings.enemySortDistance shouldBeEqual isChecked
                            settings.enemySortName.shouldBeFalse()
                        },
                        ToggleTestCase(
                            toggle = EnemySettingsToggle.TOGGLE_REVERSE_RACE,
                            initialSettings = baseSettings,
                        ) { settings, isChecked ->
                            settings.enemySortFactionReversed shouldBeEqual isChecked
                        },
                        ToggleTestCase(
                            toggle = EnemySettingsToggle.TOGGLE_INTEL,
                            initialSettings = baseSettings,
                        ) { settings, isChecked ->
                            settings.showEnemyIntel shouldBeEqual isChecked
                        },
                        ToggleTestCase(
                            toggle = EnemySettingsToggle.TOGGLE_TAUNT_STATUSES,
                            initialSettings = baseSettings,
                        ) { settings, isChecked ->
                            settings.showTauntStatuses shouldBeEqual isChecked
                        },
                        ToggleTestCase(
                            toggle = EnemySettingsToggle.TOGGLE_DISABLE_INEFFECTIVE,
                            initialSettings = baseSettings,
                        ) { settings, isChecked ->
                            settings.disableIneffectiveTaunts shouldBeEqual isChecked
                        },
                        ToggleTestCase(
                            toggle = EnemySettingsToggle.TOGGLE_SURRENDER_RANGE,
                            initialSettings = baseSettings,
                        ) { settings, isChecked ->
                            settings.surrenderRangeEnabled shouldBeEqual isChecked
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

            describe("Sort entries") {
                val expectedSize = 4

                it("Contains $expectedSize entries") {
                    EnemySettingsToggle.sortEntries shouldHaveSize expectedSize
                }

                EnemySettingsToggle.entries.forEachIndexed { index, entry ->
                    val isSort = entry.isSort
                    val shouldSort = index < expectedSize

                    it("${entry.name}: $isSort") { isSort shouldBeEqual shouldSort }
                }
            }
        }
    })
