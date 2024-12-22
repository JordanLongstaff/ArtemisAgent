package artemis.agent.setup

import androidx.activity.viewModels
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.AgentViewModel
import artemis.agent.ArtemisAgentTestHelpers
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.setup.settings.SettingsFragmentTest
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertDisabled
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertEnabled
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.clearText
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import dev.tmapps.konnection.Konnection
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
@LargeTest
class ConnectFragmentTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun scanTest() {
        val scanTimeout = AtomicInteger()
        activityScenarioRule.scenario.onActivity { activity ->
            scanTimeout.lazySet(activity.viewModels<AgentViewModel>().value.scanTimeout)
        }

        assertEnabled(R.id.scanButton)
        assertNotDisplayed(R.id.scanSpinner)
        assertDisplayed(R.id.noServersLabel, R.string.click_scan)
        assertDisplayed(R.id.serverList)
        assertRecyclerViewItemCount(R.id.serverList, 0)

        clickOn(R.id.scanButton)

        assertDisabled(R.id.scanButton)
        assertDisplayed(R.id.scanSpinner)
        assertNotDisplayed(R.id.noServersLabel)

        sleep(scanTimeout.toLong(), TimeUnit.SECONDS)

        assertEnabled(R.id.scanButton)
        assertNotDisplayed(R.id.scanSpinner)
        assertDisplayed(R.id.noServersLabel, R.string.no_servers_found)
        assertRecyclerViewItemCount(R.id.serverList, 0)
    }

    @Test
    fun addressBarTest() {
        clearText(R.id.addressBar)
        assertDisabled(R.id.connectButton)

        assertDisplayed(R.id.connectLabel, R.string.not_connected)
        assertNotDisplayed(R.id.connectSpinner)

        writeTo(R.id.addressBar, "127.0.0.1")
        assertEnabled(R.id.connectButton)
    }

    @Test
    fun connectionFailedTest() {
        val connectTimeout = AtomicInteger()
        activityScenarioRule.scenario.onActivity { activity ->
            connectTimeout.lazySet(activity.viewModels<AgentViewModel>().value.connectTimeout)
        }

        assertDisplayed(R.id.connectLabel, R.string.not_connected)
        assertNotDisplayed(R.id.connectSpinner)

        writeTo(R.id.addressBar, "127.0.0.1")
        clickOn(R.id.connectButton)

        assertDisplayed(R.id.connectLabel, R.string.connecting)
        assertDisplayed(R.id.connectSpinner)

        sleep(connectTimeout.toLong(), TimeUnit.SECONDS)

        assertDisplayed(R.id.connectLabel, R.string.failed_to_connect)
        assertNotDisplayed(R.id.connectSpinner)

        clickOn(R.id.connectButton)
        assertDisplayed(R.id.connectLabel, R.string.failed_to_connect)
        assertNotDisplayed(R.id.connectSpinner)
    }

    @Test
    fun showNetworkInfoTest() = runTest {
        val showingInfo = AtomicBoolean()
        activityScenarioRule.scenario.onActivity { activity ->
            showingInfo.lazySet(activity.viewModels<AgentViewModel>().value.showingNetworkInfo)
        }

        val hasConnection = Konnection.instance.getInfo()?.ipv4 != null

        val infoViews = intArrayOf(
            R.id.addressLabel,
            R.id.networkTypeLabel,
            R.id.networkInfoDivider,
        )

        val settingValue = showingInfo.get()
        listOf(settingValue, !settingValue, settingValue).forEachIndexed { index, showing ->
            if (index != 0) {
                SettingsFragmentTest.openSettingsMenu()
                SettingsFragmentTest.openSettingsSubMenu(0)

                clickOn(R.id.showNetworkInfoButton)
                clickOn(R.id.connectPageButton)
            }

            infoViews.forEachIndexed { viewIndex, resId ->
                val couldBeNull = viewIndex == 0 && !hasConnection
                ArtemisAgentTestHelpers.assertDisplayed(resId, showing && !couldBeNull)
            }
        }
    }
}
