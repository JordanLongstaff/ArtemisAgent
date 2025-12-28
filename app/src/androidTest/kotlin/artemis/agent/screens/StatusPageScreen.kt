package artemis.agent.screens

import android.view.View
import artemis.agent.R
import artemis.agent.game.status.StatusFragment
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.recycler.KRecyclerView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher

object StatusPageScreen : KScreen<StatusPageScreen>() {
    override val layoutId: Int = R.layout.status_fragment
    override val viewClass: Class<*> = StatusFragment::class.java

    val statusReportView =
        KRecyclerView({ withId(R.id.statusReportView) }, { itemType(::StatusLine) })

    class StatusLine(parent: Matcher<View>) : KRecyclerItem<StatusLine>(parent) {
        val line = KTextView(parent) {}
    }
}
