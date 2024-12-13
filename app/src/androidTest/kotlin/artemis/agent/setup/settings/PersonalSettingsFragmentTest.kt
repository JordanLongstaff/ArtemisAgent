package artemis.agent.setup.settings

import androidx.activity.viewModels
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.AgentViewModel
import artemis.agent.ArtemisAgentTestHelpers.assertChecked
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaProgressBarAssertions.assertProgress
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo
import com.adevinta.android.barista.interaction.BaristaSeekBarInteractions.setProgressTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@LargeTest
class PersonalSettingsFragmentTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun personalSettingsTest() {
        val threeDigits = AtomicBoolean()
        val soundVolume = AtomicInteger()
        activityScenarioRule.scenario.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value

            threeDigits.lazySet(viewModel.threeDigitDirections)
            soundVolume.lazySet((viewModel.volume * AgentViewModel.VOLUME_SCALE).toInt())
        }

        SettingsFragmentTest.openSettingsMenu()
        SettingsFragmentTest.openSettingsSubMenu(7)

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
        assertChecked(R.id.threeDigitDirectionsButton, threeDigits.get())

        scrollTo(R.id.soundVolumeDivider)
        assertDisplayed(R.id.soundVolumeTitle, R.string.sound_volume)
        assertDisplayed(R.id.soundVolumeBar)
        assertProgress(R.id.soundVolumeBar, soundVolume.get())
        assertDisplayed(R.id.soundVolumeLabel, soundVolume.toString())

        repeat(PROGRESS_TEST_COUNT) {
            val progress = Random.nextInt(MAX_SOUND_VOLUME)
            setProgressTo(R.id.soundVolumeBar, progress)
            assertDisplayed(R.id.soundVolumeLabel, progress.toString())
        }

        setProgressTo(R.id.soundVolumeBar, soundVolume.get())
        assertDisplayed(R.id.soundVolumeLabel, soundVolume.toString())

        SettingsFragmentTest.closeSettingsSubMenu()
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

    private companion object {
        const val PROGRESS_TEST_COUNT = 10
        const val MAX_SOUND_VOLUME = 101
    }
}
