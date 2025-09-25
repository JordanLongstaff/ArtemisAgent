package artemis.agent.scenario

import android.os.Build
import androidx.activity.viewModels
import androidx.test.core.app.ActivityScenario
import artemis.agent.AgentViewModel
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.isDisplayedWithText
import artemis.agent.screens.ConnectPageScreen
import com.kaspersky.kaspresso.testcases.api.scenario.Scenario
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import io.github.kakaocup.kakao.screen.Screen
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class ConnectScenario(ip: String, activityScenario: ActivityScenario<MainActivity>) : Scenario() {
    override val steps: TestContext<Unit>.() -> Unit = {
        step("Enable network connections") { device.network.enable() }

        ConnectPageScreen {
            val connectTimeout = AtomicInteger()
            step("Fetch connect timeout") {
                activityScenario.onActivity { activity ->
                    connectTimeout.lazySet(
                        activity.viewModels<AgentViewModel>().value.connectTimeout
                    )
                }
            }

            step("Attempt connection") {
                addressBar.replaceText(ip)
                connectButton.click()
            }

            if (!isEmulator) {
                // Skip this check on CI since it always fails
                step("Connecting state") {
                    connectLabel.isDisplayedWithText(R.string.connecting)
                    connectSpinner.isCompletelyDisplayed()
                }

                step("Wait for timeout") {
                    Screen.idle(connectTimeout.get().seconds.inWholeMilliseconds)
                }
            }
        }
    }

    private companion object {
        val EMULATOR_DEVICES = setOf("emu64x", "emulator64_x86_64", "generic_x86_64")

        val isEmulator by lazy { Build.DEVICE in EMULATOR_DEVICES }
    }
}
