package artemis.agent.setup

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
import artemis.agent.isHidden
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
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ConnectFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun scanTest() = run {
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
                        isDisplayed()
                        hasText(R.string.scan)
                        isEnabled()
                    }
                    scanSpinner.isHidden()
                    noServersLabel.isDisplayedWithText(R.string.click_scan)
                    serverList.isDisplayedWithSize(0)
                }

                step("Scan for servers") {
                    scanButton {
                        click()
                        isDisabled()
                    }
                    scanSpinner.isDisplayed()
                    noServersLabel.isHidden()
                }

                step("Wait for scan to finish") {
                    Screen.idle(scanTimeout.get().seconds.inWholeMilliseconds)
                }

                step("No servers found") {
                    scanButton.isEnabled()
                    scanSpinner.isHidden()
                    noServersLabel.isDisplayedWithText(R.string.no_servers_found)
                    serverList.isDisplayedWithSize(0)
                }
            }
        }
    }

    @Test
    fun addressBarTest() = run {
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
                    connectSpinner.isHidden()
                }

                step("Write in address") {
                    addressBar.replaceText("127.0.0.1")
                    connectButton.isEnabled()
                }
            }
        }
    }

    @Test
    fun connectionSuccessTest() = run {
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
                    connectSpinner.isHidden()
                }
            }
        }
    }

    @Test
    fun connectionFailedTest() = run {
        mainScreenTest {
            scenario(ConnectScenario("127.0.0.1", activityScenarioRule.scenario))

            ConnectPageScreen {
                step("Failure state") {
                    connectLabel.isDisplayedWithText(R.string.failed_to_connect)
                    connectSpinner.isHidden()
                }
            }
        }
    }

    @Test
    fun showNetworkInfoTest() = run {
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

                    ConnectPageScreen.infoViews.forEachIndexed { viewIndex, view ->
                        val isNotEmpty = viewIndex > 0 || hasNetwork
                        view.isDisplayedIf(showing && isNotEmpty)
                    }
                }
            }
        }
    }

    companion object {
        const val FAKE_SERVER_IP = "noseynick.net"
    }
}
