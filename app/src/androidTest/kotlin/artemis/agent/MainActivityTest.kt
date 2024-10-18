package artemis.agent

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.karumi.shot.ActivityScenarioUtils.waitForActivity
import com.karumi.shot.ScreenshotTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest : ScreenshotTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun radioButtonsTest() {
        val activity = activityScenarioRule.scenario.waitForActivity()

        assertChecked(R.id.setupPageButton)
        assertDisplayed(R.id.setupPageSelector)

        assertUnchecked(R.id.gamePageButton)
        assertNotExist(R.id.shipNumberLabel)

        assertUnchecked(R.id.helpPageButton)
        assertNotExist(R.id.helpTopicContent)

        compareScreenshot(activity, name = "setupPage")

        clickOn(R.id.gamePageButton)

        assertUnchecked(R.id.setupPageButton)
        assertNotExist(R.id.setupPageSelector)

        assertChecked(R.id.gamePageButton)
        assertDisplayed(R.id.shipNumberLabel)

        assertUnchecked(R.id.helpPageButton)
        assertNotExist(R.id.helpTopicContent)

        compareScreenshot(activity, name = "gamePage")

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

        compareScreenshot(activity, name = "helpPage")
    }
}
