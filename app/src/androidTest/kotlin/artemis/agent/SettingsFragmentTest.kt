package artemis.agent

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingsFragmentTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    private val pageTitles = intArrayOf(
        R.string.settings_menu_client,
        R.string.settings_menu_connection,
        R.string.settings_menu_missions,
        R.string.settings_menu_allies,
        R.string.settings_menu_enemies,
        R.string.settings_menu_biomechs,
        R.string.settings_menu_routing,
        R.string.settings_menu_personal,
    )

    private fun openSettingsMenu() {
        clickOn(R.id.settingsPageButton)
        assertSettingsMainMenuDisplayed()
    }

    private fun openSettingsSubMenu(index: Int) {
        clickListItem(R.id.settingsPageMenu, index)

        assertDisplayed(R.id.settingsPageTitle, pageTitles[index])
        assertDisplayed(R.id.settingsBack)
    }

    private fun closeSettingsSubMenu() {
        clickOn(R.id.settingsBack)
        assertSettingsMainMenuDisplayed()
    }

    private fun assertSettingsMainMenuDisplayed() {
        assertDisplayed(R.id.settingsPageTitle, R.string.settings)
        assertNotDisplayed(R.id.settingsBack)
    }

    @Test
    fun clientSettingsTest() {
        openSettingsMenu()

        openSettingsSubMenu(0)

        scrollTo(R.id.vesselDataDivider)
        assertDisplayed(R.id.vesselDataTitle, R.string.vessel_data_xml_location)
        assertDisplayed(R.id.vesselDataOptions)

        scrollTo(R.id.serverPortDivider)
        assertDisplayed(R.id.serverPortTitle, R.string.server_port)
        assertDisplayed(R.id.serverPortField)

        scrollTo(R.id.addressLimitDivider)
        assertDisplayed(R.id.addressLimitTitle)
        assertDisplayed(R.id.addressLimitEnableButton)

        scrollTo(R.id.updateIntervalDivider)
        assertDisplayed(R.id.updateIntervalTitle, R.string.update_interval)
        assertDisplayed(R.id.updateIntervalField)
        assertDisplayed(R.id.updateIntervalMilliseconds)

        closeSettingsSubMenu()
        assertNotExist(R.id.vesselDataTitle)
        assertNotExist(R.id.vesselDataOptions)
        assertNotExist(R.id.serverPortTitle)
        assertNotExist(R.id.serverPortField)
        assertNotExist(R.id.addressLimitTitle)
        assertNotExist(R.id.addressLimitEnableButton)
        assertNotExist(R.id.updateIntervalTitle)
        assertNotExist(R.id.updateIntervalField)
        assertNotExist(R.id.updateIntervalMilliseconds)
    }

    @Test
    fun connectionSettingsTest() {
        openSettingsMenu()

        openSettingsSubMenu(1)

        scrollTo(R.id.connectionTimeoutDivider)
        assertDisplayed(R.id.connectionTimeoutTitle, R.string.connection_timeout)
        assertDisplayed(R.id.connectionTimeoutTimeInput)
        assertDisplayed(R.id.connectionTimeoutSecondsLabel)

        scrollTo(R.id.heartbeatTimeoutDivider)
        assertDisplayed(R.id.heartbeatTimeoutTitle, R.string.heartbeat_timeout)
        assertDisplayed(R.id.heartbeatTimeoutTimeInput)
        assertDisplayed(R.id.heartbeatTimeoutSecondsLabel)

        scrollTo(R.id.scanTimeoutDivider)
        assertDisplayed(R.id.scanTimeoutTitle, R.string.scan_timeout)
        assertDisplayed(R.id.scanTimeoutTimeInput)
        assertDisplayed(R.id.scanTimeoutSecondsLabel)

        closeSettingsSubMenu()
        assertNotExist(R.id.connectionTimeoutTitle)
        assertNotExist(R.id.connectionTimeoutTimeInput)
        assertNotExist(R.id.connectionTimeoutSecondsLabel)
        assertNotExist(R.id.heartbeatTimeoutTitle)
        assertNotExist(R.id.heartbeatTimeoutTimeInput)
        assertNotExist(R.id.heartbeatTimeoutSecondsLabel)
        assertNotExist(R.id.scanTimeoutTitle)
        assertNotExist(R.id.scanTimeoutTimeInput)
        assertNotExist(R.id.scanTimeoutSecondsLabel)
    }

    @Test
    fun personalSettingsTest() {
        openSettingsMenu()

        openSettingsSubMenu(7)

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

        scrollTo(R.id.soundVolumeDivider)
        assertDisplayed(R.id.soundVolumeTitle)
        assertDisplayed(R.id.soundVolumeBar)
        assertDisplayed(R.id.soundVolumeLabel)

        closeSettingsSubMenu()
        assertNotExist(R.id.themeTitle)
        assertNotExist(R.id.themeSelector)
        assertNotExist(R.id.themeDefaultButton)
        assertNotExist(R.id.themeRedButton)
        assertNotExist(R.id.themeGreenButton)
        assertNotExist(R.id.themeYellowButton)
        assertNotExist(R.id.themeBlueButton)
        assertNotExist(R.id.themePurpleButton)
        assertNotExist(R.id.threeDigitDirectionsTitle)
        assertNotExist(R.id.threeDigitDirectionsButton)
        assertNotExist(R.id.threeDigitDirectionsLabel)
        assertNotExist(R.id.soundVolumeTitle)
        assertNotExist(R.id.soundVolumeBar)
        assertNotExist(R.id.soundVolumeLabel)
    }
}
