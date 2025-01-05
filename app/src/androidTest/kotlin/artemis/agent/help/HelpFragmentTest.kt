package artemis.agent.help

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.ActivityScenarioManager
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertDisplayedAtPosition
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickBack
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.PermissionGranter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class HelpFragmentTest {
    @get:Rule
    val activityScenarioManager = ActivityScenarioManager.forActivity<MainActivity>()

    @Test
    fun menuOptionsTest() {
        testHelpFragment { clickOn(R.id.backButton) }
    }

    @Test
    fun backButtonTest() {
        testHelpFragment { clickBack() }
    }

    private companion object {
        val helpTopics = arrayOf(
            R.string.help_topics_getting_started to 8,
            R.string.help_topics_basics to 4,
            R.string.help_topics_stations to 12,
            R.string.help_topics_allies to 4,
            R.string.help_topics_missions to 14,
            R.string.help_topics_routing to 6,
            R.string.help_topics_enemies to 12,
            R.string.help_topics_biomechs to 3,
            R.string.help_topics_notifications to 16,
            R.string.help_topics_about to 5,
        )

        fun assertHelpMenuDisplayed() {
            assertNotDisplayed(R.id.helpTopicTitle)
            assertNotDisplayed(R.id.backButton)
            assertDisplayed(R.id.helpTopicContent)
            assertRecyclerViewItemCount(R.id.helpTopicContent, helpTopics.size)

            helpTopics.forEachIndexed { index, (res, _) ->
                assertDisplayedAtPosition(R.id.helpTopicContent, index, res)
            }
        }

        fun testHelpFragment(goBack: () -> Unit) {
            PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

            clickOn(R.id.helpPageButton)
            assertHelpMenuDisplayed()

            helpTopics.forEach { (stringRes, itemCount) ->
                clickOn(stringRes)

                assertDisplayed(R.id.helpTopicTitle, stringRes)
                assertDisplayed(R.id.backButton)
                assertDisplayed(R.id.helpTopicContent)
                assertRecyclerViewItemCount(R.id.helpTopicContent, itemCount)

                goBack()
                assertHelpMenuDisplayed()
            }
        }
    }
}
