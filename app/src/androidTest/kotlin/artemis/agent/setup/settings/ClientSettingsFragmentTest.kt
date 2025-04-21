package artemis.agent.setup.settings

import android.Manifest
import android.widget.ToggleButton
import androidx.activity.viewModels
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.ActivityScenarioManager
import artemis.agent.AgentViewModel
import artemis.agent.ArtemisAgentTestHelpers
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaAssertions.assertAny
import com.adevinta.android.barista.assertion.BaristaAssertions.assertThatBackButtonClosesTheApp
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
    fun clientSettingsMutableTest() {
        testWithSettings(true) { SettingsFragmentTest.closeSettingsSubMenu() }
    }

    @Test
    fun clientSettingsBackButtonTest() {
        testWithSettings(false) { SettingsFragmentTest.backFromSubMenu() }
    }

    private fun testWithSettings(shouldTestSettings: Boolean, closeSubMenu: () -> Unit) {
        val expectedPort = AtomicInteger()
        val expectedUpdateInterval = AtomicInteger()
        val vesselDataCount = AtomicInteger()
        val vesselDataIndex = AtomicInteger()
        val showingInfo = AtomicBoolean()

        activityScenarioManager.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value
            expectedPort.lazySet(viewModel.port)
            expectedUpdateInterval.lazySet(viewModel.updateObjectsInterval)
            vesselDataCount.lazySet(viewModel.vesselDataManager.count)
            vesselDataIndex.lazySet(viewModel.vesselDataManager.index)
            showingInfo.lazySet(viewModel.showingNetworkInfo)
        }

        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

        SettingsFragmentTest.openSettingsMenu()
        SettingsFragmentTest.openSettingsSubMenu(0)

        testClientSubMenuOpen(
            data =
                Data(
                    port = expectedPort.toString(),
                    updateInterval = expectedUpdateInterval.get(),
                    vesselDataIndex = vesselDataIndex.get(),
                    vesselDataCount = vesselDataCount.get(),
                    showingInfo = showingInfo.get(),
                ),
            shouldTestSettings = shouldTestSettings,
        )

        closeSubMenu()
        testClientSubMenuClosed()

        assertThatBackButtonClosesTheApp()
    }

    private data class Data(
        val port: String,
        val updateInterval: Int,
        val vesselDataIndex: Int,
        val vesselDataCount: Int,
        val showingInfo: Boolean,
    )

    private companion object {
        val showNetworkInfoToggleSetting =
            SingleToggleButtonSetting(
                divider = R.id.showNetworkInfoDivider,
                label = R.id.showNetworkInfoTitle,
                text = R.string.show_network_info,
                button = R.id.showNetworkInfoButton,
            )

        val vesselDataButtons by lazy {
            listOf(
                R.id.vesselDataDefault to R.string.default_setting,
                R.id.vesselDataInternalStorage to R.string.vessel_data_internal,
                R.id.vesselDataExternalStorage to R.string.vessel_data_external,
            )
        }

        fun testClientSubMenuOpen(data: Data, shouldTestSettings: Boolean) {
            scrollTo(R.id.vesselDataDivider)
            assertDisplayed(R.id.vesselDataTitle, R.string.vessel_data_xml_location)
            assertDisplayed(R.id.vesselDataOptions)
            assertDisplayed(R.id.vesselDataDefault, R.string.default_setting)

            testClientSubMenuVesselDataButtons(
                data.vesselDataIndex,
                data.vesselDataCount,
                shouldTestSettings,
            )

            showNetworkInfoToggleSetting.testSingleToggle(data.showingInfo)

            scrollTo(R.id.serverPortDivider)
            assertDisplayed(R.id.serverPortTitle, R.string.server_port)
            assertDisplayed(R.id.serverPortField, data.port)

            testClientSubMenuAddressLimit(shouldTestSettings)

            scrollTo(R.id.updateIntervalDivider)
            assertDisplayed(R.id.updateIntervalTitle, R.string.update_interval)
            assertDisplayed(R.id.updateIntervalField, data.updateInterval)
            assertDisplayed(R.id.updateIntervalMilliseconds, R.string.milliseconds)
        }

        fun testClientSubMenuClosed() {
            assertNotExist(R.id.vesselDataTitle)
            assertNotExist(R.id.vesselDataOptions)
            vesselDataButtons.forEach { (button) -> assertNotExist(button) }
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

        fun testClientSubMenuVesselDataButtons(index: Int, count: Int, shouldTest: Boolean) {
            vesselDataButtons.forEachIndexed { i, (id, label) ->
                if (i < count) assertDisplayed(id, label) else assertNotDisplayed(id)

                ArtemisAgentTestHelpers.assertChecked(id, i == index)
            }

            if (!shouldTest) return

            vesselDataButtons.forEachIndexed { i, (button) ->
                clickOn(button)
                vesselDataButtons.forEachIndexed { j, (otherButton) ->
                    ArtemisAgentTestHelpers.assertChecked(otherButton, i == j)
                }
            }

            clickOn(vesselDataButtons[index].first)
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
