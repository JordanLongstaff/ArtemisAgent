package artemis.agent.setup

import androidx.activity.viewModels
import artemis.agent.AgentViewModel
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertDisabled
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertEnabled
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.clearText
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ConnectFragmentTest {
    @Test
    fun scanTest() {
        Robolectric.buildActivity(MainActivity::class.java).use {
            it.setup()

            val activity = it.get()
            val scanTimeout = activity.viewModels<AgentViewModel>().value.scanTimeout
            val shadowLooper = ShadowLooper.shadowMainLooper()

            assertEnabled(R.id.scanButton)
            assertNotDisplayed(R.id.scanSpinner)

            clickOn(R.id.scanButton)
            shadowLooper.idle()

            assertDisabled(R.id.scanButton)
            assertDisplayed(R.id.scanSpinner)

            shadowLooper.idleFor(scanTimeout.toLong(), TimeUnit.SECONDS)
            shadowLooper.idle()

            assertEnabled(R.id.scanButton)
            assertNotDisplayed(R.id.scanSpinner)
        }
    }

    @Test
    fun connectButtonTest() {
        Robolectric.buildActivity(MainActivity::class.java).use {
            it.setup()

            clearText(R.id.addressBar)
            assertDisabled(R.id.connectButton)

            assertDisplayed(R.id.connectLabel, R.string.not_connected)
            assertNotDisplayed(R.id.connectSpinner)

            writeTo(R.id.addressBar, "127.0.0.1")
            assertEnabled(R.id.connectButton)
        }
    }
}
