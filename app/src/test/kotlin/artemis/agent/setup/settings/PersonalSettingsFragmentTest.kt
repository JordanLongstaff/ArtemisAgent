package artemis.agent.setup.settings

import androidx.activity.viewModels
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.AgentViewModel
import artemis.agent.ArtemisAgentTestHelpers.assertChecked
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
@LargeTest
class PersonalSettingsFragmentTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun personalSettingsTest() {
        val threeDigits = AtomicBoolean()
        activityScenarioRule.scenario.onActivity { activity ->
            threeDigits.lazySet(activity.viewModels<AgentViewModel>().value.threeDigitDirections)
        }

        SettingsFragmentTest.openSettingsMenu()
        SettingsFragmentTest.openSettingsSubMenu(7)

        scrollTo(R.id.themeDivider)
        assertDisplayed(R.id.themeTitle, R.string.theme)
        assertDisplayed(R.id.themeSelector)
        assertDisplayed(R.id.themeDefaultButton)
        assertDisplayed(R.id.themeRedButton)
        assertDisplayed(R.id.themeGreenButton)
        assertDisplayed(R.id.themeYellowButton)
        assertDisplayed(R.id.themeBlueButton)
        assertDisplayed(R.id.themePurpleButton)

        scrollTo(R.id.threeDigitDirectionsDivider)
        assertDisplayed(R.id.threeDigitDirectionsTitle)
        assertDisplayed(R.id.threeDigitDirectionsButton)
        assertDisplayed(R.id.threeDigitDirectionsLabel)
        assertChecked(R.id.threeDigitDirectionsButton, threeDigits.get())

        scrollTo(R.id.soundVolumeDivider)
        assertDisplayed(R.id.soundVolumeTitle)
        assertDisplayed(R.id.soundVolumeBar)
        assertDisplayed(R.id.soundVolumeLabel)

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
}
