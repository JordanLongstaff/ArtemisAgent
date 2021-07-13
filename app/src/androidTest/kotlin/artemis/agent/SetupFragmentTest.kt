package artemis.agent

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SetupFragmentTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun radioButtonsTest() {
        assertChecked(R.id.setupPageButton)
        assertDisplayed(R.id.setupPageSelector)

        assertChecked(R.id.connectPageButton)
        assertDisplayed(R.id.addressBar)

        assertUnchecked(R.id.shipsPageButton)
        assertNotExist(R.id.shipsList)

        assertUnchecked(R.id.settingsPageButton)
        assertNotExist(R.id.settingsFragmentContainer)

        clickOn(R.id.shipsPageButton)

        assertUnchecked(R.id.connectPageButton)
        assertNotExist(R.id.addressBar)

        assertChecked(R.id.shipsPageButton)
        assertDisplayed(R.id.shipsList)

        assertUnchecked(R.id.settingsPageButton)
        assertNotExist(R.id.settingsFragmentContainer)

        clickOn(R.id.settingsPageButton)

        assertUnchecked(R.id.connectPageButton)
        assertNotExist(R.id.addressBar)

        assertUnchecked(R.id.shipsPageButton)
        assertNotExist(R.id.shipsList)

        assertChecked(R.id.settingsPageButton)
        assertDisplayed(R.id.settingsFragmentContainer)

        clickOn(R.id.connectPageButton)

        assertChecked(R.id.connectPageButton)
        assertDisplayed(R.id.addressBar)

        assertUnchecked(R.id.shipsPageButton)
        assertNotExist(R.id.shipsList)

        assertUnchecked(R.id.settingsPageButton)
        assertNotExist(R.id.settingsFragmentContainer)
    }
}
