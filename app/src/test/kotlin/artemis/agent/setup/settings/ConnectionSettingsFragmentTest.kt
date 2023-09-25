package artemis.agent.setup.settings

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ConnectionSettingsFragmentTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun connectionSettingsTest() {
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
    }
}
