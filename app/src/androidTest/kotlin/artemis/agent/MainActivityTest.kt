package artemis.agent

import android.Manifest
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogNegativeButton
import com.adevinta.android.barista.interaction.BaristaDialogInteractions.clickDialogPositiveButton
import com.adevinta.android.barista.interaction.PermissionGranter
import org.junit.Assert
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {
    @get:Rule val activityScenarioManager = ActivityScenarioManager.forActivity<MainActivity>()

    @Test
    fun radioButtonsTest() {
        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

        assertChecked(R.id.setupPageButton)
        assertDisplayed(R.id.setupPageSelector)

        assertUnchecked(R.id.gamePageButton)
        assertNotExist(R.id.shipNumberLabel)

        assertUnchecked(R.id.helpPageButton)
        assertNotExist(R.id.helpTopicContent)

        clickOn(R.id.gamePageButton)

        assertUnchecked(R.id.setupPageButton)
        assertNotExist(R.id.setupPageSelector)

        assertChecked(R.id.gamePageButton)
        assertDisplayed(R.id.shipNumberLabel)

        assertUnchecked(R.id.helpPageButton)
        assertNotExist(R.id.helpTopicContent)

        clickOn(R.id.helpPageButton)

        assertUnchecked(R.id.setupPageButton)
        assertNotExist(R.id.setupPageSelector)

        assertUnchecked(R.id.gamePageButton)
        assertNotExist(R.id.shipNumberLabel)

        assertChecked(R.id.helpPageButton)
        assertDisplayed(R.id.helpTopicContent)

        clickOn(R.id.setupPageButton)

        assertChecked(R.id.setupPageButton)
        assertDisplayed(R.id.setupPageSelector)

        assertUnchecked(R.id.gamePageButton)
        assertNotExist(R.id.shipNumberLabel)

        assertUnchecked(R.id.helpPageButton)
        assertNotExist(R.id.helpTopicContent)
    }

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
            Assume.assumeTrue(
                "Requires API 33 or higher",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
            )

            PermissionGranter.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
            assertDisplayed(android.R.id.message, R.string.permission_rationale)
        }
    }
}
