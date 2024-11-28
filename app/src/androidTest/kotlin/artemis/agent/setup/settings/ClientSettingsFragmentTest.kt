package artemis.agent.setup.settings

import androidx.activity.viewModels
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.AgentViewModel
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
@LargeTest
class ClientSettingsFragmentTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun clientSettingsTest() {
        val expectedPort = AtomicInteger()
        val expectedUpdateInterval = AtomicInteger()
        val externalVesselDataCount = AtomicInteger()
        val showingInfo = AtomicBoolean()
        val alwaysPublic = AtomicBoolean()

        activityScenarioRule.scenario.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value
            expectedPort.lazySet(viewModel.port)
            expectedUpdateInterval.lazySet(viewModel.updateObjectsInterval)
            externalVesselDataCount.lazySet(viewModel.storageDirectories.size)
            showingInfo.lazySet(viewModel.showingNetworkInfo)
            alwaysPublic.lazySet(viewModel.alwaysScanPublicBroadcasts)
        }

        SettingsFragmentTest.openSettingsMenu()
        SettingsFragmentTest.openSettingsSubMenu(0)
        testClientSubMenuOpen(
            externalVesselDataCount.get(),
            expectedPort.toString(),
            expectedUpdateInterval.toString(),
            showingInfo.get(),
            alwaysPublic.get(),
        )

        SettingsFragmentTest.closeSettingsSubMenu()
        testClientSubMenuClosed()
    }

    private companion object {
        val clientSingleToggleSettings = arrayOf(
            SingleToggleButtonSetting(
                R.id.showNetworkInfoDivider,
                R.id.showNetworkInfoTitle,
                R.string.show_network_info,
                R.id.showNetworkInfoButton,
            ),
            SingleToggleButtonSetting(
                R.id.alwaysScanPublicDivider,
                R.id.alwaysScanPublicTitle,
                R.string.always_scan_publicly,
                R.id.alwaysScanPublicButton,
            )
        )

        fun testClientSubMenuOpen(
            externalVesselDataCount: Int,
            expectedPort: String,
            expectedUpdateInterval: String,
            showingInfo: Boolean,
            alwaysPublic: Boolean,
        ) {
            scrollTo(R.id.vesselDataDivider)
            assertDisplayed(R.id.vesselDataTitle, R.string.vessel_data_xml_location)
            assertDisplayed(R.id.vesselDataOptions)
            assertDisplayed(R.id.vesselDataDefault, R.string.default_setting)

            listOf(
                R.id.vesselDataInternalStorage to R.string.vessel_data_internal,
                R.id.vesselDataExternalStorage to R.string.vessel_data_external,
            ).forEachIndexed { index, (id, label) ->
                if (index < externalVesselDataCount) {
                    assertDisplayed(id, label)
                } else {
                    assertNotDisplayed(id)
                }
            }

            clientSingleToggleSettings.zip(
                listOf(showingInfo, alwaysPublic),
            ).forEach { (setting, isChecked) -> setting.testSingleToggle(isChecked) }

            scrollTo(R.id.serverPortDivider)
            assertDisplayed(R.id.serverPortTitle, R.string.server_port)
            assertDisplayed(R.id.serverPortField, expectedPort)

            scrollTo(R.id.addressLimitDivider)
            assertDisplayed(R.id.addressLimitTitle, R.string.recent_address_limit)
            assertDisplayed(R.id.addressLimitEnableButton)

            scrollTo(R.id.updateIntervalDivider)
            assertDisplayed(R.id.updateIntervalTitle, R.string.update_interval)
            assertDisplayed(R.id.updateIntervalField, expectedUpdateInterval)
            assertDisplayed(R.id.updateIntervalMilliseconds, R.string.milliseconds)
        }

        fun testClientSubMenuClosed() {
            assertNotExist(R.id.vesselDataTitle)
            assertNotExist(R.id.vesselDataOptions)
            assertNotExist(R.id.vesselDataDefault)
            assertNotExist(R.id.vesselDataInternalStorage)
            assertNotExist(R.id.vesselDataExternalStorage)
            assertNotExist(R.id.vesselDataDivider)
            assertNotExist(R.id.serverPortTitle)
            assertNotExist(R.id.serverPortField)
            assertNotExist(R.id.serverPortDivider)
            clientSingleToggleSettings.forEach { it.testNotExist() }
            assertNotExist(R.id.addressLimitTitle)
            assertNotExist(R.id.addressLimitEnableButton)
            assertNotExist(R.id.addressLimitInfinity)
            assertNotExist(R.id.addressLimitField)
            assertNotExist(R.id.addressLimitDivider)
            assertNotExist(R.id.updateIntervalTitle)
            assertNotExist(R.id.updateIntervalField)
            assertNotExist(R.id.updateIntervalMilliseconds)
            assertNotExist(R.id.updateIntervalDivider)
        }
    }
}
