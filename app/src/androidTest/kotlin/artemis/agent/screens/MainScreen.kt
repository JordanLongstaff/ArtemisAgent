package artemis.agent.screens

import android.os.Build
import androidx.test.espresso.NoActivityResumedException
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.isDisplayedWithText
import artemis.agent.isRemoved
import com.kaspersky.kaspresso.device.Device
import com.kaspersky.kaspresso.screens.KScreen
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import io.github.kakaocup.kakao.check.KCheckBox
import io.github.kakaocup.kakao.dialog.KAlertDialog
import io.github.kakaocup.kakao.text.KButton
import org.junit.Assert

object MainScreen : KScreen<MainScreen>() {
    override val layoutId: Int = R.layout.activity_main
    override val viewClass: Class<*> = MainActivity::class.java

    val setupPageButton = KCheckBox { withId(R.id.setupPageButton) }
    val gamePageButton = KCheckBox { withId(R.id.gamePageButton) }
    val helpPageButton = KCheckBox { withId(R.id.helpPageButton) }

    val updateButton = KButton { withId(R.id.updateButton) }

    val alertDialog = KAlertDialog()

    private val isTiramisu by lazy { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }

    fun TestContext<*>.mainScreenTest(
        backButtonShouldCloseApp: Boolean = true,
        test: MainScreen.() -> Unit,
    ) {
        this@MainScreen {
            step("Accept permissions") { acceptPermissions(device) }
            step("Dismiss changelog") { pressBack() }
            test()
            if (backButtonShouldCloseApp)
                step("Back button should close the app") { assertCloseOnBackButton() }
        }
    }

    private fun assertCloseOnBackButton() {
        try {
            pressBack()
            Assert.fail("Expected back button to close the app")
        } catch (_: NoActivityResumedException) {
            // Success
        }
    }

    fun acceptPermissions(device: Device) {
        if (!isTiramisu) return
        device.permissions.allowViaDialog()
    }

    fun denyPermissions(device: Device) {
        if (!isTiramisu) return
        device.permissions.denyViaDialog()
    }

    fun assertPermissionsDialogOpen(device: Device) {
        Assert.assertEquals(device.permissions.isDialogVisible(), isTiramisu)
    }

    fun assertPermissionRationaleDialogOpen() {
        alertDialog {
            isCompletelyDisplayed()
            title.isRemoved()
            message.isDisplayedWithText(R.string.permission_rationale)
            positiveButton.isDisplayedWithText(R.string.yes)
            negativeButton.isDisplayedWithText(R.string.no)
            neutralButton.isRemoved()
        }
    }

    fun assertChangelogOpen() {
        alertDialog {
            isCompletelyDisplayed()
            title.isDisplayedWithText(R.string.app_version)
            message.isCompletelyDisplayed()
            positiveButton.isRemoved()
            negativeButton.isRemoved()
            neutralButton.isRemoved()
        }
    }
}
