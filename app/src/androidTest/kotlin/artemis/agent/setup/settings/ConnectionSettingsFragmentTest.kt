package artemis.agent.setup.settings

import androidx.activity.viewModels
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.ActivityScenarioManager
import artemis.agent.AgentViewModel
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
@LargeTest
class ConnectionSettingsFragmentTest {
    @get:Rule
    val activityScenarioManager = ActivityScenarioManager.forActivity<MainActivity>()

    @Test
    fun connectionSettingsTest() {
        val alwaysPublic = AtomicBoolean()

        activityScenarioManager.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value
            alwaysPublic.lazySet(viewModel.alwaysScanPublicBroadcasts)
        }

        SettingsFragmentTest.openSettingsMenu()
        SettingsFragmentTest.openSettingsSubMenu(1)

        scrollTo(R.id.connectionTimeoutDivider)
        assertDisplayed(R.id.connectionTimeoutTitle, R.string.connection_timeout)
        assertDisplayed(R.id.connectionTimeoutTimeInput)
        assertDisplayed(R.id.connectionTimeoutSecondsLabel, R.string.seconds)

        scrollTo(R.id.heartbeatTimeoutDivider)
        assertDisplayed(R.id.heartbeatTimeoutTitle, R.string.heartbeat_timeout)
        assertDisplayed(R.id.heartbeatTimeoutTimeInput)
        assertDisplayed(R.id.heartbeatTimeoutSecondsLabel, R.string.seconds)

        scrollTo(R.id.scanTimeoutDivider)
        assertDisplayed(R.id.scanTimeoutTitle, R.string.scan_timeout)
        assertDisplayed(R.id.scanTimeoutTimeInput)
        assertDisplayed(R.id.scanTimeoutSecondsLabel, R.string.seconds)

        alwaysScanPublicToggleSetting.testSingleToggle(alwaysPublic.get())

        SettingsFragmentTest.closeSettingsSubMenu()
        assertNotExist(R.id.connectionTimeoutTitle)
        assertNotExist(R.id.connectionTimeoutTimeInput)
        assertNotExist(R.id.connectionTimeoutSecondsLabel)
        assertNotExist(R.id.connectionTimeoutDivider)
        assertNotExist(R.id.heartbeatTimeoutTitle)
        assertNotExist(R.id.heartbeatTimeoutTimeInput)
        assertNotExist(R.id.heartbeatTimeoutSecondsLabel)
        assertNotExist(R.id.heartbeatTimeoutDivider)
        assertNotExist(R.id.scanTimeoutTitle)
        assertNotExist(R.id.scanTimeoutTimeInput)
        assertNotExist(R.id.scanTimeoutSecondsLabel)
        assertNotExist(R.id.scanTimeoutDivider)
        alwaysScanPublicToggleSetting.testNotExist()
    }

    private companion object {
        val alwaysScanPublicToggleSetting = SingleToggleButtonSetting(
            R.id.alwaysScanPublicDivider,
            R.id.alwaysScanPublicTitle,
            R.string.always_scan_publicly,
            R.id.alwaysScanPublicButton,
        )
    }
}
