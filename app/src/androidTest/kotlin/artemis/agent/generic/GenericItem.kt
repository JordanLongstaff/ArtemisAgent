package artemis.agent.generic

import android.view.View
import artemis.agent.R
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher

class GenericItem(parent: Matcher<View>) : KRecyclerItem<GenericItem>(parent) {
    val entryNameLabel = KTextView(parent) { withId(R.id.entryNameLabel) }
    val entryDataLabel = KTextView(parent) { withId(R.id.entryDataLabel) }
}
