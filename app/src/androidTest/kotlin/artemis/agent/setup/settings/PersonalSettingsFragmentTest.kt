package artemis.agent.setup.settings

import android.Manifest
import androidx.activity.viewModels
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.ActivityScenarioManager
import artemis.agent.AgentViewModel
import artemis.agent.ArtemisAgentTestHelpers.assertChecked
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaProgressBarAssertions.assertProgress
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
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
        val threeDigits = AtomicBoolean()
        val soundVolume = AtomicInteger()
        activityScenarioManager.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value

            threeDigits.lazySet(viewModel.threeDigitDirections)
            soundVolume.lazySet((viewModel.volume * AgentViewModel.VOLUME_SCALE).toInt())
        }

        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

        SettingsFragmentTest.openSettingsMenu()
        SettingsFragmentTest.openSettingsSubMenu(ENTRY_INDEX)
        testPersonalSubMenuOpen(threeDigits.get(), soundVolume.get(), shouldTestVolumeBar)

        closeSubMenu()
        testPersonalSubMenuClosed()
    }

    private companion object {
        const val ENTRY_INDEX = 7
        const val VOLUME_TEST_COUNT = 20
        const val MAX_VOLUME = 101

        fun testPersonalSubMenuOpen(
            isThreeDigitsOn: Boolean,
            soundVolume: Int,
            shouldTestVolumeBar: Boolean,
        ) {
            scrollTo(R.id.themeDivider)
            assertDisplayed(R.id.themeTitle, R.string.theme)
            assertDisplayed(R.id.themeSelector)
            assertDisplayed(R.id.themeDefaultButton, R.string.default_setting)
            assertDisplayed(R.id.themeRedButton)
            assertDisplayed(R.id.themeGreenButton)
            assertDisplayed(R.id.themeYellowButton)
            assertDisplayed(R.id.themeBlueButton)
            assertDisplayed(R.id.themePurpleButton)

            scrollTo(R.id.threeDigitDirectionsDivider)
            assertDisplayed(R.id.threeDigitDirectionsTitle, R.string.three_digit_directions)
            assertDisplayed(R.id.threeDigitDirectionsButton)
            assertDisplayed(R.id.threeDigitDirectionsLabel)
            assertChecked(R.id.threeDigitDirectionsButton, isThreeDigitsOn)

            scrollTo(R.id.soundVolumeDivider)
            assertDisplayed(R.id.soundVolumeTitle, R.string.sound_volume)
            assertDisplayed(R.id.soundVolumeBar)
            assertProgress(R.id.soundVolumeBar, soundVolume)
            assertDisplayed(R.id.soundVolumeLabel, soundVolume.toString())

            if (shouldTestVolumeBar) {
                val volumeTests =
                    List(VOLUME_TEST_COUNT) { Random.nextInt(MAX_VOLUME) } + soundVolume
                volumeTests.forEach { volume ->
                    setProgressTo(R.id.soundVolumeBar, volume)
                    assertDisplayed(R.id.soundVolumeLabel, volume.toString())
                }
            }
        }

        fun testPersonalSubMenuClosed() {
            assertNotExist(R.id.themeTitle)
            assertNotExist(R.id.themeSelector)
            assertNotExist(R.id.themeDefaultButton)
            assertNotExist(R.id.themeRedButton)
            assertNotExist(R.id.themeGreenButton)
            assertNotExist(R.id.themeYellowButton)
            assertNotExist(R.id.themeBlueButton)
            assertNotExist(R.id.themePurpleButton)
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
