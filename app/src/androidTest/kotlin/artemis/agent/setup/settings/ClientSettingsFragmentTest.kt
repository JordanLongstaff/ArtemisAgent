package artemis.agent.setup.settings

import android.Manifest
import android.widget.ToggleButton
import androidx.activity.viewModels
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.ActivityScenarioManager
import artemis.agent.AgentViewModel
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaAssertions.assertAny
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo
import com.adevinta.android.barista.interaction.PermissionGranter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ClientSettingsFragmentTest {
    @get:Rule val activityScenarioManager = ActivityScenarioManager.forActivity<MainActivity>()

    @Test
    fun clientSettingsTest() {
        val expectedPort = AtomicInteger()
        val expectedUpdateInterval = AtomicInteger()
        val externalVesselDataCount = AtomicInteger()
        val showingInfo = AtomicBoolean()

        activityScenarioManager.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value
            expectedPort.lazySet(viewModel.port)
            expectedUpdateInterval.lazySet(viewModel.updateObjectsInterval)
            externalVesselDataCount.lazySet(viewModel.vesselDataManager.externalCount)
            showingInfo.lazySet(viewModel.showingNetworkInfo)
        }

        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

        SettingsFragmentTest.openSettingsMenu()

        listOf(
                { SettingsFragmentTest.closeSettingsSubMenu() },
                { SettingsFragmentTest.backFromSubMenu() },
            )
            .forEachIndexed { index, closeSubMenu ->
                SettingsFragmentTest.openSettingsSubMenu(0)
                testClientSubMenuOpen(
                    externalVesselDataCount = externalVesselDataCount.get(),
                    expectedPort = expectedPort.toString(),
                    expectedUpdateInterval = expectedUpdateInterval.toString(),
                    showingInfo = showingInfo.get(),
                    shouldTestAddressLimitButton = index == 0,
                )

                closeSubMenu()
                testClientSubMenuClosed()
            }
    }

    private companion object {
        val showNetworkInfoToggleSetting =
            SingleToggleButtonSetting(
                divider = R.id.showNetworkInfoDivider,
                label = R.id.showNetworkInfoTitle,
                text = R.string.show_network_info,
                button = R.id.showNetworkInfoButton,
            )

        fun testClientSubMenuOpen(
            externalVesselDataCount: Int,
            expectedPort: String,
            expectedUpdateInterval: String,
            showingInfo: Boolean,
            shouldTestAddressLimitButton: Boolean,
        ) {
            scrollTo(R.id.vesselDataDivider)
            assertDisplayed(R.id.vesselDataTitle, R.string.vessel_data_xml_location)
            assertDisplayed(R.id.vesselDataOptions)
            assertDisplayed(R.id.vesselDataDefault, R.string.default_setting)

            listOf(
                    R.id.vesselDataInternalStorage to R.string.vessel_data_internal,
                    R.id.vesselDataExternalStorage to R.string.vessel_data_external,
                )
                .forEachIndexed { index, (id, label) ->
                    if (index < externalVesselDataCount) assertDisplayed(id, label)
                    else assertNotDisplayed(id)
                }

            showNetworkInfoToggleSetting.testSingleToggle(showingInfo)

            scrollTo(R.id.serverPortDivider)
            assertDisplayed(R.id.serverPortTitle, R.string.server_port)
            assertDisplayed(R.id.serverPortField, expectedPort)

            testClientSubMenuAddressLimit(shouldTestAddressLimitButton)

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
            showNetworkInfoToggleSetting.testNotExist()
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

        fun testClientSubMenuAddressLimit(shouldTest: Boolean) {
            scrollTo(R.id.addressLimitDivider)
            assertDisplayed(R.id.addressLimitTitle, R.string.recent_address_limit)
            assertDisplayed(R.id.addressLimitEnableButton)

            assertAny<ToggleButton>(R.id.addressLimitEnableButton) { button ->
                testClientSubMenuAddressLimitField(button.isChecked)

                if (shouldTest) {
                    booleanArrayOf(false, true).forEach { shouldBeEnabled ->
                        clickOn(R.id.addressLimitEnableButton)
                        testClientSubMenuAddressLimitField(button.isChecked == shouldBeEnabled)
                    }
                }

                true
            }
        }

        fun testClientSubMenuAddressLimitField(isEnabled: Boolean) {
            if (isEnabled) {
                assertChecked(R.id.addressLimitEnableButton)
                assertNotDisplayed(R.id.addressLimitInfinity)
                assertDisplayed(R.id.addressLimitField)
            } else {
                assertUnchecked(R.id.addressLimitEnableButton)
                assertDisplayed(R.id.addressLimitInfinity, R.string.infinity)
                assertNotDisplayed(R.id.addressLimitField)
            }
        }
    }
}
