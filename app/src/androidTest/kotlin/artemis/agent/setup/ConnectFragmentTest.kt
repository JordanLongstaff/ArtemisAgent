package artemis.agent.setup

import android.os.Build
import androidx.activity.viewModels
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.AgentViewModel
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.isDisplayedIf
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
import dev.tmapps.konnection.Konnection
import io.github.kakaocup.kakao.screen.Screen
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ConnectFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun scanTest() {
        run {
            mainScreenTest {
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
                            scanSpinner.isDisplayed()
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
                            isDisplayed()
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
                    ShipsPageScreen.shipsList.isDisplayed()
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
                val showingInfo = AtomicBoolean()
                step("Fetch showing network info setting") {
                    activityScenarioRule.scenario.onActivity { activity ->
                        showingInfo.lazySet(
                            activity.viewModels<AgentViewModel>().value.showingNetworkInfo
                        )
                    }
                }

                runTest {
                    val hasNetwork = !Konnection.instance.getInfo()?.ipv4.isNullOrBlank()
                    val settingValue = showingInfo.get()

                    booleanArrayOf(settingValue, !settingValue, settingValue).forEachIndexed {
                        index,
                        showing ->
                        if (index != 0) {
                            scenario(SettingsMenuScenario)
                            scenario(SettingsSubmenuOpenScenario.Client)
                            SettingsPageScreen.Client.showNetworkInfoButton.click()
                            SetupPageScreen.connectPageButton.click()
                        }

                        delay(300L)

                        ConnectPageScreen.infoViews.forEachIndexed { viewIndex, view ->
                            val isNotEmpty = viewIndex > 0 || hasNetwork
                            view.isDisplayedIf(showing && isNotEmpty)
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val FAKE_SERVER_IP = "noseynick.net"

        private val EMULATOR_DEVICES = setOf("emu64x", "emulator64_x86_64", "generic_x86_64")

        private val isEmulator by lazy { Build.DEVICE in EMULATOR_DEVICES }
    }
}
