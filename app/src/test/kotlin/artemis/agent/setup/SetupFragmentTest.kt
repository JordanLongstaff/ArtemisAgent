package artemis.agent.setup

import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SetupFragmentTest {
    @Test
    fun radioButtonsTest() {
        Robolectric.buildActivity(MainActivity::class.java).use {
            it.setup()

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
}
