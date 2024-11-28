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
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.clearText
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
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

        clickOn(R.id.scanButton)

        assertDisabled(R.id.scanButton)
        assertDisplayed(R.id.scanSpinner)

        sleep(scanTimeout.toLong(), TimeUnit.SECONDS)

        assertEnabled(R.id.scanButton)
        assertNotDisplayed(R.id.scanSpinner)
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
    fun showNetworkInfoTest() {
        val showingInfo = AtomicBoolean()
        activityScenarioRule.scenario.onActivity { activity ->
            showingInfo.lazySet(activity.viewModels<AgentViewModel>().value.showingNetworkInfo)
        }

        val infoViews = intArrayOf(
            R.id.addressLabel,
            R.id.networkTypeLabel,
            R.id.networkInfoDivider,
        )

        val settingValue = showingInfo.get()
        listOf(settingValue, !settingValue, settingValue).forEachIndexed { index, showing ->
            if (index != 0) {
                SettingsFragmentTest.openSettingsMenu()
                SettingsFragmentTest.openSettingsSubMenu(1)

                clickOn(R.id.showNetworkInfoButton)
                clickOn(R.id.connectPageButton)
            }

            infoViews.forEach { resId ->
                ArtemisAgentTestHelpers.assertDisplayed(resId, showing)
            }
        }
    }
}
