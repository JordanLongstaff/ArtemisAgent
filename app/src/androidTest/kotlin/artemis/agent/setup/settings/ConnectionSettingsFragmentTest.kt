package artemis.agent.setup.settings

import android.Manifest
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
import com.adevinta.android.barista.interaction.PermissionGranter
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ConnectionSettingsFragmentTest {
    @get:Rule val activityScenarioManager = ActivityScenarioManager.forActivity<MainActivity>()

    @Test
    fun connectionSettingsTest() {
        val alwaysPublic = AtomicBoolean()

        activityScenarioManager.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value
            alwaysPublic.lazySet(viewModel.alwaysScanPublicBroadcasts)
        }

        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

        SettingsFragmentTest.openSettingsMenu()

        listOf(
                { SettingsFragmentTest.closeSettingsSubMenu() },
                { SettingsFragmentTest.backFromSubMenu() },
            )
            .forEach { closeSubMenu ->
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

                closeSubMenu()
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
    }

    private companion object {
        val alwaysScanPublicToggleSetting =
            SingleToggleButtonSetting(
                divider = R.id.alwaysScanPublicDivider,
                label = R.id.alwaysScanPublicTitle,
                text = R.string.always_scan_publicly,
                button = R.id.alwaysScanPublicButton,
            )
    }
}
