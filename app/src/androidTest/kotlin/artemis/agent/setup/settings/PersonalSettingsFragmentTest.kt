package artemis.agent.setup.settings

import androidx.activity.viewModels
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.AgentViewModel
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.isCheckedIf
import artemis.agent.isDisplayedWithText
import artemis.agent.isEnabledIf
import artemis.agent.isRemoved
import artemis.agent.scenario.SettingsMenuScenario
import artemis.agent.scenario.SettingsSubmenuOpenScenario
import artemis.agent.screens.MainScreen.mainScreenTest
import artemis.agent.screens.SettingsPageScreen
import artemis.agent.screens.SettingsPageScreen.Personal.soundMuteButton
import artemis.agent.screens.SettingsPageScreen.Personal.soundVolumeBar
import artemis.agent.screens.SettingsPageScreen.Personal.soundVolumeLabel
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import io.github.kakaocup.kakao.text.KTextView
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PersonalSettingsFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = activityScenarioRule<MainActivity>()

    @Test
    fun personalSettingsMutableTest() {
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
                val soundsMuted = AtomicBoolean()
                val hapticsEnabled = AtomicBoolean()

                step("Fetch settings") {
                    activityScenarioRule.scenario.onActivity { activity ->
                        val viewModel = activity.viewModels<AgentViewModel>().value
                        themeIndex.lazySet(viewModel.themeIndex)
                        threeDigits.lazySet(viewModel.threeDigitDirections)
                        soundVolume.lazySet(
                            (viewModel.volume * AgentViewModel.VOLUME_SCALE).toInt()
                        )
                        soundsMuted.lazySet(viewModel.soundsMuted)
                        hapticsEnabled.lazySet(viewModel.hapticsEnabled)
                    }
                }

                scenario(SettingsMenuScenario)
                scenario(SettingsSubmenuOpenScenario.Personal)

                testThemeSetting(themeIndex.get())
                testThreeDigitsSetting(shouldTestSettings, threeDigits.get())
                testSoundVolume(shouldTestSettings, soundVolume.get(), soundsMuted.get())

                SettingsPageScreen.Personal {
                    step("Check haptics setting") {
                        enableHapticsToggleSetting.testSingleToggle(hapticsEnabled.get())
                    }

                    step("Close submenu") { closeSubmenu() }

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
                            isCompletelyDisplayed()
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
                                    isCompletelyDisplayed()
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

        fun TestContext<*>.testSoundVolume(
            shouldTest: Boolean,
            soundVolume: Int,
            isMuted: Boolean,
        ) {
            SettingsPageScreen.Personal {
                step("Check sound volume setting components") {
                    soundVolumeTitle.isDisplayedWithText(R.string.sound_volume)
                    soundVolumeBar {
                        isCompletelyDisplayed()
                        hasProgress(soundVolume)
                    }
                    soundMuteButton {
                        isDisplayedWithText(R.string.mute)
                        isCheckedIf(isMuted)
                        isEnabledIf(soundVolume > 0)
                    }
                    soundVolumeLabel.testVolume(soundVolume, isMuted)
                }

                if (shouldTest) {
                    step("Test changing sound volume") {
                        val volumeTests =
                            List(VOLUME_TEST_COUNT) { Random.nextInt(MAX_VOLUME) } + soundVolume
                        volumeTests.forEach { volume ->
                            soundVolumeBar.setProgress(volume)
                            soundVolumeLabel.testVolume(volume, isMuted)
                            soundMuteButton {
                                isCheckedIf(isMuted)
                                isEnabledIf(volume > 0)
                            }
                        }
                    }

                    testMuteButton(soundVolume, isMuted)
                }
            }
        }

        fun TestContext<*>.testMuteButton(volume: Int, isMuted: Boolean) {
            var tempVolume = volume
            if (volume == 0) {
                step("Temporarily increase volume") {
                    tempVolume = MAX_VOLUME
                    soundVolumeBar.setProgress(MAX_VOLUME)
                    soundVolumeLabel.testVolume(MAX_VOLUME, isMuted)
                    soundMuteButton {
                        isCheckedIf(isMuted)
                        isEnabled()
                    }
                }
            }

            step("Test mute button") {
                listOf(!isMuted, isMuted).forEachIndexed { index, mute ->
                    testMuteButtonState(tempVolume, mute)

                    step("Disable") {
                        soundVolumeBar.setProgress(0)
                        soundMuteButton {
                            isCheckedIf(mute)
                            isDisabled()
                        }
                    }

                    if (index == 0) {
                        step("Enable") {
                            soundVolumeBar.setProgress(tempVolume)
                            soundMuteButton {
                                isCheckedIf(mute)
                                isEnabled()
                            }
                        }
                    }
                }
            }

            if (volume != 0) {
                step("Reset volume") { soundVolumeBar.setProgress(volume) }
            }
        }

        fun TestContext<*>.testMuteButtonState(volume: Int, isMuted: Boolean) {
            step("Turn ${if (isMuted) "on" else "off"}") {
                soundMuteButton.click()
                soundMuteButton {
                    isCheckedIf(isMuted)
                    isEnabled()
                }
                soundVolumeBar.hasProgress(volume)
                soundVolumeLabel.testVolume(volume, isMuted)
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
            themeOrangeButton.doesNotExist()
            themeDivider.doesNotExist()
            threeDigitDirectionsTitle.doesNotExist()
            threeDigitDirectionsButton.doesNotExist()
            threeDigitDirectionsLabel.doesNotExist()
            threeDigitDirectionsDivider.doesNotExist()
            soundVolumeTitle.doesNotExist()
            soundVolumeBar.doesNotExist()
            soundVolumeLabel.doesNotExist()
            soundMuteButton.doesNotExist()
            soundVolumeDivider.doesNotExist()
            enableHapticsToggleSetting.testNotExist()
        }

        fun KTextView.testVolume(volume: Int, isMuted: Boolean) {
            if (isMuted) {
                isRemoved()
            } else {
                isDisplayedWithText(volume.toString())
            }
        }
    }
}
