package artemis.agent.setup.settings

import androidx.activity.viewModels
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.AgentViewModel
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.isCheckedIf
import artemis.agent.isDisplayedWithText
import artemis.agent.scenario.SettingsMenuScenario
import artemis.agent.scenario.SettingsSubmenuOpenScenario
import artemis.agent.screens.MainScreen.mainScreenTest
import artemis.agent.screens.SettingsPageScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PersonalSettingsFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun personalSettingsVolumeTest() {
        testWithSettings(true) { SettingsPageScreen.closeSubmenu() }
    }

    @Test
    fun personalSettingsBackButtonTest() {
        testWithSettings(false) { SettingsPageScreen.backFromSubmenu() }
    }

    private fun testWithSettings(shouldTestSettings: Boolean, closeSubmenu: () -> Unit) {
        run {
            mainScreenTest {
                val themeIndex = AtomicInteger()
                val threeDigits = AtomicBoolean()
                val soundVolume = AtomicInteger()
                step("Fetch settings") {
                    activityScenarioRule.scenario.onActivity { activity ->
                        val viewModel = activity.viewModels<AgentViewModel>().value
                        themeIndex.lazySet(viewModel.themeIndex)
                        threeDigits.lazySet(viewModel.threeDigitDirections)
                        soundVolume.lazySet(
                            (viewModel.volume * AgentViewModel.VOLUME_SCALE).toInt()
                        )
                    }
                }

                scenario(SettingsMenuScenario)
                scenario(SettingsSubmenuOpenScenario.Personal)

                testThemeSetting(themeIndex.get())
                testThreeDigitsSetting(shouldTestSettings, threeDigits.get())
                testSoundVolume(shouldTestSettings, soundVolume.get())

                step("Close submenu") { closeSubmenu() }

                SettingsPageScreen.Personal {
                    step("All settings should be gone") { testScreenClosed() }
                }
            }
        }
    }

    private companion object {
        const val VOLUME_TEST_COUNT = 20
        const val MAX_VOLUME = 101

        fun TestContext<*>.testThemeSetting(themeIndex: Int) {
            SettingsPageScreen.Personal {
                step("Check theme setting components") {
                    themeTitle.isDisplayedWithText(R.string.theme)
                    themeButtons.forEachIndexed { index, button ->
                        button {
                            isDisplayed()
                            isCheckedIf(index == themeIndex)
                        }
                    }
                }
            }
        }

        fun TestContext<*>.testThreeDigitsSetting(
            shouldTest: Boolean,
            isShowingThreeDigits: Boolean,
        ) {
            SettingsPageScreen.Personal {
                listOf(
                        "Check three-digit setting components" to isShowingThreeDigits,
                        "Toggle three-digit setting once" to !isShowingThreeDigits,
                        "Toggle three-digit setting again" to isShowingThreeDigits,
                    )
                    .let { if (shouldTest) it else it.take(1) }
                    .forEachIndexed { index, (stepName, showingThree) ->
                        step(stepName) {
                            if (index > 0) {
                                step("Click toggle") { threeDigitDirectionsButton.click() }
                            }

                            step("Test UI") {
                                threeDigitDirectionsTitle.isDisplayedWithText(
                                    R.string.three_digit_directions
                                )
                                threeDigitDirectionsButton {
                                    isDisplayed()
                                    isCheckedIf(showingThree)
                                }
                                threeDigitDirectionsLabel.isDisplayedWithText(
                                    device.targetContext.getString(
                                        R.string.direction,
                                        "0".repeat(if (showingThree) 3 else 1),
                                    )
                                )
                            }
                        }
                    }
            }
        }

        fun TestContext<*>.testSoundVolume(shouldTest: Boolean, soundVolume: Int) {
            SettingsPageScreen.Personal {
                step("Check sound volume setting components") {
                    soundVolumeTitle.isDisplayedWithText(R.string.sound_volume)
                    soundVolumeBar {
                        isDisplayed()
                        hasProgress(soundVolume)
                    }
                    soundVolumeLabel.isDisplayedWithText(soundVolume.toString())
                }

                if (shouldTest) {
                    step("Test changing sound volume") {
                        val volumeTests =
                            List(VOLUME_TEST_COUNT) { Random.nextInt(MAX_VOLUME) } + soundVolume
                        volumeTests.forEach { volume ->
                            soundVolumeBar.setProgress(volume)
                            soundVolumeLabel.hasText(volume.toString())
                        }
                    }
                }
            }
        }

        fun SettingsPageScreen.Personal.testScreenClosed() {
            themeTitle.doesNotExist()
            themeDefaultButton.doesNotExist()
            themeRedButton.doesNotExist()
            themeGreenButton.doesNotExist()
            themeYellowButton.doesNotExist()
            themeBlueButton.doesNotExist()
            themePurpleButton.doesNotExist()
            threeDigitDirectionsTitle.doesNotExist()
            threeDigitDirectionsButton.doesNotExist()
            threeDigitDirectionsLabel.doesNotExist()
            soundVolumeTitle.doesNotExist()
            soundVolumeBar.doesNotExist()
            soundVolumeLabel.doesNotExist()
        }
    }
}
