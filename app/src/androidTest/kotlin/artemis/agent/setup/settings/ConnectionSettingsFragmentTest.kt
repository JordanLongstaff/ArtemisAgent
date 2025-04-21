package artemis.agent.setup.settings

import android.Manifest
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.StringRes
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
    fun connectionSettingsTitleBackTest() {
        testWithSettings { SettingsFragmentTest.closeSettingsSubMenu() }
    }

    @Test
    fun connectionSettingsBackButtonTest() {
        testWithSettings { SettingsFragmentTest.backFromSubMenu() }
    }

    private fun testWithSettings(closeSubMenu: () -> Unit) {
        val alwaysPublic = AtomicBoolean()

        activityScenarioManager.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value
            alwaysPublic.lazySet(viewModel.alwaysScanPublicBroadcasts)
        }

        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

        SettingsFragmentTest.openSettingsMenu()
        SettingsFragmentTest.openSettingsSubMenu(1)

        val timeInputSettings =
            listOf(
                TimeInputSetting(
                    divider = R.id.connectionTimeoutDivider,
                    title = R.id.connectionTimeoutTitle,
                    text = R.string.connection_timeout,
                    timeInput = R.id.connectionTimeoutTimeInput,
                    secondsLabel = R.id.connectionTimeoutSecondsLabel,
                ),
                TimeInputSetting(
                    divider = R.id.heartbeatTimeoutDivider,
                    title = R.id.heartbeatTimeoutTitle,
                    text = R.string.heartbeat_timeout,
                    timeInput = R.id.heartbeatTimeoutTimeInput,
                    secondsLabel = R.id.heartbeatTimeoutSecondsLabel,
                ),
                TimeInputSetting(
                    divider = R.id.scanTimeoutDivider,
                    title = R.id.scanTimeoutTitle,
                    text = R.string.scan_timeout,
                    timeInput = R.id.scanTimeoutTimeInput,
                    secondsLabel = R.id.scanTimeoutSecondsLabel,
                ),
            )

        timeInputSettings.forEach { it.testDisplayed() }

        alwaysScanPublicToggleSetting.testSingleToggle(alwaysPublic.get())

        closeSubMenu()
        timeInputSettings.forEach { it.testNotExist() }
        alwaysScanPublicToggleSetting.testNotExist()
    }

    private data class TimeInputSetting(
        @IdRes val divider: Int,
        @IdRes val title: Int,
        @StringRes val text: Int,
        @IdRes val timeInput: Int,
        @IdRes val secondsLabel: Int,
    ) {
        fun testDisplayed() {
            scrollTo(divider)
            assertDisplayed(title, text)
            assertDisplayed(timeInput)
            assertDisplayed(secondsLabel, R.string.seconds)
        }

        fun testNotExist() {
            assertNotExist(title)
            assertNotExist(timeInput)
            assertNotExist(secondsLabel)
            assertNotExist(divider)
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
