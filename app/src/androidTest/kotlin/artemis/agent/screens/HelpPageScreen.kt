package artemis.agent.screens

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import artemis.agent.R
import artemis.agent.help.HelpFragment
import artemis.agent.isDisplayedWithSize
import artemis.agent.isDisplayedWithText
import artemis.agent.isRemoved
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.common.actions.BaseActions
import io.github.kakaocup.kakao.common.assertions.BaseAssertions
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.recycler.KRecyclerView
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView
import io.github.kakaocup.kakao.text.TextViewAssertions
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf

object HelpPageScreen : KScreen<HelpPageScreen>() {
    override val layoutId: Int = R.layout.help_fragment
    override val viewClass: Class<*> = HelpFragment::class.java

    val helpTopicContent =
        KRecyclerView(
            builder = { withId(R.id.helpTopicContent) },
            itemTypeBuilder = {
                itemType(::MenuButtonItem)
                itemType(::ImageItem)
                itemType(::TextItem)
            },
        )
    val helpTopicTitle = KTextView { withId(R.id.helpTopicTitle) }
    val backButton = KButton { withId(R.id.backButton) }

    val helpTopics by lazy {
        arrayOf(
            R.string.help_topics_getting_started to 8,
            R.string.help_topics_basics to 4,
            R.string.help_topics_stations to 12,
            R.string.help_topics_allies to 5,
            R.string.help_topics_missions to 14,
            R.string.help_topics_routing to 6,
            R.string.help_topics_enemies to 12,
            R.string.help_topics_biomechs to 3,
            R.string.help_topics_notifications to 16,
            R.string.help_topics_about to 5,
        )
    }

    val aboutHelpTopicIndex by lazy { helpTopics.lastIndex }

    fun assertMainMenuDisplayed() {
        helpTopicTitle.isRemoved()
        backButton.isRemoved()
        helpTopicContent.isDisplayedWithSize(helpTopics.size)

        helpTopics.forEachIndexed { index, (res, _) ->
            helpTopicContent.childAt<MenuButtonItem>(index) { isDisplayedWithText(res) }
        }
    }

    fun openTopicAtIndex(index: Int) {
        val (titleId, itemCount) = helpTopics[index]

        helpTopicContent {
            childAt<MenuButtonItem>(index) { click() }
            isDisplayedWithSize(itemCount)
        }
        helpTopicTitle.isDisplayedWithText(titleId)
        backButton.isCompletelyDisplayed()
    }

    class MenuButtonItem(parent: Matcher<View>) :
        KRecyclerItem<MenuButtonItem>(allOf(parent, instanceOf(Button::class.java))),
        BaseActions,
        BaseAssertions,
        TextViewAssertions

    class ImageItem(parent: Matcher<View>) :
        KRecyclerItem<ImageItem>(allOf(parent, instanceOf(ImageView::class.java)))

    class TextItem(parent: Matcher<View>) :
        KRecyclerItem<TextItem>(allOf(parent, instanceOf(TextView::class.java)))
}
