package artemis.agent.setup.settings

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import artemis.agent.ArtemisAgentTestHelpers
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo

class SingleToggleButtonSetting(
    @IdRes val divider: Int,
    @IdRes val label: Int,
    @StringRes val text: Int,
    @IdRes val button: Int,
) {
    fun testSingleToggle(isChecked: Boolean) {
        scrollTo(divider)
        assertDisplayed(label, text)
        assertDisplayed(button)
        ArtemisAgentTestHelpers.assertChecked(button, isChecked)
    }

    fun testNotExist() {
        assertNotExist(divider)
        assertNotExist(button)
        assertNotExist(text)
    }
}
