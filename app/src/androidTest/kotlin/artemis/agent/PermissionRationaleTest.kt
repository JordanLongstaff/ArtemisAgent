package artemis.agent

import android.os.Build
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import artemis.agent.screens.MainScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
@LargeTest
class PermissionRationaleTest : TestCase() {
    @get:Rule val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun permissionRationaleDialogPositiveTest() = run {
        testPermissionDialog { screen ->
            step("Click positive button") {
                screen.permissionRationaleDialog { positiveButton.click() }
            }
        }
    }

    @Test
    fun permissionRationaleDialogNegativeTest() = run {
        testPermissionDialog { screen ->
            step("Click negative button") {
                screen.permissionRationaleDialog { negativeButton.click() }
            }

            step("Check for permissions dialog again") {
                screen.assertPermissionsDialogOpen(device)
            }

            step("Deny permissions again") { screen.denyPermissions(device) }
        }
    }

    private companion object {
        fun TestContext<*>.testPermissionDialog(withDialog: TestContext<*>.(MainScreen) -> Unit) {
            MainScreen {
                step("Check for permissions dialog") { assertPermissionsDialogOpen(device) }

                step("Deny permissions") { denyPermissions(device) }

                step("Check for permission rationale dialog") {
                    assertPermissionRationaleDialogOpen()
                }

                withDialog(this)

                step("No permission rationale dialog afterwards") {
                    permissionRationaleDialog.isNotDisplayed()
                }
            }
        }
    }
}
