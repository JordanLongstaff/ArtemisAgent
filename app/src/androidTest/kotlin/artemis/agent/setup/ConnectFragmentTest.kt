package artemis.agent.setup

import android.Manifest
import android.os.Build
import androidx.activity.viewModels
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.ActivityScenarioManager
import artemis.agent.AgentViewModel
import artemis.agent.ArtemisAgentTestHelpers
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.setup.settings.SettingsFragmentTest
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertDisabled
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertEnabled
import com.adevinta.android.barista.assertion.BaristaHintAssertions.assertHint
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.clearText
import com.adevinta.android.barista.interaction.BaristaEditTextInteractions.writeTo
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import com.adevinta.android.barista.interaction.PermissionGranter
import dev.tmapps.konnection.Konnection
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ConnectFragmentTest {
    @get:Rule val activityScenarioManager = ActivityScenarioManager.forActivity<MainActivity>()

    @Test
    fun scanTest() {
        val scanTimeout = AtomicInteger()
        activityScenarioManager.onActivity { activity ->
            scanTimeout.lazySet(activity.viewModels<AgentViewModel>().value.scanTimeout)
        }

        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

        assertEnabled(R.id.scanButton)
        assertNotDisplayed(R.id.scanSpinner)
        assertDisplayed(R.id.noServersLabel, R.string.click_scan)
        assertDisplayed(R.id.serverList)
        assertRecyclerViewItemCount(R.id.serverList, 0)

        clickOn(R.id.scanButton)

        if (!isEmulator || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // This part is failing on CI with pre-Android 24 emulators for some reason
            assertDisabled(R.id.scanButton)
            assertDisplayed(R.id.scanSpinner)
            assertNotDisplayed(R.id.noServersLabel)

            sleep(scanTimeout.toLong(), TimeUnit.SECONDS)
        }

        assertEnabled(R.id.scanButton)
        assertNotDisplayed(R.id.scanSpinner)
        assertDisplayed(R.id.noServersLabel, R.string.no_servers_found)
        assertRecyclerViewItemCount(R.id.serverList, 0)
    }

    @Test
    fun addressBarTest() {
        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

        clearText(R.id.addressBar)
        assertHint(R.id.addressBar, R.string.address)
        assertDisabled(R.id.connectButton)

        assertDisplayed(R.id.connectLabel, R.string.not_connected)
        assertNotDisplayed(R.id.connectSpinner)

        writeTo(R.id.addressBar, "127.0.0.1")
        assertEnabled(R.id.connectButton)
    }

    @Test
    fun connectionFailedTest() {
        val connectTimeout = AtomicInteger()
        activityScenarioManager.onActivity { activity ->
            connectTimeout.lazySet(activity.viewModels<AgentViewModel>().value.connectTimeout)
        }

        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

        assertDisplayed(R.id.connectLabel, R.string.not_connected)
        assertNotDisplayed(R.id.connectSpinner)

        writeTo(R.id.addressBar, "127.0.0.1")
        sleep(100L)
        clickOn(R.id.connectButton)

        if (!isEmulator) {
            // Skip this check on CI since it always fails
            assertDisplayed(R.id.connectLabel, R.string.connecting)
            assertDisplayed(R.id.connectSpinner)

            sleep(connectTimeout.toLong(), TimeUnit.SECONDS)
        }

        assertDisplayed(R.id.connectLabel, R.string.failed_to_connect)
        assertNotDisplayed(R.id.connectSpinner)
    }

    @Test
    fun showNetworkInfoTest() = runTest {
        val showingInfo = AtomicBoolean()
        activityScenarioManager.onActivity { activity ->
            showingInfo.lazySet(activity.viewModels<AgentViewModel>().value.showingNetworkInfo)
        }

        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

        val hasNetwork = !Konnection.instance.getInfo()?.ipv4.isNullOrBlank()

        val infoViews =
            intArrayOf(R.id.addressLabel, R.id.networkTypeLabel, R.id.networkInfoDivider)

        val settingValue = showingInfo.get()
        listOf(settingValue, !settingValue, settingValue).forEachIndexed { index, showing ->
            if (index != 0) {
                SettingsFragmentTest.openSettingsMenu()
                SettingsFragmentTest.openSettingsSubMenu(0)

                clickOn(R.id.showNetworkInfoButton)
                clickOn(R.id.connectPageButton)
            }

            infoViews.forEachIndexed { viewIndex, resId ->
                val isNotEmpty = viewIndex > 0 || hasNetwork
                ArtemisAgentTestHelpers.assertDisplayed(resId, showing && isNotEmpty)
            }
        }
    }

    companion object {
        private val EMULATOR_DEVICES = setOf("emu64x", "emulator64_x86_64", "generic_x86_64")

        private val isEmulator by lazy { Build.DEVICE in EMULATOR_DEVICES }
    }
}
