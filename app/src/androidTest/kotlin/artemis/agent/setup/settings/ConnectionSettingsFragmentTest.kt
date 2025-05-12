package artemis.agent.setup.settings

import androidx.annotation.StringRes
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.isDisplayedWithText
import artemis.agent.scenario.SettingsMenuScenario
import artemis.agent.scenario.SettingsSubmenuOpenScenario
import artemis.agent.scenario.TimeInputTestScenario
import artemis.agent.screens.MainScreen.mainScreenTest
import artemis.agent.screens.SettingsPageScreen
import artemis.agent.withViewModel
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.text.KTextView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ConnectionSettingsFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun connectionSettingsTimeInputTest() {
        testWithSettings(true) { SettingsPageScreen.closeSubmenu() }
    }

    @Test
    fun connectionSettingsBackButtonTest() {
        testWithSettings(false) { SettingsPageScreen.backFromSubmenu() }
    }

    private fun testWithSettings(shouldTestTimeInputs: Boolean, closeSubmenu: () -> Unit) {
        run {
            mainScreenTest {
                withViewModel { viewModel ->
                    val alwaysPublic = viewModel.alwaysScanPublicBroadcasts
                    val connectTimeout = viewModel.connectTimeout
                    val scanTimeout = viewModel.scanTimeout
                    val heartbeatTimeout = viewModel.heartbeatTimeout

                    scenario(SettingsMenuScenario)
                    scenario(SettingsSubmenuOpenScenario.Connection)

                    SettingsPageScreen.Connection {
                        val timeInputSettings =
                            buildTimeInputSettings(
                                connectTimeout = connectTimeout,
                                heartbeatTimeout = heartbeatTimeout,
                                scanTimeout = scanTimeout,
                            )

                        timeInputSettings.forEach { setting ->
                            val settingName = device.targetContext.getString(setting.text)
                            step(settingName) {
                                step("Components should be displayed") { setting.testDisplayed() }

                                if (shouldTestTimeInputs) {
                                    step("Test changing time") {
                                        scenario(
                                            TimeInputTestScenario(
                                                timeInput = setting.timeInput,
                                                seconds = setting.initialSeconds,
                                                includeMinutes = false,
                                                minimumSeconds = 1,
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        step("Check public scan setting components") {
                            alwaysScanPublicToggleSetting.testSingleToggle(alwaysPublic)
                        }

                        step("Close submenu") { closeSubmenu() }

                        step("All settings should be gone") {
                            timeInputSettings.forEach { setting -> setting.testNotExist() }
                            alwaysScanPublicToggleSetting.testNotExist()
                        }
                    }
                }
            }
        }
    }

    private data class TimeInputSetting(
        val divider: KView,
        val title: KTextView,
        @StringRes val text: Int,
        val timeInput: KTimeInputBinder,
        val secondsLabel: KTextView,
        val initialSeconds: Int,
    ) {
        fun testDisplayed() {
            divider.scrollTo()
            title.isDisplayedWithText(text)
            timeInput.isDisplayed(withMinutes = false)
            secondsLabel.isDisplayedWithText(R.string.seconds)
        }

        fun testNotExist() {
            title.doesNotExist()
            timeInput.doesNotExist()
            secondsLabel.doesNotExist()
            divider.doesNotExist()
        }
    }

    private companion object {
        fun SettingsPageScreen.Connection.buildTimeInputSettings(
            connectTimeout: Int,
            heartbeatTimeout: Int,
            scanTimeout: Int,
        ): List<TimeInputSetting> = buildList {
            add(
                TimeInputSetting(
                    divider = connectionTimeoutDivider,
                    title = connectionTimeoutTitle,
                    text = R.string.connection_timeout,
                    timeInput = connectionTimeoutTimeInput,
                    secondsLabel = connectionTimeoutSecondsLabel,
                    initialSeconds = connectTimeout,
                )
            )
            add(
                TimeInputSetting(
                    divider = heartbeatTimeoutDivider,
                    title = heartbeatTimeoutTitle,
                    text = R.string.heartbeat_timeout,
                    timeInput = heartbeatTimeoutTimeInput,
                    secondsLabel = heartbeatTimeoutSecondsLabel,
                    initialSeconds = heartbeatTimeout,
                )
            )
            add(
                TimeInputSetting(
                    divider = scanTimeoutDivider,
                    title = scanTimeoutTitle,
                    text = R.string.scan_timeout,
                    timeInput = scanTimeoutTimeInput,
                    secondsLabel = scanTimeoutSecondsLabel,
                    initialSeconds = scanTimeout,
                )
            )
        }
    }
}
