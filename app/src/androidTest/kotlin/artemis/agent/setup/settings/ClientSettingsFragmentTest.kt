package artemis.agent.setup.settings

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.isCheckedIf
import artemis.agent.isDisplayedWithText
import artemis.agent.isRemoved
import artemis.agent.scenario.SettingsMenuScenario
import artemis.agent.scenario.SettingsSubmenuOpenScenario
import artemis.agent.screens.MainScreen.mainScreenTest
import artemis.agent.screens.SettingsPageScreen
import artemis.agent.withViewModel
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ClientSettingsFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun clientSettingsMutableTest() {
        testWithSettings(true) { SettingsPageScreen.closeSubmenu() }
    }

    @Test
    fun clientSettingsBackButtonTest() {
        testWithSettings(false) { SettingsPageScreen.backFromSubmenu() }
    }

    private fun testWithSettings(shouldTestSettings: Boolean, closeSubmenu: () -> Unit) {
        run {
            mainScreenTest {
                withViewModel { viewModel ->
                    val expectedPort = viewModel.port
                    val expectedUpdateInterval = viewModel.updateObjectsInterval
                    val vesselDataCount = viewModel.vesselDataManager.count
                    val vesselDataIndex = viewModel.vesselDataManager.index
                    val showingInfo = viewModel.showingNetworkInfo

                    scenario(SettingsMenuScenario)
                    scenario(SettingsSubmenuOpenScenario.Client)

                    testVesselDataSetting(
                        count = vesselDataCount,
                        index = vesselDataIndex,
                        shouldTest = shouldTestSettings,
                    )
                    testAddressFieldSetting(shouldTestSettings)

                    SettingsPageScreen.Client {
                        step("Test showing info setting") {
                            showNetworkInfoToggleSetting.testSingleToggle(showingInfo)
                        }

                        step("Test server port setting") {
                            serverPortDivider.scrollTo()
                            serverPortTitle.isDisplayedWithText(R.string.server_port)
                            serverPortField.isDisplayedWithText(expectedPort.toString())
                        }

                        step("Test update interval setting") {
                            updateIntervalDivider.scrollTo()
                            updateIntervalTitle.isDisplayedWithText(R.string.update_interval)
                            updateIntervalField.isDisplayedWithText(
                                expectedUpdateInterval.toString()
                            )
                            updateIntervalMilliseconds.isDisplayedWithText(R.string.milliseconds)
                        }

                        step("Close submenu") {
                            closeSubmenu()
                            testScreenClosed()
                        }
                    }
                }
            }
        }
    }

    private companion object {
        val vesselDataButtons by lazy {
            listOf(
                SettingsPageScreen.Client.vesselDataDefaultButton to R.string.default_setting,
                SettingsPageScreen.Client.vesselDataInternalButton to R.string.vessel_data_internal,
                SettingsPageScreen.Client.vesselDataExternalButton to R.string.vessel_data_external,
            )
        }

        fun TestContext<*>.testVesselDataSetting(count: Int, index: Int, shouldTest: Boolean) {
            SettingsPageScreen.Client {
                step("Vessel data settings") {
                    step("Title displayed") {
                        vesselDataDivider.scrollTo()
                        vesselDataTitle.isDisplayedWithText(R.string.vessel_data_xml_location)
                    }

                    step("Correct number of options displayed") {
                        vesselDataButtons.forEachIndexed { i, (button, label) ->
                            if (i < count) button.isDisplayedWithText(label) else button.isRemoved()
                        }
                    }

                    step("Correct option is selected") {
                        vesselDataButtons.forEachIndexed { i, (button) ->
                            button.isCheckedIf(i == index)
                        }
                    }

                    if (shouldTest) {
                        step("Select different options") {
                            vesselDataButtons.take(count).forEachIndexed { i, (button) ->
                                button.click()
                                vesselDataButtons.forEachIndexed { j, (otherButton) ->
                                    otherButton.isCheckedIf(i == j)
                                }
                            }
                        }

                        step("Revert setting") { vesselDataButtons[index].first.click() }
                    }
                }
            }
        }

        fun TestContext<*>.testAddressFieldSetting(shouldTest: Boolean) {
            step("Recent address limit setting") {
                SettingsPageScreen.Client {
                    step("Title displayed") {
                        addressLimitDivider.scrollTo()
                        addressLimitTitle.isDisplayedWithText(R.string.recent_address_limit)
                        addressLimitEnableButton.isDisplayed()
                    }

                    step("UI state") {
                        repeat(if (shouldTest) 3 else 1) { i ->
                            if (i > 0) addressLimitEnableButton.click()

                            compose {
                                or(addressLimitEnableButton) {
                                    isChecked()
                                    this@Client.testAddressLimitFieldDisplayState(true)
                                }
                                or(addressLimitEnableButton) {
                                    isNotChecked()
                                    this@Client.testAddressLimitFieldDisplayState(false)
                                }
                            }
                        }
                    }
                }
            }
        }

        fun SettingsPageScreen.Client.testAddressLimitFieldDisplayState(isChecked: Boolean) {
            if (isChecked) {
                addressLimitField.isDisplayed()
                addressLimitInfinity.isRemoved()
            } else {
                addressLimitField.isRemoved()
                addressLimitInfinity.isDisplayedWithText(R.string.infinity)
            }
        }

        fun SettingsPageScreen.Client.testScreenClosed() {
            vesselDataTitle.doesNotExist()
            vesselDataButtons.forEach { (button) -> button.doesNotExist() }
            vesselDataDivider.doesNotExist()
            serverPortTitle.doesNotExist()
            serverPortField.doesNotExist()
            serverPortDivider.doesNotExist()
            showNetworkInfoToggleSetting.testNotExist()
            addressLimitTitle.doesNotExist()
            addressLimitEnableButton.doesNotExist()
            addressLimitInfinity.doesNotExist()
            addressLimitField.doesNotExist()
            addressLimitDivider.doesNotExist()
            updateIntervalTitle.doesNotExist()
            updateIntervalField.doesNotExist()
            updateIntervalMilliseconds.doesNotExist()
            updateIntervalDivider.doesNotExist()
        }
    }
}
