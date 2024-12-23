package artemis.agent

import androidx.annotation.IdRes
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions

object ArtemisAgentTestHelpers {
    fun assertEnabled(@IdRes resId: Int, enabled: Boolean) {
        if (enabled) {
            BaristaEnabledAssertions.assertEnabled(resId)
        } else {
            BaristaEnabledAssertions.assertDisabled(resId)
        }
    }

    fun assertChecked(@IdRes resId: Int, checked: Boolean) {
        if (checked) {
            BaristaCheckedAssertions.assertChecked(resId)
        } else {
            BaristaCheckedAssertions.assertUnchecked(resId)
        }
    }

    fun assertDisplayed(@IdRes resId: Int, displayed: Boolean) {
        if (displayed) {
            BaristaVisibilityAssertions.assertDisplayed(resId)
        } else {
            BaristaVisibilityAssertions.assertNotDisplayed(resId)
        }
    }
}
