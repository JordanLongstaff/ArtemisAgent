package artemis.agent.screens

import android.view.View
import android.widget.PopupWindow
import artemis.agent.R
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.recycler.KRecyclerView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher

object SelectorPopupScreen : KScreen<SelectorPopupScreen>() {
    override val layoutId: Int = R.layout.selector_popup
    override val viewClass: Class<*> = PopupWindow::class.java

    val selectorList = KRecyclerView({ withId(R.id.selectorList) }, { itemType(::PageEntry) })

    class PageEntry(parent: Matcher<View>) : KRecyclerItem<PageEntry>(parent) {
        val entryLabel = KTextView(parent) { withId(R.id.entryLabel) }
    }
}
