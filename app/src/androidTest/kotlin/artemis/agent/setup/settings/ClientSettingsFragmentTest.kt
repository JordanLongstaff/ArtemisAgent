package artemis.agent.setup.settings

import android.os.Build
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.AgentViewModel
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.isCheckedIf
import artemis.agent.isDisplayedWithText
import artemis.agent.isRemoved
import artemis.agent.scenario.ConnectScenario
import artemis.agent.scenario.SettingsMenuScenario
import artemis.agent.scenario.SettingsSubmenuOpenScenario
import artemis.agent.screens.ConnectPageScreen
import artemis.agent.screens.MainScreen
import artemis.agent.screens.MainScreen.mainScreenTest
import artemis.agent.screens.SettingsPageScreen
import artemis.agent.screens.SetupPageScreen
import artemis.agent.setup.ConnectFragmentTest
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import io.github.kakaocup.kakao.dialog.KAlertDialog
import io.github.kakaocup.kakao.text.KButton
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt
import kotlin.random.Random
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ClientSettingsFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = activityScenarioRule<MainActivity>()

    @Test
    fun clientSettingsMutableTest() {
        testWithSettings(true) { SettingsPageScreen.closeSubmenu() }
    }

    @Test
    fun clientSettingsBackButtonTest() {
        testWithSettings(false) { SettingsPageScreen.backFromSubmenu() }
    }

    @Test
    fun vesselDataWarningTest() {
        testVesselDataWarningDialog(shouldChange = false, connectedString = R.string.connected) {
            negativeButton
        }
    }

    @Test
    fun vesselDataDisconnectTest() {
        testVesselDataWarningDialog(shouldChange = true, connectedString = R.string.not_connected) {
            positiveButton
        }
    }

    private fun testWithSettings(shouldTestSettings: Boolean, closeSubmenu: () -> Unit) {
        run {
            mainScreenTest {
                val expectedPort = AtomicInteger()
                val expectedUpdateInterval = AtomicInteger()
                val vesselDataCount = AtomicInteger()
                val vesselDataIndex = AtomicInteger()
                val showingInfo = AtomicBoolean()

                step("Fetch settings") {
                    activityScenarioRule.scenario.onActivity { activity ->
                        val viewModel = activity.viewModels<AgentViewModel>().value
                        expectedPort.lazySet(viewModel.port)
                        expectedUpdateInterval.lazySet(viewModel.updateObjectsInterval)
                        vesselDataCount.lazySet(viewModel.vesselDataManager.count)
                        vesselDataIndex.lazySet(viewModel.vesselDataManager.index)
                        showingInfo.lazySet(viewModel.showingNetworkInfo)
                    }
                }

                scenario(SettingsMenuScenario)
                scenario(SettingsSubmenuOpenScenario.Client)

                testVesselDataSetting(
                    count = vesselDataCount.get(),
                    index = vesselDataIndex.get(),
                    shouldTest = shouldTestSettings,
                )
                testAddressFieldSetting(shouldTestSettings)
                testServerPortSetting(expectedPort.get(), shouldTestSettings)

                step("Test showing info setting") {
                    SettingsPageScreen.Client.showNetworkInfoToggleSetting.testSingleToggle(
                        showingInfo.get()
                    )
                }

                testUpdateIntervalSetting(expectedUpdateInterval.get(), shouldTestSettings)

                SettingsPageScreen.Client {
                    step("Close submenu") {
                        closeSubmenu()
                        testScreenClosed()
                    }
                }
            }
        }
    }

    private fun testVesselDataWarningDialog(
        shouldChange: Boolean,
        @StringRes connectedString: Int,
        button: KAlertDialog.() -> KButton,
    ) {
        run {
            mainScreenTest(shouldChange) {
                val vesselDataCount = AtomicInteger()

                step("Fetch settings") {
                    activityScenarioRule.scenario.onActivity { activity ->
                        val viewModel = activity.viewModels<AgentViewModel>().value
                        val count = viewModel.vesselDataManager.count

                        Assume.assumeTrue("No alternate vessel data options", count > 1)
                        vesselDataCount.lazySet(count)
                    }
                }

                for (index in 1 until vesselDataCount.get()) {
                    if (shouldChange || index == 1) {
                        scenario(
                            ConnectScenario(
                                ConnectFragmentTest.FAKE_SERVER_IP,
                                activityScenarioRule.scenario,
                            )
                        )
                    }

                    scenario(SettingsMenuScenario)
                    scenario(SettingsSubmenuOpenScenario.Client)

                    step("Disclaimer should be showing") {
                        SettingsPageScreen.Client.vesselDataDisclaimer.isDisplayedWithText(
                            R.string.vessel_data_setting_disclaimer
                        )
                    }

                    step("Select option #${index + 1}") { vesselDataButtons[index].first.click() }

                    MainScreen {
                        step("Warning dialog should be displayed") { assertVesselDataWarningOpen() }

                        step("Dismiss warning dialog") { alertDialog.button().click() }
                    }

                    step("Check vessel data setting") {
                        val expectedIndex = if (shouldChange) index else 0
                        vesselDataButtons.forEachIndexed { i, (button) ->
                            button.isCheckedIf(i == expectedIndex)
                        }
                    }

                    step("Check connection status") {
                        SetupPageScreen.connectPageButton.click()
                        ConnectPageScreen.connectLabel.isDisplayedWithText(connectedString)
                    }
                }
            }
        }
    }

    private companion object {
        const val PORT_TEST_COUNT = 5
        const val INTERVAL_TEST_COUNT = 20
        const val BASE_MAX_PROGRESS = 100f
        const val MAX_INTERVAL = 500
        const val PROGRESS_SCALE = 5

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

                    step("Disclaimer not displayed") { vesselDataDisclaimer.isRemoved() }

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

        fun TestContext<*>.testServerPortSetting(expectedPort: Int, shouldTestReset: Boolean) {
            val expectedPortText = expectedPort.toString()

            SettingsPageScreen.Client {
                step("Test server port setting") {
                    serverPortDivider.scrollTo()
                    serverPortTitle.isDisplayedWithText(R.string.server_port)
                    serverPortField.isDisplayedWithText(expectedPortText)
                    serverPortInfo.isDisplayedWithText(R.string.server_port_info)
                    serverPortResetButton.isDisplayedWithText(R.string.reset)
                }

                if (!shouldTestReset) return@Client

                step("Test resetting port") {
                    repeat(PORT_TEST_COUNT) {
                        serverPortField.replaceText(
                            Random.nextInt(UShort.MAX_VALUE.toInt()).toString()
                        )
                        serverPortResetButton.click()
                        serverPortField.isDisplayedWithText("2010")
                        serverPortField.replaceText(expectedPortText)
                    }
                }
            }
        }

        fun TestContext<*>.testAddressFieldSetting(shouldTest: Boolean) {
            step("Recent address limit setting") {
                SettingsPageScreen.Client {
                    step("Title displayed") {
                        addressLimitDivider.scrollTo()
                        addressLimitTitle.isDisplayedWithText(R.string.server_memory_limit)
                        addressLimitEnableButton.isCompletelyDisplayed()
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
                addressLimitField.isCompletelyDisplayed()
                addressLimitInfinity.isRemoved()
            } else {
                addressLimitField.isRemoved()
                addressLimitInfinity.isDisplayedWithText(R.string.infinity)
            }
        }

        fun TestContext<*>.testUpdateIntervalSetting(expectedInterval: Int, shouldTest: Boolean) {
            SettingsPageScreen.Client {
                step("Test update interval setting") {
                    updateIntervalDivider.scrollTo()
                    updateIntervalTitle.isDisplayedWithText(R.string.update_interval)
                    updateIntervalMilliseconds.isDisplayedWithText(R.string.milliseconds)
                    updateIntervalDisclaimer.isDisplayedWithText(
                        R.string.update_interval_disclaimer
                    )
                    updateIntervalBar {
                        isCompletelyDisplayed()
                        hasProgress(getProgressScale(expectedInterval))
                    }
                }

                if (!shouldTest) return@Client

                step("Test changing update interval") {
                    val intervalTests =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            List(INTERVAL_TEST_COUNT) { Random.nextInt(MAX_INTERVAL + 1) }
                        } else {
                            List(INTERVAL_TEST_COUNT) {
                                Random.nextInt(MAX_INTERVAL / PROGRESS_SCALE + 1) * PROGRESS_SCALE
                            }
                        } + expectedInterval

                    intervalTests.forEach { interval ->
                        updateIntervalBar.setProgress(getProgressScale(interval))
                        updateIntervalLabel.isDisplayedWithText(interval.toString())
                    }
                }
            }
        }

        fun SettingsPageScreen.Client.testScreenClosed() {
            vesselDataTitle.doesNotExist()
            vesselDataButtons.forEach { (button) -> button.doesNotExist() }
            vesselDataDisclaimer.doesNotExist()
            vesselDataDivider.doesNotExist()
            serverPortTitle.doesNotExist()
            serverPortField.doesNotExist()
            serverPortResetButton.doesNotExist()
            serverPortInfo.doesNotExist()
            serverPortDivider.doesNotExist()
            showNetworkInfoToggleSetting.testNotExist()
            addressLimitTitle.doesNotExist()
            addressLimitEnableButton.doesNotExist()
            addressLimitInfinity.doesNotExist()
            addressLimitField.doesNotExist()
            addressLimitDivider.doesNotExist()
            updateIntervalTitle.doesNotExist()
            updateIntervalLabel.doesNotExist()
            updateIntervalMilliseconds.doesNotExist()
            updateIntervalBar.doesNotExist()
            updateIntervalDisclaimer.doesNotExist()
            updateIntervalDivider.doesNotExist()
        }

        fun getProgressScale(progress: Int): Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                progress
            } else {
                (progress * BASE_MAX_PROGRESS / MAX_INTERVAL).roundToInt()
            }
    }
}
