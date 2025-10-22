package artemis.agent.setup

import android.os.Build
import androidx.activity.viewModels
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.AgentViewModel
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.isDisplayedWithSize
import artemis.agent.isDisplayedWithText
import artemis.agent.isRemoved
import artemis.agent.scenario.ConnectScenario
import artemis.agent.scenario.SettingsMenuScenario
import artemis.agent.scenario.SettingsSubmenuOpenScenario
import artemis.agent.screens.ConnectPageScreen
import artemis.agent.screens.MainScreen.mainScreenTest
import artemis.agent.screens.SettingsPageScreen
import artemis.agent.screens.SetupPageScreen
import artemis.agent.screens.ShipsPageScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import dev.tmapps.konnection.Konnection
import io.github.kakaocup.kakao.screen.Screen
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ConnectFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = activityScenarioRule<MainActivity>()

    @Test
    fun scanTest() {
        run {
            mainScreenTest {
                step("Enable network connections") { device.network.enable() }

                val scanTimeout = AtomicInteger()
                step("Fetch scan timeout") {
                    activityScenarioRule.scenario.onActivity { activity ->
                        scanTimeout.lazySet(activity.viewModels<AgentViewModel>().value.scanTimeout)
                    }
                }

                ConnectPageScreen {
                    step("Initial state") {
                        scanButton {
                            isDisplayedWithText(R.string.scan)
                            isEnabled()
                        }
                        scanSpinner.isRemoved()
                        noServersLabel.isDisplayedWithText(R.string.click_scan)
                        serverList.isDisplayedWithSize(0)
                    }

                    step("Scan for servers") { scanButton.click() }

                    if (!isEmulator || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // These steps fail on CI with pre-Android 24 emulators for some reason
                        step("Check UI state") {
                            scanButton.isDisabled()
                            scanSpinner.isCompletelyDisplayed()
                            noServersLabel.isRemoved()
                        }

                        step("Wait for scan to finish") {
                            Screen.idle(scanTimeout.get().seconds.inWholeMilliseconds)
                        }
                    }

                    step("No servers found") {
                        scanButton.isEnabled()
                        scanSpinner.isRemoved()
                        noServersLabel.isDisplayedWithText(R.string.no_servers_found)
                        serverList.isDisplayedWithSize(0)
                    }
                }
            }
        }
    }

    @Test
    fun addressBarTest() {
        run {
            mainScreenTest {
                ConnectPageScreen {
                    step("Initial state") {
                        addressBar {
                            clearText()
                            isCompletelyDisplayed()
                            hasHint(R.string.address)
                        }
                        connectButton {
                            isDisplayedWithText(R.string.connect)
                            isDisabled()
                        }
                        connectLabel.isDisplayedWithText(R.string.not_connected)
                        connectSpinner.isRemoved()
                    }

                    step("Write in address") {
                        addressBar.replaceText("127.0.0.1")
                        connectButton.isEnabled()
                    }
                }
            }
        }
    }

    @Test
    fun connectionSuccessTest() {
        run {
            mainScreenTest(false) {
                scenario(ConnectScenario(FAKE_SERVER_IP, activityScenarioRule.scenario))

                step("Advance to Ships page") {
                    SetupPageScreen {
                        connectPageButton.isNotChecked()
                        shipsPageButton.isChecked()
                    }
                    ConnectPageScreen {
                        addressBar.doesNotExist()
                        connectButton.doesNotExist()
                        connectLabel.doesNotExist()
                        connectSpinner.doesNotExist()
                        scanButton.doesNotExist()
                        scanSpinner.doesNotExist()
                        noServersLabel.doesNotExist()
                        serverList.doesNotExist()
                    }
                    ShipsPageScreen.shipsList.isCompletelyDisplayed()
                }

                step("Check connection success state") {
                    SetupPageScreen.connectPageButton.click()
                    ConnectPageScreen {
                        connectLabel.isDisplayedWithText(R.string.connected)
                        connectSpinner.isRemoved()
                    }
                }
            }
        }
    }

    @Test
    fun connectionFailedTest() {
        run {
            mainScreenTest {
                scenario(ConnectScenario("127.0.0.1", activityScenarioRule.scenario))

                ConnectPageScreen {
                    step("Failure state") {
                        connectLabel.isDisplayedWithText(R.string.failed_to_connect)
                        connectSpinner.isRemoved()
                    }
                }
            }
        }
    }

    @Test
    fun showNetworkInfoTest() {
        run {
            mainScreenTest {
                step("Enable network connections") { device.network.enable() }

                val showingInfo = AtomicBoolean()
                step("Fetch showing network info setting") {
                    activityScenarioRule.scenario.onActivity { activity ->
                        showingInfo.lazySet(
                            activity.viewModels<AgentViewModel>().value.showingNetworkInfo
                        )
                    }
                }
                val settingValue = showingInfo.get()

                runTest(timeout = 5.minutes) {
                    val hasNetwork = !Konnection.instance.getInfo()?.ipv4.isNullOrBlank()

                    booleanArrayOf(settingValue, !settingValue, settingValue).forEachIndexed {
                        index,
                        showing ->
                        if (index != 0) toggleShowingInfo()
                        testShowingInfo(showing, hasNetwork)
                    }
                }
            }
        }
    }

    @Test
    fun noNetworkTest() {
        run {
            mainScreenTest {
                step("Disable network connections") { device.network.disable() }

                val showingInfo = AtomicBoolean()
                step("Fetch showing network info setting") {
                    activityScenarioRule.scenario.onActivity { activity ->
                        showingInfo.lazySet(
                            activity.viewModels<AgentViewModel>().value.showingNetworkInfo
                        )
                    }
                }
                val settingValue = showingInfo.get()

                if (!settingValue) {
                    toggleShowingInfo()
                }

                ConnectPageScreen {
                    step("Wait a bit") { Screen.idle(NO_NETWORK_IDLE) }

                    step("Check \"Network not found\" text") {
                        networkTypeLabel.isDisplayedWithText(R.string.network_not_found)
                    }

                    step("Check for no address") { addressLabel.isNotDisplayed() }
                }

                if (!settingValue) {
                    toggleShowingInfo()
                }
            }
        }
    }

    companion object {
        const val FAKE_SERVER_IP = "noseynick.net"

        private val EMULATOR_DEVICES = setOf("emu64x", "emulator64_x86_64", "generic_x86_64")

        private val isEmulator by lazy { Build.DEVICE in EMULATOR_DEVICES }

        private const val NO_NETWORK_IDLE = 2000L

        private fun TestContext<Unit>.toggleShowingInfo() {
            scenario(SettingsMenuScenario)
            scenario(SettingsSubmenuOpenScenario.Client)

            step("Toggle network info setting") {
                SettingsPageScreen.Client.showNetworkInfoButton.click()
            }

            step("Return to Connect page") { SetupPageScreen.connectPageButton.click() }
        }

        private fun TestContext<Unit>.testShowingInfo(isShowing: Boolean, hasNetwork: Boolean) {
            step("Network info views should ${if (isShowing) "" else "not "}be displayed") {
                ConnectPageScreen.infoViews.forEachIndexed { index, view ->
                    if (isShowing && (index > 0 || hasNetwork)) {
                        flakySafely(timeoutMs = 2.minutes.inWholeMilliseconds) {
                            view.isCompletelyDisplayed()
                        }
                    } else view.isNotDisplayed()
                }
            }
        }
    }
}
