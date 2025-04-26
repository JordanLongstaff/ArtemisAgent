package artemis.agent

import android.Manifest
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogNegativeButton
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import com.adevinta.android.barista.interaction.PermissionGranter
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
@LargeTest
class PermissionRationaleTest {
    @get:Rule val activityScenarioManager = ActivityScenarioManager.forActivity<MainActivity>()

    @Test
    fun permissionRationaleDialogPositiveTest() {
        testPermissionDialog()
        clickDialogPositiveButton()
        assertDisplayed(R.id.mainPageSelector)
    }

    @Test
    fun permissionRationaleDialogNegativeTest() {
        testPermissionDialog()
        clickDialogNegativeButton()
        Assert.assertThrows(RuntimeException::class.java) { assertNotExist(R.id.mainPageSelector) }
        PermissionGranter.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertDisplayed(R.id.mainPageSelector)
    }

    private companion object {
        fun testPermissionDialog() {
            PermissionGranter.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
            sleep(300L)
            assertDisplayed(android.R.id.message, R.string.permission_rationale)
        }
    }
}
