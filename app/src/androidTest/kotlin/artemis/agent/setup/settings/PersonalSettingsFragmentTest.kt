package artemis.agent.setup.settings

import android.Manifest
import androidx.activity.viewModels
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import artemis.agent.ActivityScenarioManager
import artemis.agent.AgentViewModel
import artemis.agent.ArtemisAgentTestHelpers.assertChecked
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaAssertions.assertThatBackButtonClosesTheApp
import com.adevinta.android.barista.assertion.BaristaProgressBarAssertions.assertProgress
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo
import com.adevinta.android.barista.interaction.BaristaSeekBarInteractions.setProgressTo
import com.adevinta.android.barista.interaction.PermissionGranter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PersonalSettingsFragmentTest {
    @get:Rule val activityScenarioManager = ActivityScenarioManager.forActivity<MainActivity>()

    @Test
    fun personalSettingsVolumeTest() {
        testWithSettings(true) { SettingsFragmentTest.closeSettingsSubMenu() }
    }

    @Test
    fun personalSettingsBackButtonTest() {
        testWithSettings(false) { SettingsFragmentTest.backFromSubMenu() }
    }

    private fun testWithSettings(shouldTestVolumeBar: Boolean, closeSubMenu: () -> Unit) {
        val themeIndex = AtomicInteger()
        val threeDigits = AtomicBoolean()
        val soundVolume = AtomicInteger()
        activityScenarioManager.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value

            themeIndex.lazySet(viewModel.themeIndex)
            threeDigits.lazySet(viewModel.threeDigitDirections)
            soundVolume.lazySet((viewModel.volume * AgentViewModel.VOLUME_SCALE).toInt())
        }

        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

        SettingsFragmentTest.openSettingsMenu()
        SettingsFragmentTest.openSettingsSubMenu(ENTRY_INDEX)
        testPersonalSubMenuOpen(
            themeIndex.get(),
            threeDigits.get(),
            soundVolume.get(),
            shouldTestVolumeBar,
        )

        closeSubMenu()
        testPersonalSubMenuClosed()

        assertThatBackButtonClosesTheApp()
    }

    private companion object {
        const val ENTRY_INDEX = 7
        const val VOLUME_TEST_COUNT = 20
        const val MAX_VOLUME = 101

        val context by lazy {
            checkNotNull(InstrumentationRegistry.getInstrumentation().targetContext) {
                "Failed to get context"
            }
        }

        val themeButtonIds =
            intArrayOf(
                R.id.themeDefaultButton,
                R.id.themeRedButton,
                R.id.themeGreenButton,
                R.id.themeYellowButton,
                R.id.themeBlueButton,
                R.id.themePurpleButton,
            )

        fun testPersonalSubMenuOpen(
            themeIndex: Int,
            isThreeDigitsOn: Boolean,
            soundVolume: Int,
            shouldTestSettings: Boolean,
        ) {
            scrollTo(R.id.themeDivider)
            assertDisplayed(R.id.themeTitle, R.string.theme)
            assertDisplayed(R.id.themeSelector)
            assertDisplayed(R.id.themeDefaultButton, R.string.default_setting)

            themeButtonIds.forEachIndexed { index, button ->
                if (index > 0) assertDisplayed(button)
                assertChecked(button, index == themeIndex)
            }

            testPersonalSubMenuThreeDigits(isThreeDigitsOn, shouldTestSettings)

            scrollTo(R.id.soundVolumeDivider)
            assertDisplayed(R.id.soundVolumeTitle, R.string.sound_volume)
            assertDisplayed(R.id.soundVolumeBar)
            assertProgress(R.id.soundVolumeBar, soundVolume)
            assertDisplayed(R.id.soundVolumeLabel, soundVolume.toString())

            if (shouldTestSettings) {
                val volumeTests =
                    List(VOLUME_TEST_COUNT) { Random.nextInt(MAX_VOLUME) } + soundVolume
                volumeTests.forEach { volume ->
                    setProgressTo(R.id.soundVolumeBar, volume)
                    assertDisplayed(R.id.soundVolumeLabel, volume.toString())
                }
            }
        }

        fun testPersonalSubMenuThreeDigits(threeDigits: Boolean, shouldTest: Boolean) {
            scrollTo(R.id.threeDigitDirectionsDivider)
            assertDisplayed(R.id.threeDigitDirectionsButton)
            assertDisplayed(R.id.threeDigitDirectionsLabel)

            testPersonalSubMenuThreeDigitsState(threeDigits)

            if (!shouldTest) return

            booleanArrayOf(!threeDigits, threeDigits).forEach { isOn ->
                clickOn(R.id.threeDigitDirectionsButton)
                testPersonalSubMenuThreeDigitsState(isOn)
            }
        }

        fun testPersonalSubMenuThreeDigitsState(isOn: Boolean) {
            assertChecked(R.id.threeDigitDirectionsButton, isOn)

            val threeDigitsText =
                context.getString(R.string.three_digit_directions, "0".repeat(if (isOn) 3 else 1))
            assertDisplayed(R.id.threeDigitDirectionsTitle, threeDigitsText)
        }

        fun testPersonalSubMenuClosed() {
            assertNotExist(R.id.themeTitle)
            assertNotExist(R.id.themeSelector)
            themeButtonIds.forEach { assertNotExist(it) }
            assertNotExist(R.id.themeDivider)
            assertNotExist(R.id.threeDigitDirectionsTitle)
            assertNotExist(R.id.threeDigitDirectionsButton)
            assertNotExist(R.id.threeDigitDirectionsLabel)
            assertNotExist(R.id.threeDigitDirectionsDivider)
            assertNotExist(R.id.soundVolumeTitle)
            assertNotExist(R.id.soundVolumeBar)
            assertNotExist(R.id.soundVolumeLabel)
            assertNotExist(R.id.soundVolumeDivider)
        }
    }
}
