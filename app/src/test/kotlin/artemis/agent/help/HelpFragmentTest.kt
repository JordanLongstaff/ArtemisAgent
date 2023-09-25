package artemis.agent.help

import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertDisplayedAtPosition
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HelpFragmentTest {
    private val helpTopics = arrayOf(
        R.string.help_topics_getting_started to 8,
        R.string.help_topics_basics to 4,
        R.string.help_topics_stations to 12,
        R.string.help_topics_allies to 4,
        R.string.help_topics_missions to 14,
        R.string.help_topics_routing to 6,
        R.string.help_topics_enemies to 12,
        R.string.help_topics_biomechs to 3,
        R.string.help_topics_about to 5,
    )

    @Test
    fun menuOptionsTest() {
        Robolectric.buildActivity(MainActivity::class.java).use {
            it.setup()

            clickOn(R.id.helpPageButton)

            assertNotDisplayed(R.id.helpTopicTitle)
            assertNotDisplayed(R.id.backButton)

            helpTopics.forEach { (stringRes, itemCount) ->
                clickOn(stringRes)

                assertDisplayed(R.id.helpTopicTitle, stringRes)
                assertDisplayed(R.id.backButton)
                assertDisplayed(R.id.helpTopicContent)
                assertRecyclerViewItemCount(R.id.helpTopicContent, itemCount)

                clickOn(R.id.backButton)

                assertHelpMenuDisplayed()
            }
        }
    }

    private fun assertHelpMenuDisplayed() {
        assertNotDisplayed(R.id.helpTopicTitle)
        assertNotDisplayed(R.id.backButton)
        assertDisplayed(R.id.helpTopicContent)
        assertRecyclerViewItemCount(R.id.helpTopicContent, helpTopics.size)

        helpTopics.forEachIndexed { index, (res, _) ->
            assertDisplayedAtPosition(R.id.helpTopicContent, index, res)
        }
    }
}
