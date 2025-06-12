package artemis.agent.screens

import com.kaspersky.components.kautomator.component.text.UiTextView
import com.kaspersky.components.kautomator.screen.UiScreen

object NotificationScreen : UiScreen<NotificationScreen>() {
    override val packageName: String = "com.android.systemui"
    private const val ID_PACKAGE: String = "android"

    val title = UiTextView { withId(this@NotificationScreen.ID_PACKAGE, "title") }
    val content = UiTextView { withId(this@NotificationScreen.ID_PACKAGE, "text") }
}
