package artemis.agent

import android.os.Build
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import artemis.agent.screens.MainScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import io.github.kakaocup.kakao.dialog.KAlertDialog
import io.github.kakaocup.kakao.text.KButton
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
@LargeTest
class PermissionRationaleTest : TestCase() {
    @get:Rule val activityScenarioRule = activityScenarioRule<MainActivity>()

    @Test
    fun permissionRationaleDialogPositiveTest() {
        run {
            testPermissionDialog {
                step("Click positive button") { clickRationaleDialogButton { positiveButton } }
            }
        }
    }

    @Test
    fun permissionRationaleDialogNegativeTest() {
        run {
            testPermissionDialog {
                step("Click negative button") { clickRationaleDialogButton { negativeButton } }

                step("Check for permissions dialog again") {
                    MainScreen.assertPermissionsDialogOpen(device)
                }

                step("Deny permissions again") {
                    /*
                     * In the second instance of the permissions dialog, the deny button has a different
                     * resource ID that Kaspresso doesn't know about, so unfortunately we have to bypass
                     * Kaspresso's methods and go through UiAutomator ourselves.
                     */
                    val denyAndDontAskAgain =
                        UiSelector()
                            .clickable(true)
                            .checkable(false)
                            .resourceId(DENY_AND_DONT_ASK_BUTTON)
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                        .findObject(denyAndDontAskAgain)
                        .click()
                }
            }
        }
    }

    private companion object {
        const val DENY_AND_DONT_ASK_BUTTON =
            "com.android.permissioncontroller:id/permission_deny_and_dont_ask_again_button"

        fun clickRationaleDialogButton(button: KAlertDialog.() -> KButton) {
            MainScreen.alertDialog.button().click()
        }

        fun TestContext<*>.testPermissionDialog(withDialog: TestContext<*>.() -> Unit) {
            MainScreen {
                step("Check for permissions dialog") { assertPermissionsDialogOpen(device) }

                step("Deny permissions") { denyPermissions(device) }

                step("Check for permission rationale dialog") {
                    assertPermissionRationaleDialogOpen()
                }

                withDialog()

                step("Check for changelog") {
                    assertChangelogOpen()
                    Assert.assertFalse(device.permissions.isDialogVisible())
                }
            }
        }
    }
}
