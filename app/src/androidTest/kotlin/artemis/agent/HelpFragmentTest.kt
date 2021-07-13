package artemis.agent

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class HelpFragmentTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun menuOptionsTest() {
        val stringResources = intArrayOf(
            R.string.help_topics_getting_started,
            R.string.help_topics_basics,
            R.string.help_topics_stations,
            R.string.help_topics_allies,
            R.string.help_topics_missions,
            R.string.help_topics_routing,
            R.string.help_topics_biomechs,
            R.string.help_topics_about,
        )

        clickOn(R.id.helpPageButton)

        assertNotDisplayed(R.id.helpTopicTitle)
        assertNotDisplayed(R.id.backButton)

        stringResources.forEach { stringRes ->
            clickOn(stringRes)

            assertDisplayed(R.id.helpTopicTitle, stringRes)
            assertDisplayed(R.id.backButton)

            clickOn(R.id.backButton)

            assertNotDisplayed(R.id.helpTopicTitle)
            assertNotDisplayed(R.id.backButton)
        }
    }
}
