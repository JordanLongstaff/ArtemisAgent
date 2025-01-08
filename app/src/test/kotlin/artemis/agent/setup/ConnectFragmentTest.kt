package artemis.agent.setup

import android.Manifest
import androidx.activity.viewModels
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
import com.adevinta.android.barista.interaction.PermissionGranter
import dev.tmapps.konnection.Konnection
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

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

            PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

            assertEnabled(R.id.scanButton)
            assertNotDisplayed(R.id.scanSpinner)
            assertDisplayed(R.id.noServersLabel, R.string.click_scan)
            assertDisplayed(R.id.serverList)
            assertRecyclerViewItemCount(R.id.serverList, 0)

            clickOn(R.id.scanButton)
            shadowLooper.idle()

            assertDisabled(R.id.scanButton)
            assertDisplayed(R.id.scanSpinner)
            assertNotDisplayed(R.id.noServersLabel)

            shadowLooper.idleFor(scanTimeout.toLong(), TimeUnit.SECONDS)
            shadowLooper.idle()

            assertEnabled(R.id.scanButton)
            assertNotDisplayed(R.id.scanSpinner)
            assertDisplayed(R.id.noServersLabel, R.string.no_servers_found)
            assertRecyclerViewItemCount(R.id.serverList, 0)
        }
    }

    @Test
    fun addressBarTest() {
        Robolectric.buildActivity(MainActivity::class.java).use {
            it.setup()
            PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

            clearText(R.id.addressBar)
            assertHint(R.id.addressBar, R.string.address)
            assertDisabled(R.id.connectButton)

            assertDisplayed(R.id.connectLabel, R.string.not_connected)
            assertNotDisplayed(R.id.connectSpinner)

            writeTo(R.id.addressBar, "127.0.0.1")
            assertEnabled(R.id.connectButton)
        }
    }

    @Test
    fun connectionFailedTest() {
        Robolectric.buildActivity(MainActivity::class.java).use {
            it.setup()

            val activity = it.get()
            val connectTimeout = activity.viewModels<AgentViewModel>().value.connectTimeout
            val shadowLooper = ShadowLooper.shadowMainLooper()

            PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

            assertDisplayed(R.id.connectLabel, R.string.not_connected)
            assertNotDisplayed(R.id.connectSpinner)

            writeTo(R.id.addressBar, "127.0.0.1")
            shadowLooper.idle()

            clickOn(R.id.connectButton)
            shadowLooper.idle()

            assertDisplayed(R.id.connectLabel, R.string.connecting)
            assertDisplayed(R.id.connectSpinner)

            shadowLooper.idleFor(connectTimeout.toLong(), TimeUnit.SECONDS)
            shadowLooper.idle()

            assertDisplayed(R.id.connectLabel, R.string.failed_to_connect)
            assertNotDisplayed(R.id.connectSpinner)
        }
    }

    @Test
    fun showNetworkInfoTest() = runTest {
        Robolectric.buildActivity(MainActivity::class.java).use {
            it.setup()

            val activity = it.get()
            val showingInfo = activity.viewModels<AgentViewModel>().value.showingNetworkInfo

            PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

            val hasNetwork = !Konnection.instance.getInfo()?.ipv4.isNullOrBlank()

            val infoViews =
                intArrayOf(R.id.addressLabel, R.id.networkTypeLabel, R.id.networkInfoDivider)

            listOf(showingInfo, !showingInfo, showingInfo).forEachIndexed { index, showing ->
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
    }
}
