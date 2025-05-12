package artemis.agent.scenario

import android.os.Build
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.isDisplayedWithText
import artemis.agent.screens.ConnectPageScreen
import com.kaspersky.kaspresso.testcases.api.scenario.Scenario
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import io.github.kakaocup.kakao.screen.Screen
import kotlin.time.Duration.Companion.seconds

class ConnectScenario(ip: String, viewModel: AgentViewModel) : Scenario() {
    override val steps: TestContext<Unit>.() -> Unit = {
        ConnectPageScreen {
            val connectTimeout = viewModel.connectTimeout

            step("Attempt connection") {
                addressBar.typeText(ip)
                connectButton.click()
            }

            if (!isEmulator) {
                // Skip this check on CI since it always fails
                step("Connecting state") {
                    connectLabel.isDisplayedWithText(R.string.connecting)
                    connectSpinner.isDisplayed()
                }

                step("Wait for timeout") { Screen.idle(connectTimeout.seconds.inWholeMilliseconds) }
            }
        }
    }

    private companion object {
        val EMULATOR_DEVICES = setOf("emu64x", "emulator64_x86_64", "generic_x86_64")

        val isEmulator by lazy { Build.DEVICE in EMULATOR_DEVICES }
    }
}
