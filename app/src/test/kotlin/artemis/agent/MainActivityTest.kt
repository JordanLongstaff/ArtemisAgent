package artemis.agent

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
class MainActivityTest {
    @Test
    fun radioButtonsTest() {
        Robolectric.buildActivity(MainActivity::class.java).use {
            it.setup()

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
    }
}
